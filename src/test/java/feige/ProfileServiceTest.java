package feige;

import feige.domain.UserAccount;
import feige.service.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileServiceTest extends AbstractFeigeTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void replacingCustomAvatarDeletesTheOldFile() {
        UserAccount user = accountService.register("张三", "secret1");
        String first = stored(profileService.uploadCustomAvatar(user.getId(), png()));
        assertTrue(profileService.requireCustomAvatar(first).isFile());

        String second = stored(profileService.uploadCustomAvatar(user.getId(), png()));
        assertTrue(profileService.requireCustomAvatar(second).isFile());
        assertThrows(ApiException.class, () -> profileService.requireCustomAvatar(first));
    }

    @Test
    void nicknameIsTrimmedAndEmptyBecomesNull() {
        UserAccount user = accountService.register("张三", "secret1");
        assertEquals("小明", profileService.updateProfile(user.getId(), "  小明  ", null).getNickname());
        assertNull(profileService.updateProfile(user.getId(), "   ", null).getNickname());
        assertThrows(ApiException.class,
                () -> profileService.updateProfile(user.getId(), repeat('a', 31), null));
    }

    @Test
    void changingPasswordRequiresTheCurrentOne() {
        UserAccount user = accountService.register("张三", "secret1");
        assertThrows(ApiException.class,
                () -> profileService.changePassword(user.getId(), "wrong", "secret2"));
        profileService.changePassword(user.getId(), "secret1", "secret2");
        assertTrue(passwordEncoder.matches("secret2", userRepository.findById(user.getId()).get().getPasswordHash()));
    }

    private static MockMultipartFile png() {
        return new MockMultipartFile("file", "avatar.png", "image/png",
                "image-bytes".getBytes(StandardCharsets.UTF_8));
    }

    private static String stored(UserAccount account) {
        return account.getAvatar().substring("custom:".length());
    }

    private static String repeat(char value, int count) {
        StringBuilder text = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            text.append(value);
        }
        return text.toString();
    }
}
