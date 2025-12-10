package moae.dev.Game;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import moae.dev.Requests.SettingsRequest;
import moae.dev.Server.AppConfig;
import moae.dev.Services.PushNotificationService;
import moae.dev.Sockets.AnnouncementSocketConnectionHandler;
import moae.dev.Sockets.SocketConnectionHandler;
import moae.dev.Sockets.StateSocketConnectionHandler;
import moae.dev.Utils.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
  private final AppConfig config;
    private final PushNotificationService pushService;

  private final List<Team> teams;
  private final List<Player> players;

  private final AtomicInteger counter = new AtomicInteger(0);
  private final List<ChatMessage> messages = new ArrayList<>();

  private SocketConnectionHandler webSocketHandler;

  private static final long REWIND_TOLERANCE_MS = 5000;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> scheduled = null;
  private long remaining = -1;
  private long stageDuration = -1;
  private long stageStartEpoch = 0;

  private final boolean locked;

  private State state;
  private boolean paused = false;
  private boolean emergencyDeclared = false;

  private Team winner;

  public enum State {
    WAITING_TO_START("ready"),
    GRACE_PERIOD("grace"),
    SCOUT_PERIOD("scout"),
    FFA_PERIOD("ffa"),
    ENDED("ended");

    private final String readableName;

    State(String readableName) {
      this.readableName = readableName;
    }

    @Override
    public String toString() {
      return readableName;
    }

    @JsonValue
    public String getReadableName() {
      return readableName;
    }
  }

  public Game(AppConfig initConfig, PushNotificationService pushService) {
    this.pushService = pushService;
    teams = new ArrayList<Team>();
    players = new ArrayList<Player>();
    state = State.WAITING_TO_START;
    paused = false;
    config = initConfig;
    locked = false;
    winner = null;

    if (!registerNTeams(initConfig.getGame().getMaxTeams(), initConfig.getTeams())) {
      throw new RuntimeException("Failed to register teams. Check the configuration and retry");
    }
  }

  public void setWebSocketHandler(SocketConnectionHandler handler) {
    this.webSocketHandler = handler;
  }

  public SocketConnectionHandler getWebSocketHandler() {
    return this.webSocketHandler;
  }

  // ----- Game Controls -----
  public synchronized void start() {
    if (state != State.WAITING_TO_START && state != State.ENDED)
      throw new IllegalStateException("Cannot start game in this state: " + state.readableName);

    goTo(State.GRACE_PERIOD, config.getGame().getGraceTime() * 1000L);
  }

  public void pause(boolean announce) {
    if (!isGameRunning()) throw new IllegalStateException("Cannot pause a game that isn't running");
    if (paused) return;
    if (scheduled != null) scheduled.cancel(false);

    long elapsed = System.currentTimeMillis() - stageStartEpoch;
    remaining = Math.max(0, stageDuration - elapsed);

    paused = true;
    if (announce)
      pushService.notifyAll(
          "The game has been paused", "Check the global chat for more information");
    stateBroadcast(state, remaining, paused);
  }

  public synchronized void pause() {
    pause(true);
  }

  public void resume() {
    if (!paused) throw new IllegalStateException("Cannot resume a unpaused game");

    if (state == State.GRACE_PERIOD && !allFlagsRegistered() && remaining <= 0)
      throw new IllegalStateException("Cannot resume until all flags are registered");

    if (state == State.GRACE_PERIOD && allFlagsRegistered() && remaining <= 0) {
      goTo(State.SCOUT_PERIOD, config.getGame().getScoutTime() * 1000L);
      return;
    }

    paused = false;
    goToWithRemaining(state, remaining);
  }

  public void skip() {
    if (!isGameRunning() && !paused)
      throw new IllegalStateException("Cannot skip a game that isn't running");

    if (paused && state == State.GRACE_PERIOD && !allFlagsRegistered() && remaining <= 0)
      throw new IllegalStateException("Cannot skip until all flags are registered");

    if (scheduled != null) scheduled.cancel(false);

    if (paused) {
      state = getNextState(state);
    } else {
      advance();
    }
    remaining = getDurationForState(state);

    if (state == State.WAITING_TO_START || state == State.ENDED) paused = false;

    stateBroadcast(state, remaining, paused);
  }

  public void rewind() {
    if (!isGameRunning() && !paused && state != State.ENDED)
      throw new IllegalStateException("Cannot rewind a game that isn't running or ended");
    if (scheduled != null) scheduled.cancel(false);

    if (paused) {
      long elapsed = stageDuration - remaining;
      if (elapsed <= REWIND_TOLERANCE_MS) {
        state = getPreviousState(state);
      }
    } else {
      long elapsed = System.currentTimeMillis() - stageStartEpoch;
      if (elapsed <= REWIND_TOLERANCE_MS) {
        State previousState = getPreviousState(state);
        goTo(previousState, getDurationForState(previousState));
      } else {
        goTo(state, getDurationForState(state));
      }
    }

    if (state == State.WAITING_TO_START) {
      remaining = config.getGame().getGraceTime() * 1000L;
    } else {
      remaining = getDurationForState(state);
    }

    if (state == State.WAITING_TO_START || state == State.ENDED) paused = false;

    stateBroadcast(state, remaining, paused);
  }

  public void end() {
    if (!isGameRunning()) throw new IllegalStateException("Cannot end a game that isn't running");
    if (scheduled != null) scheduled.cancel(false);
    remaining = -1;
    paused = false;
    state = State.ENDED;
    stateBroadcast(state, 0, paused);
  }

  public synchronized void reset() {
    reset(false);
  }

  public synchronized void reset(boolean hard) {
    if (scheduled != null) {
      scheduled.cancel(false);
      scheduled = null;
    }

    state = State.WAITING_TO_START;
    paused = false;
    remaining = -1;
    stageDuration = -1;
    stageStartEpoch = 0;
    winner = null;

    teams.forEach(Team::reset);

    if (hard) {
      players.clear();
      messages.clear();
      counter.set(0);

      AnnouncementSocketConnectionHandler.broadcast(new AnnouncementMessage("reset", null));
    }

    stateBroadcast(state, config.getGame().getGraceTime() * 1000L, paused);
  }

  // ----- State Handling -----
  public void goTo(State newState, long duration) {
    setState(newState);
    paused = false;
    stageDuration = duration;
    stageStartEpoch = System.currentTimeMillis();
    remaining = -1;

    if (state == State.WAITING_TO_START) paused = false;

    stateBroadcast(newState, duration, paused);

    if (duration > 0) {
      scheduled = scheduler.schedule(this::advance, duration, TimeUnit.MILLISECONDS);
    }
  }

  public synchronized void setState(State state) {
    this.state = state;
  }

  public synchronized State getState() {
    return state;
  }

  public synchronized boolean isPaused() {
    return paused;
  }

  private void goToWithRemaining(State restoredState, long dur) {
    remaining = -1;
    paused = false;

    state = restoredState;
    stageDuration = dur;
    stageStartEpoch = System.currentTimeMillis();
    scheduled = scheduler.schedule(this::advance, dur, TimeUnit.MILLISECONDS);
    stateBroadcast(state, dur, paused);
  }

  public void advance() {
    synchronized (this) {
      switch (state) {
        case GRACE_PERIOD -> {
          if (!allFlagsRegistered()) {
            pause(false);
            pushService.notifyAll(
                "Waiting for all flags to be registered",
                "The game will resume once all teams have registered their flags");
            AnnouncementSocketConnectionHandler.broadcast(new AnnouncementMessage("frozen", null));
          } else {
            goTo(State.SCOUT_PERIOD, config.getGame().getScoutTime() * 1000L);
          }
        }
        case SCOUT_PERIOD -> goTo(State.FFA_PERIOD, config.getGame().getFfaTime() * 1000L);
        case FFA_PERIOD -> goTo(State.ENDED, 0);
      }
    }
  }

  public void back() {
    switch (state) {
      case SCOUT_PERIOD -> goTo(State.GRACE_PERIOD, config.getGame().getGraceTime() * 1000L);
      case FFA_PERIOD -> goTo(State.SCOUT_PERIOD, config.getGame().getScoutTime() * 1000L);
      case ENDED -> goTo(State.FFA_PERIOD, config.getGame().getFfaTime() * 1000L);
      default -> goTo(State.WAITING_TO_START, 0);
    }
  }

  public boolean isGameRunning() {
    return state == State.GRACE_PERIOD || state == State.SCOUT_PERIOD || state == State.FFA_PERIOD;
  }

  private State getNextState(State current) {
    return switch (current) {
      case GRACE_PERIOD -> State.SCOUT_PERIOD;
      case SCOUT_PERIOD -> State.FFA_PERIOD;
      case FFA_PERIOD -> State.ENDED;
      default -> current;
    };
  }

  private State getPreviousState(State current) {
    return switch (current) {
      case SCOUT_PERIOD -> State.GRACE_PERIOD;
      case FFA_PERIOD -> State.SCOUT_PERIOD;
      case ENDED -> State.FFA_PERIOD;
      default -> State.WAITING_TO_START;
    };
  }

  private long getDurationForState(State state) {
    return switch (state) {
      case GRACE_PERIOD -> config.getGame().getGraceTime() * 1000L;
      case SCOUT_PERIOD -> config.getGame().getScoutTime() * 1000L;
      case FFA_PERIOD -> config.getGame().getFfaTime() * 1000L;
      default -> 0L;
    };
  }

  // ----- Utilities -----
  public Map<String, Object> status() {
    List<Map<String, Object>> playerList = new ArrayList<>();
    List<Map<String, Object>> teamList = new ArrayList<>();

    long now = System.currentTimeMillis();
    Map<String, Object> currState = getCurrentState(now);

    players.forEach(p -> playerList.add(p.toMap()));
    teams.forEach(t -> teamList.add(t.toMap(state == State.FFA_PERIOD || state == State.ENDED)));

    return Map.of(
        "players", playerList,
        "teams", teamList,
        "state", currState,
        "game", config.getMap());
  }

  private Map<String, Object> getCurrentState(long now) {
    long timeLeft =
        switch (state) {
          case WAITING_TO_START -> config.getGame().getGraceTime() * 1000L;
          case ENDED -> 0L;
          default -> {
            if (paused) {
              yield Math.max(0L, remaining);
            } else {
              yield Math.max(0L, stageDuration - (now - stageStartEpoch));
            }
          }
        };

    Map<String, Object> result = new HashMap<>();
    result.put("state", state.toString());
    result.put("duration", timeLeft);
    result.put("paused", paused);
    result.put("emergency", emergencyDeclared);
    result.put("frozen", paused && state == State.GRACE_PERIOD && remaining <= 0);
    if (winner == null || state != State.ENDED) {
      result.put("winner", null);
    } else {
      result.put("winner", winner.getID());
    }

    return result;
  }

  private long getTimeRemaining() {
    long now = System.currentTimeMillis();
    if (paused) {
      return Math.max(0L, remaining);
    } else if (state == State.WAITING_TO_START) {
      return config.getGame().getGraceTime() * 1000L;
    } else if (state == State.ENDED) {
      return 0L;
    } else {
      return Math.max(0L, stageDuration - (now - stageStartEpoch));
    }
  }

  public void merge(@Valid SettingsRequest settings) {
    if (state != State.WAITING_TO_START)
      throw new ResponseStatusException(
          HttpStatus.LOCKED, "Settings can only be changed before the game.");

    config.merge(settings);
  }

  public Integer sendMessage(UUID sender, String content) {
    Player player = getPlayer(sender);
    Integer newId = counter.incrementAndGet();
    ChatMessage msg = new ChatMessage(content, player, newId, new Date(), player.getTeam());
    messages.add(msg);

    if (webSocketHandler != null) {
      webSocketHandler.broadcastMessage(msg);
    }

    return newId;
  }

  public MessagePage getMessages(Integer start, Integer count) {
    return MessageUtils.getMessages(start, count, messages);
  }

  public void lockCheck() {
    if (locked) throw new IllegalStateException("Game process is currently locked");
  }

  public void stateBroadcast(State newState, long duration, boolean isPaused) {
    String title = "Capture the Flag";
    String body =
        switch (newState) {
          case State.WAITING_TO_START -> {
            title = "this is a title";
            yield "this is a body";
          }
          case State.GRACE_PERIOD -> {
            title = "The game has begun";
            yield "The grace period has started. You have "
                + Math.round((float) config.getGame().getGraceTime() / 60)
                + " minutes to hide and register your flag.";
          }
          case State.SCOUT_PERIOD -> {
            title = "The scouting period has commenced";
            yield "Flags can now be stolen. You have "
                + Math.round((float) config.getGame().getScoutTime() / 60)
                + " minutes until the flag locations are revealed";
          }
          case State.FFA_PERIOD -> {
            title = "Flags have been revealed!";
            yield "Check the map on the website to see where the flags are located. You have "
                + Math.round((float) config.getGame().getFfaTime() / 60)
                + " minutes until the game finishes.";
          }
          case State.ENDED -> {
            title = "The game has ended";
            yield "Return to the rendezvous point.";
          }
        };

    pushService.notifyAll(title, body);
    StateSocketConnectionHandler.broadcast(new StateMessage(newState, duration, isPaused));
  }

  public void declareEmergency() {
    emergencyDeclared = true;
    try {
      pause(false);
    } catch (Exception ignored) {
    }

    pushService.notifyAll(
        "EMERGENCY DECLARED",
        "An emergency has been declared. Return to the rendezvous point immediately");
  }

  public void releaseEmergency() {
    emergencyDeclared = false;
    pushService.notifyAll(
        "Emergency state has been lifted",
        "Check the global chat for further information if needed.");
  }

  public boolean emergencyDeclared() {
    return emergencyDeclared;
  }

  // ----- Players -----
  public Player getPlayer(UUID id) {
    Player player = players.stream().filter(p -> p.getID().equals(id)).findFirst().orElse(null);
    if (player == null) throw new NoSuchElementException("Cannot find player");

    return player;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public UUID addPlayer(String name, UUID team, boolean auth) {
    if (state != State.WAITING_TO_START && !auth)
      throw new IllegalStateException("Cannot join game at this time");

    if (players.stream().anyMatch(p -> p.getName().equals(name)))
      throw new IllegalArgumentException(
          "A player with the name " + name + " already exists in the game");

    if (teams.stream().noneMatch(t -> t.getID().equals(team))) {
      throw new IllegalArgumentException("Invalid team choice");
    }

    Player newPlayer = new Player(name, team, auth);
    players.add(newPlayer);
    return newPlayer.getID();
  }

  public boolean removePlayer(UUID id) {
    if (!isValidPlayer(id)) throw new NoSuchElementException("Player not found");

    boolean removed = players.removeIf(p -> id.equals(p.getID()));

    if (players.isEmpty()) reset();
    return removed;
  }

  public boolean isValidPlayer(UUID player) {
    return players.stream().anyMatch(p -> p.getID().equals(player));
  }

  public boolean isPlayerOnTeam(UUID playerId, UUID teamId) {
    return players.stream()
        .filter(p -> p.getID().equals(playerId))
        .anyMatch(p -> p.getTeam().equals(teamId));
  }

  public boolean isAuth(UUID player) {
    return getPlayer(player).isAuth();
  }

  // ----- Teams -----
  public List<Team> getTeams() {
    return teams;
  }

  public Team getTeam(UUID id) {
    Team team = teams.stream().filter(t -> t.getID().equals(id)).findFirst().orElse(null);
    if (team == null) throw new NoSuchElementException("Cannot find team");

    return team;
  }

  private boolean registerNTeams(int n, List<AppConfig.TeamConfig> allTeams) {

    final boolean[] success = {true};
    allTeams.stream()
        .limit(n)
        .forEach(
            t -> {
              success[0] = success[0] && registerTeam(t.getName(), t.getColor());
            });

    return success[0];
  }

  public boolean registerTeam(String name, String color) {
    if (teams.stream().anyMatch(t -> t.getName().equals(name))) {
      return false;
    }

    return teams.add(new Team(name, color));
  }

  public Integer sendTeamMessage(UUID team, UUID sender, String content) {
    Integer newId = counter.incrementAndGet();
    getTeam(team).sendMessage(getPlayer(sender), content, newId);
    return newId;
  }

  public boolean isValidTeam(UUID team) {
    return teams.stream().anyMatch(p -> p.getID().equals(team));
  }

  public boolean allFlagsRegistered() {
    return teams.stream().allMatch(Team::isRegistered);
  }

  public void registerFlag(UUID teamId, int x, int y) {
    if (state != State.GRACE_PERIOD)
      throw new IllegalStateException("Flags can only be registered during grace period");

    Team team = getTeam(teamId);
    team.registerFlag(x, y);

    if (allFlagsRegistered() && paused && getTimeRemaining() <= 0) {
      resume();
    }

    AnnouncementSocketConnectionHandler.broadcast(
        new AnnouncementMessage("register", teamId.toString()));
  }

  public void declareVictory(UUID team) {
    if (state != State.SCOUT_PERIOD && state != State.FFA_PERIOD)
      throw new IllegalStateException("Cannot declare victory in this state");

    winner = getTeam(team);

    pushService.notifyAll(
        "Team " + winner.getName() + " has declared victory!",
        "The game has concluded. Please return to the rendezvous point.");

    AnnouncementSocketConnectionHandler.broadcast(
        new AnnouncementMessage("victory", team.toString()));

    end();
  }
}
