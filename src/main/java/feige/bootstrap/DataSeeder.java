package feige.bootstrap;

import feige.service.AccountService;
import feige.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AccountService accounts;
    private final ChatService chats;

    public DataSeeder(AccountService accounts, ChatService chats) {
        this.accounts = accounts;
        this.chats = chats;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed();
    }

    public void seed() {
        if (accounts.createAdminIfMissing()) {
            log.info("已创建默认管理员 admin / admin123");
        }
        chats.ensureHall();
    }
}

