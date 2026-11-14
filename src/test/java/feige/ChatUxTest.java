package feige;

import feige.domain.UserAccount;
import feige.service.ApiException;
import feige.service.ChatService;
import feige.service.ConversationView;
import feige.service.HistoryPage;
import feige.service.MessageView;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatUxTest extends AbstractFeigeTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void recallWithinTwoMinutesRemovesFileAndHidesBody() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        long hallId = chatService.hallId();
        MessageView file = chatService.saveUpload(user.getId(), hallId, "note.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), null);
        assertTrue(Files.list(fileStorage.directory().toPath()).findAny().isPresent());
        MessageView recalled = chatService.recall(user.getId(), file.getId());
        assertTrue(recalled.isRecalled());
        assertEquals("", recalled.getBody());
        assertFalse(Files.list(fileStorage.directory().toPath()).findAny().isPresent());
    }

    @Test
    void cannotRecallOthersOrAfterWindow() {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        long hallId = chatService.hallId();
        MessageView message = chatService.sendText(left.getId(), hallId, "你好", null);
        ApiException other = assertThrows(ApiException.class,
                () -> chatService.recall(right.getId(), message.getId()));
        assertEquals(ChatService.RECALL_NOT_OWNER, other.getMessage());
        feige.domain.ChatMessage entity = messageRepository.findById(message.getId()).orElseThrow(AssertionError::new);
        entity.setCreatedAt(Instant.now().minusSeconds(121));
        messageRepository.save(entity);
        ApiException late = assertThrows(ApiException.class,
                () -> chatService.recall(left.getId(), message.getId()));
        assertEquals(ChatService.RECALL_TOO_LATE, late.getMessage());
    }

    @Test
    void quoteMustBeSameConversationAndShowsRecalledSummary() {
        UserAccount user = accountService.register("张三", "secret1");
        UserAccount other = accountService.register("李四", "secret1");
        long hallId = chatService.hallId();
        ConversationView direct = chatService.openDirect(user.getId(), other.getId());
        MessageView hallMsg = chatService.sendText(user.getId(), hallId, "大厅话", null);
        ApiException cross = assertThrows(ApiException.class,
                () -> chatService.sendText(user.getId(), direct.getId(), "引用", hallMsg.getId()));
        assertEquals(ChatService.BAD_QUOTE, cross.getMessage());
        MessageView target = chatService.sendText(user.getId(), hallId, "原文", null);
        chatService.recall(user.getId(), target.getId());
        MessageView reply = chatService.sendText(user.getId(), hallId, "回复", target.getId());
        assertEquals("引用内容已撤回", reply.getQuoteBody());
    }

    @Test
    void historyPagesAndUnreadClearOnRead() {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        ConversationView direct = chatService.openDirect(left.getId(), right.getId());
        for (int i = 0; i < 55; i++) {
            chatService.sendText(left.getId(), direct.getId(), "m" + i, null);
        }
        HistoryPage latest = chatService.history(right.getId(), direct.getId(), null, 50);
        assertEquals(50, latest.getMessages().size());
        assertTrue(latest.isHasMore());
        assertEquals("m5", latest.getMessages().get(0).getBody());
        assertEquals("m54", latest.getMessages().get(49).getBody());
        HistoryPage older = chatService.history(right.getId(), direct.getId(),
                latest.getMessages().get(0).getId(), 50);
        assertEquals(5, older.getMessages().size());
        assertFalse(older.isHasMore());
        assertEquals(55, chatService.unreadCount(right.getId(), direct.getId()));
        assertEquals(0, chatService.unreadCount(left.getId(), direct.getId()));
        chatService.markRead(right.getId(), direct.getId());
        assertEquals(0, chatService.unreadCount(right.getId(), direct.getId()));
    }

    @Test
    void imageUploadRejectsNonImageAndServesInline() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        mockMvc.perform(multipart("/api/conversations/" + chatService.hallId() + "/images")
                        .file(new MockMultipartFile("file", "a.txt", "text/plain", "nope".getBytes(StandardCharsets.UTF_8)))
                        .with(user(new feige.domain.FeigeUser(user)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ChatService.NOT_IMAGE));
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        String body = mockMvc.perform(multipart("/api/conversations/" + chatService.hallId() + "/images")
                        .file(new MockMultipartFile("file", "pic.png", "image/png", png))
                        .with(user(new feige.domain.FeigeUser(user)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("IMAGE"))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll("(?s).*\"id\"\\s*:\\s*(\\d+).*", "$1"));
        mockMvc.perform(get("/api/files/" + id).with(user(new feige.domain.FeigeUser(user))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("inline")));
    }
}
