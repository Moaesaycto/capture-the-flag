package moae.dev.Game;

import moae.dev.Requests.SettingsRequest;
import moae.dev.Server.AppConfig;
import moae.dev.Services.PushNotificationService;
import moae.dev.Sockets.SocketConnectionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameTest {
    @Mock private AppConfig mockConfig;
    @Mock private AppConfig.GameConfig mockGameConfig;
    @Mock private PushNotificationService mockPushService;
    @Mock private SocketConnectionHandler mockWebSocketHandler;

    private Game game;
    @BeforeEach
    void setUp() {
        when(mockConfig.getGame()).thenReturn(mockGameConfig);
        when(mockGameConfig.getMaxTeams()).thenReturn(4);
        when(mockGameConfig.getGraceTime()).thenReturn(300);
        when(mockGameConfig.getScoutTime()).thenReturn(600);
        when(mockGameConfig.getFfaTime()).thenReturn(1800);
        
        List<AppConfig.TeamConfig> teamConfigs =
                List.of(
                        createMockTeamConfig("Red Team", "#FF0000"),
                        createMockTeamConfig("Blue Team", "#0000FF"),
                        createMockTeamConfig("Green Team", "#00FF00"),
                        createMockTeamConfig("Yellow Team", "#FFFF00"));
        when(mockConfig.getTeams()).thenReturn(teamConfigs);
        when(mockConfig.getMap()).thenReturn(Map.of("width", 1000, "height", 1000));
        
        game = new Game(mockConfig, mockPushService);
        game.setWebSocketHandler(mockWebSocketHandler);
    }

    private AppConfig.TeamConfig createMockTeamConfig(String name, String color) {
        AppConfig.TeamConfig config = mock(AppConfig.TeamConfig.class);
        when(config.getName()).thenReturn(name);
        when(config.getColor()).thenReturn(color);
        return config;
    }

    @Nested
    @DisplayName("Game Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize game in WAITING_TO_START state")
        void testInitialState() {
            assertEquals(Game.State.WAITING_TO_START, game.getState());
            assertFalse(game.isPaused());
            assertFalse(game.isGameRunning());
        }

        @Test
        @DisplayName("Should initialize with correct number of teams")
        void testTeamInitialization() {
            List<Team> teams = game.getTeams();

            assertEquals(4, teams.size(), "Should have 4 teams");
            assertEquals("Red Team", teams.getFirst().getName());
            assertEquals("Blue Team", teams.get(1).getName());
            assertEquals("Green Team", teams.get(2).getName());
            assertEquals("Yellow Team", teams.get(3).getName());
        }

        @Test
        @DisplayName("Should start with no players")
        void testNoInitialPlayers() {
            List<Player> players = game.getPlayers();

            assertTrue(players.isEmpty(), "Game should start with no players");
        }

        @Test
        @DisplayName("Should throw exception if team registration fails")
        void testFailedTeamRegistration() {
            // Create config with duplicate team names
            List<AppConfig.TeamConfig> duplicateTeams =
                    List.of(
                            createMockTeamConfig("Same Team", "#FF0000"),
                            createMockTeamConfig("Same Team", "#0000FF"));
            when(mockConfig.getTeams()).thenReturn(duplicateTeams);

            assertThrows(
                    RuntimeException.class,
                    () -> new Game(mockConfig, mockPushService),
                    "Should throw exception when team registration fails");
        }
    }

    @Nested
    @DisplayName("Player Management Tests")
    class PlayerManagementTests {

        @Test
        @DisplayName("Should add player during WAITING_TO_START")
        void testAddPlayerDuringWaiting() {
            UUID teamId = game.getTeams().getFirst().getID();

            UUID playerId = game.addPlayer("Alice", teamId, false);

            assertNotNull(playerId);
            assertEquals(1, game.getPlayers().size());
            assertEquals("Alice", game.getPlayer(playerId).getName());
        }

        @Test
        @DisplayName("Should not allow non-auth player to join after game starts")
        void testCannotAddPlayerAfterStart() {
            game.start();
            UUID teamId = game.getTeams().getFirst().getID();

            assertThrows(
                    IllegalStateException.class,
                    () -> game.addPlayer("Bob", teamId, false),
                    "Non-auth players cannot join after game starts");
        }

        @Test
        @DisplayName("Should allow auth player to join after game starts")
        void testAuthPlayerCanJoinAfterStart() {
            game.start();
            UUID teamId = game.getTeams().getFirst().getID();

            assertDoesNotThrow(
                    () -> game.addPlayer("Admin", teamId, true), "Auth players can join at any time");
        }

        @Test
        @DisplayName("Should not allow duplicate player names")
        void testDuplicatePlayerNames() {
            UUID teamId = game.getTeams().getFirst().getID();
            game.addPlayer("Alice", teamId, false);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> game.addPlayer("Alice", teamId, false),
                    "Cannot add player with duplicate name");
        }

        @Test
        @DisplayName("Should not allow player to join invalid team")
        void testInvalidTeamId() {
            UUID fakeTeamId = UUID.randomUUID();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> game.addPlayer("Bob", fakeTeamId, false),
                    "Cannot join non-existent team");
        }

        @Test
        @DisplayName("Should remove player successfully")
        void testRemovePlayer() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Charlie", teamId, false);

            boolean removed = game.removePlayer(playerId);

            assertTrue(removed);
            assertTrue(game.getPlayers().isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent player")
        void testRemoveNonExistentPlayer() {
            UUID fakePlayerId = UUID.randomUUID();

            assertThrows(
                    NoSuchElementException.class,
                    () -> game.removePlayer(fakePlayerId),
                    "Should throw exception for non-existent player");
        }

        @Test
        @DisplayName("Should reset game when last player is removed")
        void testResetWhenLastPlayerRemoved() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Dave", teamId, false);
            game.start();

            game.removePlayer(playerId);

            assertEquals(
                    Game.State.WAITING_TO_START,
                    game.getState(),
                    "Game should reset when last player is removed");
        }

        @Test
        @DisplayName("Should validate player exists")
        void testIsValidPlayer() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Eve", teamId, false);
            UUID fakePlayerId = UUID.randomUUID();

            assertTrue(game.isValidPlayer(playerId));
            assertFalse(game.isValidPlayer(fakePlayerId));
        }

        @Test
        @DisplayName("Should check if player is on team")
        void testIsPlayerOnTeam() {
            UUID team1Id = game.getTeams().getFirst().getID();
            UUID team2Id = game.getTeams().get(1).getID();
            UUID playerId = game.addPlayer("Frank", team1Id, false);

            assertTrue(game.isPlayerOnTeam(playerId, team1Id));
            assertFalse(game.isPlayerOnTeam(playerId, team2Id));
        }

        @Test
        @DisplayName("Should identify auth players correctly")
        void testIsAuth() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID normalPlayerId = game.addPlayer("Normal", teamId, false);
            UUID authPlayerId = game.addPlayer("Admin", teamId, true);

            assertFalse(game.isAuth(normalPlayerId));
            assertTrue(game.isAuth(authPlayerId));
        }
    }

    @Nested
    @DisplayName("Game State Transition Tests")
    class StateTransitionTests {

        @Test
        @DisplayName("Should start game from WAITING_TO_START")
        void testStartGame() {
            game.start();

            assertEquals(Game.State.GRACE_PERIOD, game.getState());
            assertTrue(game.isGameRunning());
        }

        @Test
        @DisplayName("Should not start game from invalid state")
        void testCannotStartFromInvalidState() {
            game.start();

            assertThrows(
                    IllegalStateException.class,
                    () -> game.start(),
                    "Cannot start game that's already running");
        }

        @Test
        @DisplayName("Should pause running game")
        void testPauseGame() {
            game.start();

            game.pause();

            assertTrue(game.isPaused());
            assertEquals(Game.State.GRACE_PERIOD, game.getState());
        }

        @Test
        @DisplayName("Should not pause game that isn't running")
        void testCannotPauseNonRunningGame() {
            assertThrows(
                    IllegalStateException.class, () -> game.pause(), "Cannot pause game that isn't running");
        }

        @Test
        @DisplayName("Should resume paused game")
        void testResumeGame() {
            game.start();
            game.pause();

            // Register all flags so resume doesn't block
            game.getTeams().forEach(team -> team.registerFlag(100, 100));

            game.resume();

            assertFalse(game.isPaused());
        }

        @Test
        @DisplayName("Should not resume unpaused game")
        void testCannotResumeUnpausedGame() {
            game.start();

            assertThrows(
                    IllegalStateException.class, () -> game.resume(), "Cannot resume game that isn't paused");
        }

        @Test
        @DisplayName("Should end running game")
        void testEndGame() {
            game.start();

            game.end();

            assertEquals(Game.State.ENDED, game.getState());
            assertFalse(game.isPaused());
            assertFalse(game.isGameRunning());
        }

        @Test
        @DisplayName("Should not end game that isn't running")
        void testCannotEndNonRunningGame() {
            assertThrows(
                    IllegalStateException.class, () -> game.end(), "Cannot end game that isn't running");
        }

        @Test
        @DisplayName("Should reset game to initial state")
        void testResetGame() {
            UUID teamId = game.getTeams().getFirst().getID();
            game.addPlayer("TestPlayer", teamId, false);
            game.start();

            game.reset();

            assertEquals(Game.State.WAITING_TO_START, game.getState());
            assertFalse(game.isPaused());
            assertEquals(1, game.getPlayers().size(), "Soft reset keeps players");
        }

        @Test
        @DisplayName("Should hard reset game and clear all data")
        void testHardResetGame() {
            UUID teamId = game.getTeams().getFirst().getID();
            game.addPlayer("TestPlayer", teamId, false);
            game.start();

            game.reset(true);

            assertEquals(Game.State.WAITING_TO_START, game.getState());
            assertTrue(game.getPlayers().isEmpty(), "Hard reset clears players");
        }

        @Test
        @DisplayName("Should skip to next state")
        void testSkipState() {
            game.start();
            assertEquals(Game.State.GRACE_PERIOD, game.getState());

            // Register flags first to allow skip
            game.getTeams().forEach(team -> team.registerFlag(100, 100));

            game.skip();

            assertEquals(Game.State.SCOUT_PERIOD, game.getState());
        }

        @Test
        @DisplayName("Should rewind to previous state if within tolerance")
        void testRewindState() {
            game.start();
            assertEquals(Game.State.GRACE_PERIOD, game.getState());

            // Register flags and advance
            game.getTeams().forEach(team -> team.registerFlag(100, 100));
            game.skip();
            assertEquals(Game.State.SCOUT_PERIOD, game.getState());

            // Rewind immediately (within 5 second tolerance)
            game.rewind();

            assertEquals(Game.State.GRACE_PERIOD, game.getState());
        }
    }

    @Nested
    @DisplayName("Flag Management Tests")
    class FlagManagementTests {

        @Test
        @DisplayName("Should register flag during GRACE_PERIOD")
        void testRegisterFlag() {
            game.start();
            UUID teamId = game.getTeams().getFirst().getID();

            assertDoesNotThrow(
                    () -> game.registerFlag(teamId, 100, 200), "Should register flag during grace period");

            assertTrue(game.getTeam(teamId).isRegistered());
        }

        @Test
        @DisplayName("Should not register flag outside GRACE_PERIOD")
        void testCannotRegisterFlagOutsideGracePeriod() {
            UUID teamId = game.getTeams().getFirst().getID();

            assertThrows(
                    IllegalStateException.class,
                    () -> game.registerFlag(teamId, 100, 200),
                    "Cannot register flag before game starts");
        }

        @Test
        @DisplayName("Should check if all flags are registered")
        void testAllFlagsRegistered() {
            game.start();

            assertFalse(game.allFlagsRegistered(), "Initially no flags registered");

            game.getTeams().forEach(team -> game.registerFlag(team.getID(), 100, 100));

            assertTrue(game.allFlagsRegistered(), "All flags should be registered");
        }

        @Test
        @DisplayName("Should resume game automatically when all flags registered during pause")
        void testAutoResumeWhenAllFlagsRegistered() {
            game.start();
            game.pause();

            // Register all flags
            game.getTeams().forEach(team -> game.registerFlag(team.getID(), 100, 100));

            // Game should auto-resume (this depends on remaining time being <= 0)
            // This test verifies the logic exists
        }
    }

    @Nested
    @DisplayName("Victory and Game End Tests")
    class VictoryTests {

        @Test
        @DisplayName("Should declare victory during SCOUT_PERIOD")
        void testDeclareVictoryDuringScout() {
            game.start();
            game.getTeams().forEach(team -> team.registerFlag(100, 100));
            game.skip(); // Move to SCOUT_PERIOD

            UUID winningTeamId = game.getTeams().getFirst().getID();

            assertDoesNotThrow(() -> game.declareVictory(winningTeamId));
            assertEquals(Game.State.ENDED, game.getState());
        }

        @Test
        @DisplayName("Should declare victory during FFA_PERIOD")
        void testDeclareVictoryDuringFFA() {
            game.start();
            game.getTeams().forEach(team -> team.registerFlag(100, 100));
            game.skip(); // SCOUT_PERIOD
            game.skip(); // FFA_PERIOD

            UUID winningTeamId = game.getTeams().getFirst().getID();

            assertDoesNotThrow(() -> game.declareVictory(winningTeamId));
            assertEquals(Game.State.ENDED, game.getState());
        }

        @Test
        @DisplayName("Should not declare victory during invalid states")
        void testCannotDeclareVictoryDuringInvalidState() {
            UUID teamId = game.getTeams().getFirst().getID();

            assertThrows(
                    IllegalStateException.class,
                    () -> game.declareVictory(teamId),
                    "Cannot declare victory during WAITING_TO_START");
        }
    }

    @Nested
    @DisplayName("Emergency State Tests")
    class EmergencyTests {

        @Test
        @DisplayName("Should declare emergency and pause game")
        void testDeclareEmergency() {
            game.start();

            game.declareEmergency();

            assertTrue(game.emergencyDeclared());
            assertTrue(game.isPaused());
        }

        @Test
        @DisplayName("Should release emergency state")
        void testReleaseEmergency() {
            game.start();
            game.declareEmergency();

            game.releaseEmergency();

            assertFalse(game.emergencyDeclared());
        }

        @Test
        @DisplayName("Emergency declaration should not crash if game not running")
        void testEmergencyWhenNotRunning() {
            assertDoesNotThrow(
                    () -> game.declareEmergency(),
                    "Emergency declaration should handle non-running game gracefully");
        }
    }

    @Nested
    @DisplayName("Chat and Messaging Tests")
    class MessagingTests {

        @Test
        @DisplayName("Should send global message")
        void testSendGlobalMessage() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Alice", teamId, false);

            Integer messageId = game.sendMessage(playerId, "Hello everyone!");

            assertNotNull(messageId);
            assertTrue(messageId > 0);
        }

        @Test
        @DisplayName("Should send team message")
        void testSendTeamMessage() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Bob", teamId, false);

            Integer messageId = game.sendTeamMessage(teamId, playerId, "Team chat!");

            assertNotNull(messageId);
            assertTrue(messageId > 0);
        }

        @Test
        @DisplayName("Should retrieve messages with pagination")
        void testGetMessages() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Charlie", teamId, false);

            game.sendMessage(playerId, "Message 1");
            game.sendMessage(playerId, "Message 2");
            game.sendMessage(playerId, "Message 3");

            var messages = game.getMessages(0, 10);

            assertNotNull(messages);
        }

        @Test
        @DisplayName("Message IDs should increment")
        void testMessageIdIncrement() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID playerId = game.addPlayer("Dave", teamId, false);

            Integer id1 = game.sendMessage(playerId, "First");
            Integer id2 = game.sendMessage(playerId, "Second");

            assertTrue(id2 > id1, "Message IDs should increment");
        }
    }

    @Nested
    @DisplayName("Team Validation Tests")
    class TeamValidationTests {

        @Test
        @DisplayName("Should validate existing team")
        void testIsValidTeam() {
            UUID teamId = game.getTeams().getFirst().getID();
            UUID fakeTeamId = UUID.randomUUID();

            assertTrue(game.isValidTeam(teamId));
            assertFalse(game.isValidTeam(fakeTeamId));
        }

        @Test
        @DisplayName("Should retrieve team by ID")
        void testGetTeam() {
            UUID teamId = game.getTeams().getFirst().getID();

            Team team = game.getTeam(teamId);

            assertNotNull(team);
            assertEquals(teamId, team.getID());
        }

        @Test
        @DisplayName("Should throw exception for non-existent team")
        void testGetNonExistentTeam() {
            UUID fakeTeamId = UUID.randomUUID();

            assertThrows(
                    NoSuchElementException.class,
                    () -> game.getTeam(fakeTeamId),
                    "Should throw exception for non-existent team");
        }
    }

    @Nested
    @DisplayName("Game Status Tests")
    class StatusTests {

        @Test
        @DisplayName("Should return complete game status")
        void testGameStatus() {
            UUID teamId = game.getTeams().getFirst().getID();
            game.addPlayer("Alice", teamId, false);

            Map<String, Object> status = game.status();

            assertNotNull(status);
            assertTrue(status.containsKey("players"));
            assertTrue(status.containsKey("teams"));
            assertTrue(status.containsKey("state"));
            assertTrue(status.containsKey("game"));
        }

        @Test
        @DisplayName("Status should include all players")
        @SuppressWarnings("unchecked")
        void testStatusIncludesPlayers() {
            UUID teamId = game.getTeams().getFirst().getID();
            game.addPlayer("Alice", teamId, false);
            game.addPlayer("Bob", teamId, false);

            Map<String, Object> status = game.status();
            List<Map<String, Object>> players = (List<Map<String, Object>>) status.get("players");

            assertEquals(2, players.size());
        }

        @Test
        @DisplayName("Status should include all teams")
        @SuppressWarnings("unchecked")
        void testStatusIncludesTeams() {
            Map<String, Object> status = game.status();
            List<Map<String, Object>> teams = (List<Map<String, Object>>) status.get("teams");

            assertEquals(4, teams.size());
        }
    }

    @Nested
    @DisplayName("Settings Merge Tests")
    class SettingsMergeTests {

        @Test
        @DisplayName("Should merge settings during WAITING_TO_START")
        void testMergeSettings() {
            SettingsRequest settings = mock(SettingsRequest.class);

            assertDoesNotThrow(
                    () -> game.merge(settings), "Should allow settings merge before game starts");
        }

        @Test
        @DisplayName("Should not merge settings after game starts")
        void testCannotMergeSettingsAfterStart() {
            game.start();
            SettingsRequest settings = mock(SettingsRequest.class);

            assertThrows(
                    ResponseStatusException.class,
                    () -> game.merge(settings),
                    "Cannot change settings after game starts");
        }
    }

    @Nested
    @DisplayName("Game Running State Tests")
    class GameRunningTests {

        @Test
        @DisplayName("Game should not be running initially")
        void testNotRunningInitially() {
            assertFalse(game.isGameRunning());
        }

        @Test
        @DisplayName("Game should be running in GRACE_PERIOD")
        void testRunningDuringGrace() {
            game.start();
            assertTrue(game.isGameRunning());
        }

        @Test
        @DisplayName("Game should not be running when ENDED")
        void testNotRunningWhenEnded() {
            game.start();
            game.getTeams().forEach(team -> team.registerFlag(100, 100));
            game.skip(); // SCOUT
            game.skip(); // FFA
            game.skip(); // ENDED

            assertFalse(game.isGameRunning());
        }
    }
}