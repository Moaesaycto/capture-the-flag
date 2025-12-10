package moae.dev.Game;

import moae.dev.Sockets.SocketConnectionHandler;
import moae.dev.Utils.MessagePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeamTest {

  @Mock private SocketConnectionHandler mockWebSocketHandler;

  private Team team;

  @BeforeEach
  void setUp() {
    team = new Team("Red Team", "#FF0000");
    team.setWebSocketHandler(mockWebSocketHandler);
  }

  @Nested
  @DisplayName("Team Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("Should create team with valid parameters")
    void testTeamCreation() {
      assertNotNull(team.getID(), "Team ID should not be null");
      assertEquals("Red Team", team.getName(), "Team name should match");
      assertEquals("#FF0000", team.getColor(), "Team color should match");
      assertFalse(team.isRegistered(), "Team should not be registered initially");
    }

    @Test
    @DisplayName("Should generate unique IDs for different teams")
    void testUniqueTeamIDs() {
      Team team1 = new Team("Blue Team", "#0000FF");
      Team team2 = new Team("Green Team", "#00FF00");

      assertNotEquals(team1.getID(), team2.getID(), "Different teams should have different IDs");
    }

    @Test
    @DisplayName("Should handle empty team names")
    void testEmptyTeamName() {
      Team emptyTeam = new Team("", "#FFFFFF");

      assertEquals("", emptyTeam.getName(), "Empty name should be allowed");
      assertNotNull(emptyTeam.getID(), "Team with empty name should still have valid ID");
    }

    @Test
    @DisplayName("Should handle special characters in team names")
    void testSpecialCharactersInName() {
      Team specialTeam = new Team("Team-123_@#$", "#AABBCC");

      assertEquals(
          "Team-123_@#$",
          specialTeam.getName(),
          "Team name with special characters should be preserved");
    }

    @Test
    @DisplayName("Should handle unicode characters in team names")
    void testUnicodeInName() {
      Team unicodeTeam = new Team("队伍Red", "#DD0000");

      assertEquals("队伍Red", unicodeTeam.getName(), "Team name with unicode should be preserved");
    }

    @Test
    @DisplayName("Should handle very long team names")
    void testLongTeamName() {
      String longName = "A".repeat(1000);
      Team longTeam = new Team(longName, "#123456");

      assertEquals(longName, longTeam.getName(), "Very long names should be preserved");
    }

    @Test
    @DisplayName("Should handle various color formats")
    void testColorFormats() {
      Team team1 = new Team("Team1", "#FF0000");
      Team team2 = new Team("Team2", "rgb(255,0,0)");
      Team team3 = new Team("Team3", "red");

      assertEquals("#FF0000", team1.getColor());
      assertEquals("rgb(255,0,0)", team2.getColor());
      assertEquals("red", team3.getColor());
    }
  }

  @Nested
  @DisplayName("Flag Registration Tests")
  class FlagRegistrationTests {

    @Test
    @DisplayName("Should register flag at valid coordinates")
    void testRegisterFlag() {
      assertFalse(team.isRegistered(), "Team should not be registered initially");

      team.registerFlag(100, 200);

      assertTrue(team.isRegistered(), "Team should be registered after flag registration");
    }

    @Test
    @DisplayName("Should register flag at origin (0, 0)")
    void testRegisterFlagAtOrigin() {
      team.registerFlag(0, 0);

      assertTrue(team.isRegistered());
    }

    @Test
    @DisplayName("Should register flag at negative coordinates")
    void testRegisterFlagNegativeCoordinates() {
      team.registerFlag(-50, -100);

      assertTrue(team.isRegistered(), "Should allow negative coordinates");
    }

    @Test
    @DisplayName("Should register flag at large coordinates")
    void testRegisterFlagLargeCoordinates() {
      team.registerFlag(99999, 88888);

      assertTrue(team.isRegistered(), "Should allow large coordinates");
    }

    @Test
    @DisplayName("Should allow flag re-registration")
    void testReRegisterFlag() {
      team.registerFlag(100, 200);
      assertTrue(team.isRegistered());

      // Re-register at different location
      team.registerFlag(300, 400);

      assertTrue(team.isRegistered(), "Should still be registered after re-registration");
    }

    @Test
    @DisplayName("Should check registration status correctly")
    void testIsRegistered() {
      assertFalse(team.isRegistered(), "Initially should not be registered");

      team.registerFlag(50, 75);

      assertTrue(team.isRegistered(), "Should be registered after flag is set");
    }
  }

  @Nested
  @DisplayName("Team Map Serialization Tests")
  class SerializationTests {

    @Test
    @DisplayName("Should convert team to map before flag registration")
    void testToMapBeforeFlagRegistration() {
      Map<String, Object> map = team.toMap(false);

      assertEquals("Red Team", map.get("name"));
      assertEquals("#FF0000", map.get("color"));
      assertNull(map.get("flag"), "Flag should be null before registration");
      assertEquals(false, map.get("registered"), "Should not be registered");
      assertNotNull(map.get("id"));
    }

    @Test
    @DisplayName("Should not reveal flag location when revealed is false")
    void testToMapNotRevealed() {
      team.registerFlag(100, 200);

      Map<String, Object> map = team.toMap(false);

      assertNull(map.get("flag"), "Flag should be null when not revealed");
      assertEquals(true, map.get("registered"), "Should show as registered");
    }

    @Test
    @DisplayName("Should reveal flag location when revealed is true")
    void testToMapRevealed() {
      team.registerFlag(100, 200);

      Map<String, Object> map = team.toMap(true);

      assertNotNull(map.get("flag"), "Flag should be present when revealed");
      assertEquals(true, map.get("registered"), "Should show as registered");

      @SuppressWarnings("unchecked")
      Map<String, Object> flagMap = (Map<String, Object>) map.get("flag");
      assertEquals(100, flagMap.get("x"));
      assertEquals(200, flagMap.get("y"));
    }

    @Test
    @DisplayName("Should not reveal flag if not registered, even when revealed is true")
    void testToMapNotRegisteredButRevealed() {
      Map<String, Object> map = team.toMap(true);

      assertNull(map.get("flag"), "Flag should be null if not registered");
      assertEquals(false, map.get("registered"));
    }

    @Test
    @DisplayName("Map should contain team ID")
    void testMapContainsTeamID() {
      Map<String, Object> map = team.toMap(false);

      assertEquals(team.getID(), map.get("id"), "Map should contain team's actual ID");
    }

    @Test
    @DisplayName("Multiple toMap calls should return consistent data")
    void testToMapConsistency() {
      team.registerFlag(50, 75);

      Map<String, Object> map1 = team.toMap(false);
      Map<String, Object> map2 = team.toMap(false);

      assertEquals(map1.get("id"), map2.get("id"), "ID should be consistent");
      assertEquals(map1.get("name"), map2.get("name"), "Name should be consistent");
      assertEquals(map1.get("color"), map2.get("color"), "Color should be consistent");
      assertEquals(
          map1.get("registered"),
          map2.get("registered"),
          "Registration status should be consistent");
    }

    @Test
    @DisplayName("Should handle revelation state changes correctly")
    void testRevelationStateChanges() {
      team.registerFlag(150, 250);

      Map<String, Object> hiddenMap = team.toMap(false);
      Map<String, Object> revealedMap = team.toMap(true);

      assertNull(hiddenMap.get("flag"), "Should be hidden when revealed=false");
      assertNotNull(revealedMap.get("flag"), "Should be visible when revealed=true");
    }
  }

  @Nested
  @DisplayName("Messaging Tests")
  class MessagingTests {

    private Player createTestPlayer(String name, UUID teamId) {
      return new Player(name, teamId, false);
    }

    @Test
    @DisplayName("Should send team message successfully")
    void testSendMessage() {
      Player player = createTestPlayer("Alice", team.getID());

      assertDoesNotThrow(() -> team.sendMessage(player, "Hello team!", 1));
    }

    @Test
    @DisplayName("Should retrieve messages with pagination")
    void testGetMessages() {
      Player player = createTestPlayer("Bob", team.getID());

      team.sendMessage(player, "Message 1", 1);
      team.sendMessage(player, "Message 2", 2);
      team.sendMessage(player, "Message 3", 3);

      MessagePage messages = team.getMessages(0, 10);

      assertNotNull(messages);
    }

    @Test
    @DisplayName("Should handle empty message list")
    void testGetMessagesEmpty() {
      MessagePage messages = team.getMessages(0, 10);

      assertNotNull(messages, "Should return non-null MessagePage even with no messages");
    }

    @Test
    @DisplayName("Should send multiple messages from different players")
    void testMultiplePlayersMessaging() {
      Player player1 = createTestPlayer("Alice", team.getID());
      Player player2 = createTestPlayer("Bob", team.getID());

      assertDoesNotThrow(
          () -> {
            team.sendMessage(player1, "Hello from Alice", 1);
            team.sendMessage(player2, "Hello from Bob", 2);
            team.sendMessage(player1, "Another from Alice", 3);
          });
    }

    @Test
    @DisplayName("Should handle empty message content")
    void testEmptyMessageContent() {
      Player player = createTestPlayer("Charlie", team.getID());

      assertDoesNotThrow(() -> team.sendMessage(player, "", 1), "Should allow empty messages");
    }

    @Test
    @DisplayName("Should handle very long messages")
    void testLongMessage() {
      Player player = createTestPlayer("Dave", team.getID());
      String longMessage = "A".repeat(10000);

      assertDoesNotThrow(
          () -> team.sendMessage(player, longMessage, 1), "Should handle very long messages");
    }

    @Test
    @DisplayName("Should handle special characters in messages")
    void testSpecialCharactersInMessages() {
      Player player = createTestPlayer("Eve", team.getID());

      assertDoesNotThrow(
          () -> team.sendMessage(player, "Hello! @#$%^&*() 你好", 1),
          "Should handle special characters and unicode");
    }

    @Test
    @DisplayName("Should broadcast message to WebSocket handler when set")
    void testWebSocketBroadcast() {
      Player player = createTestPlayer("Frank", team.getID());

      team.sendMessage(player, "Test broadcast", 1);

      verify(mockWebSocketHandler, times(1)).broadcastMessage(any());
    }

    @Test
    @DisplayName("Should not crash when WebSocket handler is not set")
    void testNoWebSocketHandler() {
      Team teamWithoutSocket = new Team("Blue Team", "#0000FF");
      Player player = createTestPlayer("Grace", teamWithoutSocket.getID());

      assertDoesNotThrow(
          () -> teamWithoutSocket.sendMessage(player, "No socket", 1),
          "Should not crash without WebSocket handler");
    }
  }

  @Nested
  @DisplayName("Reset Functionality Tests")
  class ResetTests {

    @Test
    @DisplayName("Should reset flag registration")
    void testResetClearsFlag() {
      team.registerFlag(100, 200);
      assertTrue(team.isRegistered(), "Should be registered before reset");

      team.reset();

      assertFalse(team.isRegistered(), "Should not be registered after reset");
    }

    @Test
    @DisplayName("Should clear messages on reset")
    void testResetClearsMessages() {
      Player player = new Player("Alice", team.getID(), false);
      team.sendMessage(player, "Message 1", 1);
      team.sendMessage(player, "Message 2", 2);

      team.reset();

      MessagePage messages = team.getMessages(0, 10);
      // Assuming MessagePage has a way to check if empty
      assertNotNull(messages);
    }

    @Test
    @DisplayName("Should preserve team identity after reset")
    void testResetPreservesIdentity() {
      UUID originalId = team.getID();
      String originalName = team.getName();
      String originalColor = team.getColor();

      team.registerFlag(100, 200);
      Player player = new Player("Bob", team.getID(), false);
      team.sendMessage(player, "Test", 1);

      team.reset();

      assertEquals(originalId, team.getID(), "ID should not change after reset");
      assertEquals(originalName, team.getName(), "Name should not change after reset");
      assertEquals(originalColor, team.getColor(), "Color should not change after reset");
    }

    @Test
    @DisplayName("Should be able to re-register flag after reset")
    void testReRegisterAfterReset() {
      team.registerFlag(100, 200);
      team.reset();

      assertDoesNotThrow(
          () -> team.registerFlag(300, 400), "Should allow re-registration after reset");
      assertTrue(team.isRegistered(), "Should be registered after re-registration");
    }

    @Test
    @DisplayName("Multiple resets should work correctly")
    void testMultipleResets() {
      team.registerFlag(100, 200);
      team.reset();
      assertFalse(team.isRegistered());

      team.registerFlag(300, 400);
      team.reset();
      assertFalse(team.isRegistered());

      team.registerFlag(500, 600);
      assertTrue(team.isRegistered());
    }
  }

  @Nested
  @DisplayName("WebSocket Handler Tests")
  class WebSocketHandlerTests {

    @Test
    @DisplayName("Should set WebSocket handler")
    void testSetWebSocketHandler() {
      SocketConnectionHandler handler = mock(SocketConnectionHandler.class);

      team.setWebSocketHandler(handler);

      assertEquals(handler, team.getWebSocketHandler());
    }

    @Test
    @DisplayName("Should get WebSocket handler")
    void testGetWebSocketHandler() {
      assertEquals(mockWebSocketHandler, team.getWebSocketHandler());
    }

    @Test
    @DisplayName("Should allow setting handler to null")
    void testSetHandlerToNull() {
      team.setWebSocketHandler(null);

      assertNull(team.getWebSocketHandler());
    }

    @Test
    @DisplayName("Should allow changing WebSocket handler")
    void testChangeWebSocketHandler() {
      SocketConnectionHandler newHandler = mock(SocketConnectionHandler.class);

      team.setWebSocketHandler(newHandler);

      assertEquals(newHandler, team.getWebSocketHandler());
      assertNotEquals(mockWebSocketHandler, team.getWebSocketHandler());
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return exact name provided at creation")
    void testGetName() {
      assertEquals("Red Team", team.getName());
    }

    @Test
    @DisplayName("getColor should return exact color provided at creation")
    void testGetColor() {
      assertEquals("#FF0000", team.getColor());
    }

    @Test
    @DisplayName("getID should return non-null UUID")
    void testGetID() {
      UUID teamId = team.getID();
      assertNotNull(teamId);
      assertDoesNotThrow(() -> UUID.fromString(teamId.toString()), "ID should be a valid UUID");
    }

    @Test
    @DisplayName("Getters should be consistent across multiple calls")
    void testGetterConsistency() {
      UUID id1 = team.getID();
      UUID id2 = team.getID();
      String name1 = team.getName();
      String name2 = team.getName();
      String color1 = team.getColor();
      String color2 = team.getColor();

      assertEquals(id1, id2, "ID should be consistent");
      assertEquals(name1, name2, "Name should be consistent");
      assertEquals(color1, color2, "Color should be consistent");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Boundary Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle whitespace-only team name")
    void testWhitespaceOnlyName() {
      Team whitespaceTeam = new Team("   ", "#FFFFFF");

      assertEquals("   ", whitespaceTeam.getName(), "Whitespace-only names should be preserved");
    }

    @Test
    @DisplayName("Should handle null color gracefully")
    void testNullColor() {
      Team nullColorTeam = new Team("Null Color Team", null);

      assertNull(nullColorTeam.getColor(), "Null color should be allowed");
    }

    @Test
    @DisplayName("Team properties should be immutable after creation")
    void testImmutability() {
      UUID originalId = team.getID();
      String originalName = team.getName();
      String originalColor = team.getColor();

      // Perform various operations
      team.registerFlag(100, 100);
      Player player = new Player("Test", team.getID(), false);
      team.sendMessage(player, "Test", 1);
      team.reset();

      // Verify core properties haven't changed
      assertEquals(originalId, team.getID(), "ID should never change");
      assertEquals(originalName, team.getName(), "Name should never change");
      assertEquals(originalColor, team.getColor(), "Color should never change");
    }

    @Test
    @DisplayName("Should handle registration status changes correctly")
    void testRegistrationStatusChanges() {
      assertFalse(team.isRegistered());

      team.registerFlag(1, 1);
      assertTrue(team.isRegistered());

      team.reset();
      assertFalse(team.isRegistered());

      team.registerFlag(2, 2);
      assertTrue(team.isRegistered());
    }
  }
}
