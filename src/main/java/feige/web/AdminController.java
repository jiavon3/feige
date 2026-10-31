package feige.web;

import feige.domain.Role;
import feige.domain.UserAccount;
import feige.repo.UserAccountRepository;
import feige.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AdminController {

    private final AdminService admin;
    private final UserAccountRepository users;
    private final SessionTracker sessions;
    private final SocketHub hub;

    public AdminController(AdminService admin, UserAccountRepository users, SessionTracker sessions, SocketHub hub) {
        this.admin = admin;
        this.users = users;
        this.sessions = sessions;
        this.hub = hub;
    }

    @GetMapping("/api/admin/users")
    public List<Map<String, Object>> users() {
        List<UserAccount> accounts = users.findByRole(Role.USER);
        List<Map<String, Object>> body = new ArrayList<Map<String, Object>>();
        for (UserAccount account : accounts) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", account.getId());
            row.put("username", account.getUsername());
            row.put("createdAt", account.getCreatedAt());
            body.add(row);
        }
        return body;
    }

    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        AdminService.DeleteOutcome outcome = admin.deleteUser(id);
        sessions.expire(outcome.getUsername());
        hub.closeUser(outcome.getUserId());
        Map<String, Object> deleted = new LinkedHashMap<String, Object>();
        deleted.put("type", "user.deleted");
        deleted.put("userId", outcome.getUserId());
        hub.sendToAll(deleted);
        if (!outcome.getConversationIds().isEmpty()) {
            Map<String, Object> conversations = new LinkedHashMap<String, Object>();
            conversations.put("type", "conversations.deleted");
            conversations.put("conversationIds", outcome.getConversationIds());
            hub.sendToAll(conversations);
        }
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/api/admin/clear")
    public ResponseEntity<Void> clear() {
        admin.clear();
        hub.sendToAll(Collections.singletonMap("type", "cleared"));
        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }
}
