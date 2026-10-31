package feige.web;

import feige.domain.FeigeUser;
import feige.domain.Role;
import feige.domain.UserAccount;
import feige.repo.UserAccountRepository;
import feige.service.ChatService;
import feige.service.ConversationView;
import feige.service.HistoryPage;
import feige.service.MessageFanout;
import feige.service.MessageView;
import feige.service.PresenceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private final ChatService chats;
    private final UserAccountRepository users;
    private final PresenceService presence;
    private final MessageFanout fanout;

    public ChatController(ChatService chats, UserAccountRepository users, PresenceService presence,
                          MessageFanout fanout) {
        this.chats = chats;
        this.users = users;
        this.presence = presence;
        this.fanout = fanout;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(@AuthenticationPrincipal FeigeUser user) {
        long hallId = chats.hallId();
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("role", user.getRole().name());
        body.put("hallConversationId", hallId);
        body.put("hallUnreadCount", chats.unreadCount(user.getId(), hallId));
        return body;
    }

    @GetMapping("/api/users")
    public List<UserView> users(@AuthenticationPrincipal FeigeUser user) {
        List<UserView> views = new ArrayList<UserView>();
        for (UserAccount account : users.findByIdNot(user.getId())) {
            Long conversationId = chats.directConversationId(user.getId(), account.getId());
            int unread = conversationId == null ? 0 : chats.unreadCount(user.getId(), conversationId);
            views.add(new UserView(account.getId(), account.getUsername(), presence.isOnline(account.getId()),
                    account.getRole() == Role.ADMIN, conversationId, unread));
        }
        Collections.sort(views, new Comparator<UserView>() {
            @Override
            public int compare(UserView left, UserView right) {
                if (left.isOnline() != right.isOnline()) {
                    return left.isOnline() ? -1 : 1;
                }
                return left.getUsername().compareToIgnoreCase(right.getUsername());
            }
        });
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
        private final boolean online;
        private final boolean admin;
        private final Long conversationId;
        private final int unreadCount;

        public UserView(long id, String username, boolean online, boolean admin,
                        Long conversationId, int unreadCount) {
            this.id = id;
            this.username = username;
            this.online = online;
            this.admin = admin;
            this.conversationId = conversationId;
            this.unreadCount = unreadCount;
        }

        public long getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public boolean isOnline() {
            return online;
        }

        public boolean isAdmin() {
            return admin;
        }

        public Long getConversationId() {
            return conversationId;
        }

        public int getUnreadCount() {
            return unreadCount;
        }
    }
}
