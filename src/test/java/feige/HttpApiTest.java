package feige;

import feige.domain.FeigeUser;
import feige.domain.UserAccount;
import feige.service.ConversationView;
import feige.service.ThemeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpApiTest extends AbstractFeigeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownUserAndWrongPasswordLookTheSame() throws Exception {
        accountService.register("张三", "secret1");
        mockMvc.perform(post("/login").param("username", "nobody").param("password", "secret1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(post("/login").param("username", "张三").param("password", "wrong-password").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
        String page = StreamUtils.copyToString(new org.springframework.core.io.ClassPathResource("static/login.html").getInputStream(),
                StandardCharsets.UTF_8);
        assertTrue(page.contains("用户名或密码错误"));
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("/login.html"));
    }

    @Test
    void registerThenLogin() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType("application/json")
                        .content("{\"username\":\"李四\",\"password\":\"secret1\"}")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("李四"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(post("/login").param("username", "李四").param("password", "secret1").with(csrf()))
                .andExpect(redirectedUrl("/chat"));
    }

    @Test
    void anonymousApiIsUnauthorizedAndNormalUserCannotCallAdmin() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/chat")).andExpect(status().is3xxRedirection());
        UserAccount user = accountService.register("王五", "secret1");
        mockMvc.perform(delete("/api/admin/users/" + user.getId()).with(user(new FeigeUser(user))).with(csrf()))
                .andExpect(status().isForbidden());
        UserAccount admin = userRepository.findByUsernameKey("admin").orElseThrow(AssertionError::new);
        mockMvc.perform(delete("/api/admin/users/" + admin.getId()).with(user(new FeigeUser(admin))).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能删除管理员"));
    }

    @Test
    void meReturnsProfileChangesMadeAfterLogin() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        FeigeUser stale = new FeigeUser(user);
        profileService.updateProfile(user.getId(), "新昵称", null);
        profileService.updateProfile(user.getId(), null, "builtin:avatar3.png");
        mockMvc.perform(get("/api/me").with(user(stale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("新昵称"))
                .andExpect(jsonPath("$.avatar").value("/avatars/avatar3.png"));
    }

    @Test
    void customAvatarIsServedWithItsRealImageType() throws Exception {
        UserAccount user = accountService.register("张三", "secret1");
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        UserAccount updated = profileService.uploadCustomAvatar(user.getId(),
                new MockMultipartFile("file", "a.png", "image/png", png));
        String stored = updated.getAvatar().substring("custom:".length());
        mockMvc.perform(get("/api/avatars/" + stored).with(user(new FeigeUser(user))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png));
    }

    @Test
    void adminSwitchesTheThemeSeenByEveryone() throws Exception {
        mockMvc.perform(get("/api/theme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value(ThemeService.CLASSIC));
        UserAccount user = accountService.register("王五", "secret1");
        mockMvc.perform(post("/api/admin/theme")
                        .contentType("application/json")
                        .content("{\"theme\":\"signal\"}")
                        .with(user(new FeigeUser(user)))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        UserAccount admin = userRepository.findByUsernameKey("admin").orElseThrow(AssertionError::new);
        mockMvc.perform(post("/api/admin/theme")
                        .contentType("application/json")
                        .content("{\"theme\":\"signal\"}")
                        .with(user(new FeigeUser(admin)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value(ThemeService.SIGNAL));
        mockMvc.perform(get("/api/theme"))
                .andExpect(jsonPath("$.theme").value(ThemeService.SIGNAL));
        mockMvc.perform(post("/api/admin/theme")
                        .contentType("application/json")
                        .content("{\"theme\":\"neon\"}")
                        .with(user(new FeigeUser(admin)))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ThemeService.UNKNOWN));
    }

    @Test
    void directFileIsHiddenFromOutsidersAndHallFileIsVisible() throws Exception {
        UserAccount left = accountService.register("张三", "secret1");
        UserAccount right = accountService.register("李四", "secret1");
        UserAccount outsider = accountService.register("王五", "secret1");
        ConversationView direct = chatService.openDirect(left.getId(), right.getId());
        String directBody = mockMvc.perform(multipart("/api/conversations/" + direct.getId() + "/files")
                        .file(new MockMultipartFile("file", "secret.txt", "text/plain", "secret".getBytes(StandardCharsets.UTF_8)))
                        .with(user(new FeigeUser(left)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long directMessageId = Long.parseLong(directBody.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mockMvc.perform(get("/api/files/" + directMessageId).with(user(new FeigeUser(outsider))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/files/" + directMessageId).with(user(new FeigeUser(right))))
                .andExpect(status().isOk())
                .andExpect(content().bytes("secret".getBytes(StandardCharsets.UTF_8)));

        String hallBody = mockMvc.perform(multipart("/api/conversations/" + chatService.hallId() + "/files")
                        .file(new MockMultipartFile("file", "public.txt", "text/plain", "public".getBytes(StandardCharsets.UTF_8)))
                        .with(user(new FeigeUser(left)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long hallMessageId = Long.parseLong(hallBody.replaceAll(".*\"id\":(\\d+).*", "$1"));
        mockMvc.perform(get("/api/files/" + hallMessageId).with(user(new FeigeUser(outsider))))
                .andExpect(status().isOk())
                .andExpect(content().bytes("public".getBytes(StandardCharsets.UTF_8)));
    }
}
