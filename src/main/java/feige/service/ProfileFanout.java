package feige.service;

import feige.domain.UserAccount;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProfileFanout {

    private final ClientPush push;
    private final ProfileService profileService;

    public ProfileFanout(ClientPush push, ProfileService profileService) {
        this.push = push;
        this.profileService = profileService;
    }

    public void deliver(UserAccount account) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "profile");
        payload.put("userId", account.getId());
        payload.put("username", account.getUsername());
        payload.put("nickname", account.getNickname());
        payload.put("avatar", profileService.resolveAvatarUrl(account.getAvatar()));
        push.sendToAll(payload);
    }
}
