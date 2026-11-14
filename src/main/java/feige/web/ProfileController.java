package feige.web;

import feige.domain.FeigeUser;
import feige.domain.UserAccount;
import feige.service.ProfileFanout;
import feige.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileFanout profileFanout;

    public ProfileController(ProfileService profileService, ProfileFanout profileFanout) {
        this.profileService = profileService;
        this.profileFanout = profileFanout;
    }

    @GetMapping("/api/avatars/builtin")
    public Map<String, Object> builtinAvatars() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("avatars", profileService.listBuiltins());
        return body;
    }

    @PostMapping("/api/me/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@AuthenticationPrincipal FeigeUser user,
                                                             @RequestBody ProfileRequest request) {
        UserAccount account = profileService.updateProfile(user.getId(), request.nickname, request.avatar);
        profileFanout.deliver(account);
        return ResponseEntity.ok(toProfileBody(account));
    }

    @PostMapping("/api/me/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(@AuthenticationPrincipal FeigeUser user,
                                                            @RequestParam("file") MultipartFile file) {
        UserAccount account = profileService.uploadCustomAvatar(user.getId(), file);
        profileFanout.deliver(account);
        return ResponseEntity.ok(toProfileBody(account));
    }

    @PostMapping("/api/me/password")
    public ResponseEntity<Map<String, Object>> changePassword(@AuthenticationPrincipal FeigeUser user,
                                                              @RequestBody PasswordRequest request) {
        profileService.changePassword(user.getId(), request.currentPassword, request.newPassword);
        return ResponseEntity.ok(Collections.<String, Object>singletonMap("ok", true));
    }

    private Map<String, Object> toProfileBody(UserAccount account) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", account.getId());
        body.put("username", account.getUsername());
        body.put("nickname", account.getNickname());
        body.put("avatar", profileService.resolveAvatarUrl(account.getAvatar()));
        return body;
    }

    public static class ProfileRequest {
        public String nickname;
        public String avatar;
    }

    public static class PasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}
