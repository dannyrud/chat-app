package chat_app;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import chat_app.utils.JwtUtil;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final Map<WebSocketSession, String> userMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String authHeader = session.getHandshakeHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            session.close(new CloseStatus(1008, "Missing or invalid Authorization header"));
            return;
        }
        String token = authHeader.substring(7);
        String username = JwtUtil.validateToken(token);

        if (username == null) {
            session.close(new CloseStatus(1008, "Invalid or expired token"));
            return;
        }

        userMap.put(session, username);
        sessions.add(session);
        broadcast("🔵 " + username + ": joined the chat!");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String username = userMap.get(session);
        if (username != null) {
            broadcast("💬 " + username + ": " + message.getPayload());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = userMap.remove(session);
        sessions.remove(session);
        if (username != null) {
            broadcast("🔴 " + username + ": left the chat.");
        }
    }

    private void broadcast(String message) {
        int firstSpaceIndex = message.indexOf(" ");
        int colonIndex = message.indexOf(":");

        String emoji = message.substring(0, firstSpaceIndex).trim();
        String username = message.substring(firstSpaceIndex + 1, colonIndex).trim();
        String cleanMessage = message.substring(colonIndex + 2).trim();

        for (WebSocketSession session : sessions) {
            try {
                if(username.equals(userMap.get(session))) {
                    String newMessage = emoji + " You: " + cleanMessage;
                    session.sendMessage(new TextMessage(newMessage));
                } else {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
