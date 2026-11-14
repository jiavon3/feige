package feige.web;

import feige.domain.FeigeUser;
import feige.domain.UserAccount;
import feige.repo.UserAccountRepository;
import feige.service.ApiException;
import feige.service.ChatService;
import feige.service.ConversationView;
import feige.service.HistoryPage;
import feige.service.MessageFanout;
import feige.service.MessageView;
import feige.service.PresenceService;
import feige.service.ProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatService chats;
    private final UserAccountRepository users;
    private final PresenceService presence;
    private final MessageFanout fanout;
    private final ProfileService profileService;

    public ChatController(ChatService chats, UserAccountRepository users, PresenceService presence,
                          MessageFanout fanout, ProfileService profileService) {
        this.chats = chats;
        this.users = users;
        this.presence = presence;
        this.fanout = fanout;
        this.profileService = profileService;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal FeigeUser user) {
        // 会话里的 FeigeUser 是登录时的快照，昵称和头像要取数据库里的最新值
        UserAccount account = users.findById(user.getId()).orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        long hallId = chats.hallId();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", account.getId());
        body.put("username", account.getUsername());
        body.put("nickname", account.getNickname());
        body.put("avatar", profileService.resolveAvatarUrl(account.getAvatar()));
        body.put("role", account.getRole().name());
        body.put("hallConversationId", hallId);
        body.put("hallUnreadCount", chats.unreadCount(account.getId(), hallId));
        return body;
    }

    @GetMapping("/api/users")
    public List<UserView> users(@AuthenticationPrincipal FeigeUser user) {
        List<UserView> views = new ArrayList<UserView>();
        for (UserAccount account : users.findByIdNot(user.getId())) {
            Long conversationId = chats.directConversationId(user.getId(), account.getId());
            int unread = conversationId == null ? 0 : chats.unreadCount(user.getId(), conversationId);
            views.add(new UserView(account.getId(), account.getUsername(), account.getNickname(),
                    profileService.resolveAvatarUrl(account.getAvatar()), presence.isOnline(account.getId()),
                    conversationId, unread));
        }
        return views;
    }

    @PostMapping("/api/conversations/direct")
    public ConversationView direct(@AuthenticationPrincipal FeigeUser user, @RequestBody DirectRequest request) {
        return chats.openDirect(user.getId(), request.userId);
    }

    @GetMapping("/api/conversations/{id}/messages")
    public HistoryPage messages(@AuthenticationPrincipal FeigeUser user,
                                @PathVariable("id") long id,
                                @RequestParam(value = "beforeId", required = false) Long beforeId,
                                @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return chats.history(user.getId(), id, beforeId, limit);
    }

    @PostMapping("/api/conversations/{id}/read")
    public Map<String, Object> read(@AuthenticationPrincipal FeigeUser user, @PathVariable("id") long id) {
        long lastRead = chats.markRead(user.getId(), id);
        fanout.deliverRead(user.getId(), id, lastRead);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("conversationId", id);
        body.put("lastReadMessageId", lastRead);
        return body;
    }

    @DeleteMapping("/api/conversations/{id}/messages")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal FeigeUser user, @PathVariable("id") long id) {
        chats.clearConversation(user.getId(), id);
        fanout.deliverConversationCleared(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/messages/{id}/recall")
    public MessageView recall(@AuthenticationPrincipal FeigeUser user, @PathVariable("id") long id) {
        MessageView view = chats.recall(user.getId(), id);
        fanout.deliverRecall(view);
        return view;
    }

    public static class DirectRequest {
        public long userId;
    }

    public static class UserView {
        private final long id;
        private final String username;
        private final String nickname;
        private final String avatar;
        private final boolean online;
        private final Long conversationId;
        private final int unreadCount;

        public UserView(long id, String username, String nickname, String avatar, boolean online,
                        Long conversationId, int unreadCount) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.avatar = avatar;
            this.online = online;
            this.conversationId = conversationId;
            this.unreadCount = unreadCount;
        }

        public long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getNickname() {
            return nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public boolean isOnline() {
            return online;
        }

        public Long getConversationId() {
            return conversationId;
        }

        public int getUnreadCount() {
            return unreadCount;
        }
    }
}
