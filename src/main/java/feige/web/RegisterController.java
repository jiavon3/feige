package feige.web;

import feige.service.AccountService;
import feige.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RegisterController {

    private final AccountService accounts;
    private final ChatService chats;

    public RegisterController(AccountService accounts, ChatService chats) {
        this.accounts = accounts;
        this.chats = chats;
    }

    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        feige.domain.UserAccount user = accounts.register(request.username, request.password);
        chats.ensureHallMember(user.getId());
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        return new ResponseEntity<Map<String, Object>>(body, HttpStatus.CREATED);
    }

    public static class RegisterRequest {
        public String username;
        public String password;
    }
}
