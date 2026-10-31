package feige;

import feige.domain.UserAccount;
import feige.service.AccountService;
import feige.service.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest extends AbstractFeigeTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersChineseNameAndStoresPasswordHash() {
        UserAccount user = accountService.register("张三", "secret1");
        assertEquals("张三", user.getUsername());
        assertFalse(user.getPasswordHash().equals("secret1"));
        assertTrue(passwordEncoder.matches("secret1", user.getPasswordHash()));
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        accountService.register("Alice", "secret1");
        ApiException ex = assertThrows(ApiException.class, () -> accountService.register("alice", "secret2"));
        assertEquals(AccountService.NAME_TAKEN, ex.getMessage());
    }

    @Test
    void rejectsReservedAdminName() {
        ApiException lower = assertThrows(ApiException.class, () -> accountService.register("admin", "secret1"));
        ApiException upper = assertThrows(ApiException.class, () -> accountService.register("Admin", "secret1"));
        assertEquals(AccountService.NAME_TAKEN, lower.getMessage());
        assertEquals(AccountService.NAME_TAKEN, upper.getMessage());
    }

    @Test
    void rejectsShortPasswordAndSymbolInName() {
        ApiException password = assertThrows(ApiException.class, () -> accountService.register("李四", "12345"));
        ApiException name = assertThrows(ApiException.class, () -> accountService.register("ab_c", "secret1"));
        assertEquals(AccountService.PASSWORD_RULE, password.getMessage());
        assertEquals(AccountService.USERNAME_RULE, name.getMessage());
    }
}
