package moae.dev.Game;

import java.util.Map;
import java.util.UUID;

public class Player {
  private final UUID id;
  private final String name;
  private final UUID team;
  private final boolean auth;

  public Player(String name, UUID team, boolean auth) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.team = team;
    this.auth = auth;
  }

  public String getName() {
    return this.name;
  }

  public UUID getID() {
    return this.id;
  }

  public UUID getTeam() {
    return this.team;
  }

  public boolean isAuth() {
    return this.auth;
  }

  public Map<String, Object> toMap() {
    return Map.of(
        "id", id,
        "name", name,
        "team", team,
        "auth", auth);
  }

  public boolean isOnTeam(UUID cTeam) {
    return this.team.equals(cTeam);
  }
}
