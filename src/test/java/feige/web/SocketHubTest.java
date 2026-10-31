package feige.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocketHubTest {

    @Test
    void sendsOnlyToTheRequestedUsers() throws Exception {
        SocketHub hub = new SocketHub(new ObjectMapper());
        WebSocketSession first = openSession();
        WebSocketSession second = openSession();
        WebSocketSession third = openSession();
        hub.add(1L, first);
        hub.add(2L, second);
        hub.add(3L, third);
        hub.sendToUsers(Arrays.asList(1L, 2L), Collections.singletonMap("type", "message"));
        verify(first).sendMessage(any(TextMessage.class));
        verify(second).sendMessage(any(TextMessage.class));
        verify(third, never()).sendMessage(any(TextMessage.class));
    }

    private static WebSocketSession openSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
