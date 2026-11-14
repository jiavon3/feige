package feige;

import feige.domain.UserAccount;
import feige.service.ApiException;
import feige.service.ChatService;
import feige.service.ConversationView;
import feige.service.MessageView;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatFlowTest extends AbstractFeigeTest {

    @Test
    void directConversationIsUniqueForAPair() {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        long first = chatService.openDirect(left.getId(), right.getId()).getId();
        long second = chatService.openDirect(right.getId(), left.getId()).getId();
        assertEquals(first, second);
        assertEquals(1, conversationRepository.findDirectBetween(
                Math.min(left.getId(), right.getId()),
                Math.max(left.getId(), right.getId())).size());
    }

    @Test
    void outsiderCannotSendIntoDirectChat() {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        UserAccount outsider = accountService.register("王五", "secret1");
        ConversationView direct = chatService.openDirect(left.getId(), right.getId());
        ApiException ex = assertThrows(ApiException.class,
                () -> chatService.sendText(outsider.getId(), direct.getId(), "偷看", null));
        assertEquals(ChatService.FORBIDDEN, ex.getMessage());
        assertEquals(0, messageRepository.count());
    }

    @Test
    void blankOrTooLongTextIsNotStored() {
        UserAccount user = accountService.register("张三", "secret1");
        long hallId = chatService.hallId();
        assertThrows(ApiException.class, () -> chatService.sendText(user.getId(), hallId, "   ", null));
        char[] chars = new char[2001];
        Arrays.fill(chars, '啊');
        assertThrows(ApiException.class, () -> chatService.sendText(user.getId(), hallId, new String(chars), null));
        assertEquals(0, messageRepository.count());
        MessageView kept = chatService.sendText(user.getId(), hallId, "  你好  ", null);
        assertEquals("你好", kept.getBody());
    }

    @Test
    void directMessageReachesOnlyTheTwoParticipants() throws Exception {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        UserAccount outsider = accountService.register("王五", "secret1");
        WebSocketSession leftSocket = sessionFor(left);
        WebSocketSession rightSocket = sessionFor(right);
        WebSocketSession outsiderSocket = sessionFor(outsider);
        socketHub.add(left.getId(), leftSocket);
        socketHub.add(right.getId(), rightSocket);
        socketHub.add(outsider.getId(), outsiderSocket);
        ConversationView direct = chatService.openDirect(left.getId(), right.getId());
        chatWebSocketHandler.handleMessage(leftSocket, new TextMessage(
                "{\"type\":\"send\",\"conversationId\":" + direct.getId() + ",\"text\":\"悄悄话\"}"));
        verify(leftSocket, atLeastOnce()).sendMessage(argThat(bodyContaining("悄悄话")));
        verify(rightSocket, atLeastOnce()).sendMessage(argThat(bodyContaining("悄悄话")));
        verify(outsiderSocket, never()).sendMessage(any(WebSocketMessage.class));
        socketHub.closeUser(left.getId());
        socketHub.closeUser(right.getId());
        socketHub.closeUser(outsider.getId());
    }

    @Test
    void clearingConversationRemovesOnlyItsMessagesAndFiles() throws Exception {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        UserAccount outsider = accountService.register("王五", "secret1");
        long hallId = chatService.hallId();
        ConversationView direct = chatService.openDirect(left.getId(), right.getId());
        chatService.sendText(left.getId(), hallId, "大厅保留", null);
        chatService.sendText(left.getId(), direct.getId(), "私聊删除", null);
        chatService.saveUpload(left.getId(), direct.getId(), "note.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), null);
        assertTrue(Files.list(fileStorage.directory().toPath()).findAny().isPresent());

        chatService.clearConversation(left.getId(), direct.getId());

        assertEquals(0, chatService.history(left.getId(), direct.getId(), null, 50).getMessages().size());
        assertEquals(1, chatService.history(left.getId(), hallId, null, 50).getMessages().size());
        assertFalse(Files.list(fileStorage.directory().toPath()).findAny().isPresent());
        ApiException forbidden = assertThrows(ApiException.class,
                () -> chatService.clearConversation(outsider.getId(), direct.getId()));
        assertEquals(ChatService.FORBIDDEN, forbidden.getMessage());
    }

    @Test
    void emptyWebsocketTextRepliesWithErrorAndDoesNotStore() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        WebSocketSession socket = sessionFor(user);
        chatWebSocketHandler.handleMessage(socket, new TextMessage(
                "{\"type\":\"send\",\"conversationId\":" + chatService.hallId() + ",\"text\":\"   \"}"));
        verify(socket).sendMessage(argThat(bodyContaining(ChatService.EMPTY_TEXT)));
        assertEquals(0, messageRepository.count());
    }

    private static WebSocketSession sessionFor(UserAccount user) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("userId", user.getId());
        attributes.put("username", user.getUsername());
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    private static org.mockito.ArgumentMatcher<WebSocketMessage<?>> bodyContaining(String part) {
        return new org.mockito.ArgumentMatcher<WebSocketMessage<?>>() {
            @Override
            public boolean matches(WebSocketMessage<?> argument) {
                return argument instanceof TextMessage && ((TextMessage) argument).getPayload().contains(part);
            }
        };
    }
}
