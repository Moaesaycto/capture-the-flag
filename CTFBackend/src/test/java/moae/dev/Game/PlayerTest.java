package moae.dev.Game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import java.util.UUID;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

  @Nested
  @DisplayName("Player Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("Should create player with valid parameters")
    void testPlayerCreation() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Alice", teamId, false);

      assertNotNull(player.getID(), "Player ID should not be null");
      assertEquals("Alice", player.getName(), "Player name should match");
      assertEquals(teamId, player.getTeam(), "Team ID should match");
      assertFalse(player.isAuth(), "Player should not be authenticated");
    }

    @Test
    @DisplayName("Should create authenticated player")
    void testAuthenticatedPlayerCreation() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Admin", teamId, true);

      assertTrue(player.isAuth(), "Player should be authenticated");
    }

    @Test
    @DisplayName("Should generate unique IDs for different players")
    void testUniquePlayerIDs() {
      UUID teamId = UUID.randomUUID();
      Player player1 = new Player("Alice", teamId, false);
      Player player2 = new Player("Bob", teamId, false);

      assertNotEquals(
          player1.getID(), player2.getID(), "Different players should have different IDs");
    }

    @Test
    @DisplayName("Should handle empty player names")
    void testEmptyPlayerName() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("", teamId, false);

      assertEquals("", player.getName(), "Empty name should be allowed");
      assertNotNull(player.getID(), "Player with empty name should still have valid ID");
    }

    @Test
    @DisplayName("Should handle special characters in player names")
    void testSpecialCharactersInName() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Alice-123_@#$", teamId, false);

      assertEquals(
          "Alice-123_@#$",
          player.getName(),
          "Player name with special characters should be preserved");
    }

    @Test
    @DisplayName("Should handle unicode characters in player names")
    void testUnicodeInName() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("玩家Alice", teamId, false);

      assertEquals("玩家Alice", player.getName(), "Player name with unicode should be preserved");
    }
  }

  @Nested
  @DisplayName("Team Association Tests")
  class TeamAssociationTests {

    @Test
    @DisplayName("Should correctly identify player is on their team")
    void testPlayerIsOnCorrectTeam() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Bob", teamId, true);

      assertTrue(player.isOnTeam(teamId), "Player should be on their assigned team");
    }

    @Test
    @DisplayName("Should correctly identify player is not on different team")
    void testPlayerIsNotOnDifferentTeam() {
      UUID teamId = UUID.randomUUID();
      UUID differentTeamId = UUID.randomUUID();
      Player player = new Player("Bob", teamId, true);

      assertFalse(player.isOnTeam(differentTeamId), "Player should not be on a different team");
    }

    @Test
    @DisplayName("Should handle null team check gracefully")
    void testNullTeamCheck() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Charlie", teamId, false);

      assertFalse(player.isOnTeam(null), "Player should not be on null team");
    }

    @Test
    @DisplayName("Multiple players can be on same team")
    void testMultiplePlayersOnSameTeam() {
      UUID teamId = UUID.randomUUID();
      Player player1 = new Player("Alice", teamId, false);
      Player player2 = new Player("Bob", teamId, false);

      assertTrue(player1.isOnTeam(teamId));
      assertTrue(player2.isOnTeam(teamId));
      assertEquals(player1.getTeam(), player2.getTeam(), "Both players should be on the same team");
    }
  }

  @Nested
  @DisplayName("Player Map Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should convert player to map with all fields")
    void testPlayerToMap() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Charlie", teamId, false);

      Map<String, Object> map = player.toMap();

      assertEquals(4, map.size(), "Map should contain exactly 4 fields");
      assertEquals("Charlie", map.get("name"));
      assertEquals(teamId, map.get("team"));
      assertEquals(false, map.get("auth"));
      assertNotNull(map.get("id"));
      assertTrue(map.get("id") instanceof UUID, "ID should be a UUID");
    }

    @Test
    @DisplayName("Should preserve auth status in map for authenticated player")
    void testAuthPlayerToMap() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Admin", teamId, true);

      Map<String, Object> map = player.toMap();

      assertEquals(true, map.get("auth"), "Auth status should be true");
    }

    @Test
    @DisplayName("Map should contain actual player ID")
    void testMapContainsCorrectID() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Dave", teamId, false);

      Map<String, Object> map = player.toMap();

      assertEquals(player.getID(), map.get("id"), "Map ID should match player's actual ID");
    }

    @Test
    @DisplayName("Multiple toMap calls should return consistent data")
    void testToMapConsistency() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Eve", teamId, true);

      Map<String, Object> map1 = player.toMap();
      Map<String, Object> map2 = player.toMap();

      assertEquals(map1.get("id"), map2.get("id"), "ID should be consistent");
      assertEquals(map1.get("name"), map2.get("name"), "Name should be consistent");
      assertEquals(map1.get("team"), map2.get("team"), "Team should be consistent");
      assertEquals(map1.get("auth"), map2.get("auth"), "Auth should be consistent");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return exact name provided at creation")
    void testGetName() {
      UUID teamId = UUID.randomUUID();
      String expectedName = "TestPlayer123";
      Player player = new Player(expectedName, teamId, false);

      assertEquals(expectedName, player.getName());
    }

    @Test
    @DisplayName("getTeam should return exact team ID provided at creation")
    void testGetTeam() {
      UUID expectedTeamId = UUID.randomUUID();
      Player player = new Player("Frank", expectedTeamId, false);

      assertEquals(expectedTeamId, player.getTeam());
    }

    @Test
    @DisplayName("getID should return non-null UUID")
    void testGetID() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Grace", teamId, false);

      UUID playerId = player.getID();
      assertNotNull(playerId);
      assertDoesNotThrow(() -> UUID.fromString(playerId.toString()), "ID should be a valid UUID");
    }

    @Test
    @DisplayName("isAuth should match authentication status")
    void testIsAuth() {
      UUID teamId = UUID.randomUUID();
      Player normalPlayer = new Player("Normal", teamId, false);
      Player authPlayer = new Player("Auth", teamId, true);

      assertFalse(normalPlayer.isAuth());
      assertTrue(authPlayer.isAuth());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Boundary Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle very long player names")
    void testLongPlayerName() {
      UUID teamId = UUID.randomUUID();
      String longName = "A".repeat(1000);
      Player player = new Player(longName, teamId, false);

      assertEquals(longName, player.getName(), "Very long names should be preserved");
    }

    @Test
    @DisplayName("Should handle player name with only whitespace")
    void testWhitespaceOnlyName() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("   ", teamId, false);

      assertEquals("   ", player.getName(), "Whitespace-only names should be preserved");
    }

    @Test
    @DisplayName("Player properties should be immutable after creation")
    void testImmutability() {
      UUID teamId = UUID.randomUUID();
      Player player = new Player("Immutable", teamId, true);

      UUID originalId = player.getID();
      String originalName = player.getName();
      UUID originalTeam = player.getTeam();
      boolean originalAuth = player.isAuth();

      // Call getters multiple times
      player.getID();
      player.getName();
      player.getTeam();
      player.isAuth();

      // Verify nothing changed
      assertEquals(originalId, player.getID());
      assertEquals(originalName, player.getName());
      assertEquals(originalTeam, player.getTeam());
      assertEquals(originalAuth, player.isAuth());
    }
  }
}
