package feige.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feige.service.ApiException;
import feige.service.ChatService;
import feige.service.MessageFanout;
import feige.service.MessageView;
import feige.service.PresenceService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chats;
    private final MessageFanout fanout;
    private final SocketHub hub;
    private final PresenceService presence;
    private final ObjectMapper mapper;

    public ChatWebSocketHandler(ChatService chats, MessageFanout fanout, SocketHub hub,
                                PresenceService presence, ObjectMapper mapper) {
        this.chats = chats;
        this.fanout = fanout;
        this.hub = hub;
        this.presence = presence;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = userId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        hub.add(userId, session);
        if (presence.connected(userId)) {
            hub.sendToAll(presence(userId, username(session), true));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        hub.remove(userId, session);
        if (presence.disconnected(userId)) {
            hub.sendToAll(presence(userId, username(session), false));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = userId(session);
        if (userId == null) {
            hub.sendToSession(session, error("未登录"));
            return;
        }
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            if (!"send".equals(node.path("type").asText())) {
                hub.sendToSession(session, error("无法识别的消息"));
                return;
            }
            Long quoteId = node.has("quoteMessageId") && !node.get("quoteMessageId").isNull()
                    ? node.get("quoteMessageId").asLong()
                    : null;
            MessageView view = chats.sendText(userId, node.path("conversationId").asLong(),
                    node.path("text").asText(""), quoteId);
            fanout.deliver(view);
        } catch (ApiException ex) {
            hub.sendToSession(session, error(ex.getMessage()));
        } catch (Exception ex) {
            hub.sendToSession(session, error("发送失败"));
        }
    }

    private static Long userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        return value instanceof Long ? (Long) value : null;
    }

    private static String username(WebSocketSession session) {
        Object value = session.getAttributes().get("username");
        return value == null ? "" : value.toString();
    }

    private static Map<String, Object> presence(long userId, String username, boolean online) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "presence");
        payload.put("userId", userId);
        payload.put("username", username);
        payload.put("online", online);
        return payload;
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "error");
        payload.put("message", message);
        return payload;
    }
}
