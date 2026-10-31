package feige.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feige.service.ClientPush;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SocketHub implements ClientPush {

    private static final Logger log = LoggerFactory.getLogger(SocketHub.class);

    private final ObjectMapper mapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<Long, Set<WebSocketSession>>();

    public SocketHub(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void add(long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remove(long userId, WebSocketSession session) {
        Set<WebSocketSession> group = sessions.get(userId);
        if (group == null) {
            return;
        }
        group.remove(session);
        if (group.isEmpty()) {
            sessions.remove(userId, group);
        }
    }

    public void closeUser(long userId) {
        Set<WebSocketSession> group = sessions.remove(userId);
        if (group == null) {
            return;
        }
        for (WebSocketSession session : group) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException ignored) {
                // 连接已经断开
            }
        }
    }

    public void sendToSession(WebSocketSession session, Object payload) {
        String json = toJson(payload);
        if (json != null) {
            send(session, json);
        }
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, Object payload) {
        String json = toJson(payload);
        if (json == null || userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            Set<WebSocketSession> group = sessions.get(userId);
            if (group == null) {
                continue;
            }
            for (WebSocketSession session : snapshot(group)) {
                send(session, json);
            }
        }
    }

    @Override
    public void sendToAll(Object payload) {
        String json = toJson(payload);
        if (json == null) {
            return;
        }
        for (Set<WebSocketSession> group : new ArrayList<Set<WebSocketSession>>(sessions.values())) {
            for (WebSocketSession session : snapshot(group)) {
                send(session, json);
            }
        }
    }

    private List<WebSocketSession> snapshot(Set<WebSocketSession> group) {
        return new ArrayList<WebSocketSession>(group);
    }

    private void send(WebSocketSession session, String json) {
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(json));
            } catch (IOException ex) {
                log.debug("推送失败", ex);
            }
        }
    }

    private String toJson(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("消息无法序列化", ex);
            return null;
        }
    }
}
