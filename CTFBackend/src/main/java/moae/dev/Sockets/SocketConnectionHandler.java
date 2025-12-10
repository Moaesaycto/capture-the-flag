package moae.dev.Sockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import moae.dev.Utils.ChatMessage;
import moae.dev.Game.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SocketConnectionHandler extends TextWebSocketHandler {
  private final AtomicLong messageIdCounter = new AtomicLong(0);

  public final List<WebSocketSession> webSocketSessions =
      Collections.synchronizedList(new ArrayList<>());
  protected final Logger logger = LoggerFactory.getLogger(SocketConnectionHandler.class);
  protected final Game game;

  public SocketConnectionHandler(Game game) {
    this.game = game;
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
    super.afterConnectionEstablished(session);
    // System.out.println(session.getId() + " Connected to " + session.getUri());
    webSocketSessions.add(session);
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status)
      throws Exception {
    super.afterConnectionClosed(session, status);
    // System.out.println(session.getId() + " Disconnected from " + session.getUri());
    webSocketSessions.remove(session);
  }

  @Override
  public void handleMessage(@NonNull WebSocketSession session, @NonNull WebSocketMessage<?> message)
      throws Exception {}

  public void broadcastMessage(ChatMessage message) {
    ObjectMapper mapper = new ObjectMapper();
    String json;
    try {
      json = mapper.writeValueAsString(message);
    } catch (Exception e) {
      return;
    }

    synchronized (webSocketSessions) {
      for (WebSocketSession session : webSocketSessions) {
        if (session.isOpen()) {
          try {
            session.sendMessage(new TextMessage(json));
          } catch (Exception ignored) {
          }
        }
      }
    }
  }

  @PreDestroy
  public void cleanup() {
    // No cleanup needed just yet
  }
}
