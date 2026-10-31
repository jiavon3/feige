package feige.service;

import feige.domain.Role;
import feige.domain.UserAccount;
import feige.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
public class AccountService {

    public static final String USERNAME_RULE = "用户名需为 2 到 20 位字母、数字或汉字";
    public static final String PASSWORD_RULE = "密码需为 6 到 64 位";
    public static final String NAME_TAKEN = "用户名已存在";
    static final String USERNAME_PATTERN = "^[\\p{L}\\p{N}]{2,20}$";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String username, String password) {
        validate(username, password);
        if ("admin".equalsIgnoreCase(username) || users.existsByUsernameKey(key(username))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, NAME_TAKEN);
        }
        return create(username, password, Role.USER);
    }

    public boolean createAdminIfMissing() {
        if (users.existsByUsernameKey("admin")) {
            return false;
        }
        create("admin", "admin123", Role.ADMIN);
        return true;
    }

    private void validate(String username, String password) {
        if (username == null || !username.matches(USERNAME_PATTERN)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, USERNAME_RULE);
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_RULE);
        }
    }

    private UserAccount create(String username, String password, Role role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setUsernameKey(key(username));
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(role);
        account.setCreatedAt(Instant.now());
        return users.save(account);
    }

    private static String key(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
