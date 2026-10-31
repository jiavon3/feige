package feige;

import feige.domain.ConversationType;
import feige.domain.UserAccount;
import feige.service.AdminService;
import feige.service.ApiException;
import feige.service.ConversationView;
import feige.service.MessageView;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest extends AbstractFeigeTest {

    @Test
    void deleteUserRemovesTheirMessagesFilesAndDirectChats() throws Exception {
        UserAccount keeper = accountService.register("张三", "secret1");
        UserAccount victim = accountService.register("李四", "secret1");
        long hallId = chatService.hallId();
        chatService.sendText(keeper.getId(), hallId, "留下来", null);
        chatService.sendText(victim.getId(), hallId, "删掉我", null);
        ConversationView direct = chatService.openDirect(keeper.getId(), victim.getId());
        chatService.sendText(keeper.getId(), direct.getId(), "私聊也删", null);
        MessageView file = chatService.saveUpload(victim.getId(), hallId, "note.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), null);
        assertTrue(Files.list(fileStorage.directory().toPath()).findAny().isPresent());

        adminService.deleteUser(victim.getId());

        assertFalse(userRepository.existsByUsernameKey("李四"));
        assertTrue(userRepository.existsByUsernameKey("张三"));
        assertEquals(1, chatService.history(keeper.getId(), hallId, null, 50).getMessages().size());
        assertEquals("留下来", chatService.history(keeper.getId(), hallId, null, 50).getMessages().get(0).getBody());
        assertFalse(conversationRepository.findById(direct.getId()).isPresent());
        assertEquals(0, messageRepository.findByConversation_IdIn(java.util.Collections.singleton(direct.getId())).size());
        assertFalse(Files.list(fileStorage.directory().toPath()).findAny().isPresent());
        assertTrue(file.getId() > 0);
    }

    @Test
    void cannotDeleteAdmin() {
        UserAccount admin = userRepository.findByUsernameKey("admin").orElseThrow(AssertionError::new);
        ApiException ex = assertThrows(ApiException.class, () -> adminService.deleteUser(admin.getId()));
        assertEquals(AdminService.CANNOT_DELETE_ADMIN, ex.getMessage());
        assertTrue(userRepository.existsByUsernameKey("admin"));
    }

    @Test
    void clearRemovesMessagesAndFilesButKeepsAccountsAndHall() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        ConversationView direct = chatService.openDirect(user.getId(), userRepository.findByUsernameKey("admin").get().getId());
        chatService.sendText(user.getId(), chatService.hallId(), "大厅", null);
        chatService.saveUpload(user.getId(), direct.getId(), "a.txt",
                new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)), null);

        adminService.clear();

        assertEquals(0, messageRepository.count());
        assertTrue(userRepository.existsByUsernameKey("张三"));
        assertTrue(userRepository.existsByUsernameKey("admin"));
        assertTrue(conversationRepository.findFirstByType(ConversationType.HALL).isPresent());
        assertEquals(0, conversationRepository.findByType(ConversationType.DIRECT).size());
        assertFalse(Files.list(fileStorage.directory().toPath()).findAny().isPresent());
    }
}
