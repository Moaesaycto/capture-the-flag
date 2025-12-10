package moae.dev.Sockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import moae.dev.Game.Game;
import moae.dev.Utils.StateMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.lang.NonNull;

import java.io.IOException;

public class StateSocketConnectionHandler extends SocketConnectionHandler {
  private static StateSocketConnectionHandler instance;

  public StateSocketConnectionHandler(Game game) {
    super(game);
    instance = this;
  }

  @Override
  public void handleMessage(
      @NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message) {
    // Do nothing
  }

  public static void broadcast(StateMessage message) {
    ObjectMapper mapper = new ObjectMapper();
    String json;

    try {
      json = mapper.writeValueAsString(message);
    } catch (Exception e) {
      return;
    }

    if (instance != null) {
      synchronized (instance.webSocketSessions) {
        for (WebSocketSession session : instance.webSocketSessions) {
          try {
            if (session.isOpen()) {
              session.sendMessage(new TextMessage(json));
            }
          } catch (IOException e) {
            instance.logger.error(
                "Error sending broadcast message to session {}", session.getId(), e);
          }
        }
      }
    }
  }
}
