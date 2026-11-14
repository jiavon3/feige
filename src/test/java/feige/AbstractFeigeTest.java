package feige;

import feige.bootstrap.DataSeeder;
import feige.repo.AppSettingRepository;
import feige.repo.ChatMessageRepository;
import feige.repo.ConversationMemberRepository;
import feige.repo.ConversationRepository;
import feige.repo.UserAccountRepository;
import feige.service.AccountService;
import feige.service.AdminService;
import feige.service.ChatService;
import feige.service.FileStorage;
import feige.service.ProfileService;
import feige.web.ChatWebSocketHandler;
import feige.web.SocketHub;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "feige.data-dir=${java.io.tmpdir}/feige-test-data",
                "spring.datasource.url=jdbc:h2:mem:feigetest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.feige=WARN"
})
public abstract class AbstractFeigeTest {

    @Autowired
    protected UserAccountRepository userRepository;
    @Autowired
    protected AppSettingRepository appSettingRepository;
    @Autowired
    protected ConversationRepository conversationRepository;
    @Autowired
    protected ConversationMemberRepository memberRepository;
    @Autowired
    protected ChatMessageRepository messageRepository;
    @Autowired
    protected AccountService accountService;
    @Autowired
    protected ChatService chatService;
    @Autowired
    protected AdminService adminService;
    @Autowired
    protected FileStorage fileStorage;
    @Autowired
    protected ProfileService profileService;
    @Autowired
    protected DataSeeder dataSeeder;
    @Autowired
    protected ChatWebSocketHandler chatWebSocketHandler;
    @Autowired
    protected SocketHub socketHub;

    @BeforeEach
    public void resetData() {
        appSettingRepository.deleteAll();
        messageRepository.deleteAll();
        memberRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
        fileStorage.deleteAll();
        dataSeeder.seed();
    }
}
