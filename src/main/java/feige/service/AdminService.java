package feige.service;

import feige.domain.ChatMessage;
import feige.domain.Conversation;
import feige.domain.ConversationMember;
import feige.domain.ConversationType;
import feige.domain.Role;
import feige.domain.UserAccount;
import feige.repo.ChatMessageRepository;
import feige.repo.ConversationMemberRepository;
import feige.repo.ConversationRepository;
import feige.repo.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminService {

    public static final String CANNOT_DELETE_ADMIN = "不能删除管理员";

    private final UserAccountRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final FileStorage files;
    private final ProfileService profileService;

    public AdminService(UserAccountRepository users,
                        ConversationRepository conversations,
                        ConversationMemberRepository members,
                        ChatMessageRepository messages,
                        FileStorage files,
                        ProfileService profileService) {
        this.users = users;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.files = files;
        this.profileService = profileService;
    }

    @Transactional
    public DeleteOutcome deleteUser(long userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (user.getRole() == Role.ADMIN || "admin".equalsIgnoreCase(user.getUsername())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, CANNOT_DELETE_ADMIN);
        }
        List<Conversation> directs = conversations.findDirectByUser(userId);
        List<Long> directIds = new ArrayList<Long>();
        for (Conversation conversation : directs) {
            directIds.add(conversation.getId());
        }
        List<ChatMessage> doomed = new ArrayList<ChatMessage>();
        if (!directIds.isEmpty()) {
            doomed.addAll(messages.findByConversation_IdIn(directIds));
        }
        doomed.addAll(messages.findHallMessagesBySender(userId));
        List<String> storedNames = new ArrayList<String>();
        for (ChatMessage message : doomed) {
            if (message.getStoredName() != null) {
                storedNames.add(message.getStoredName());
            }
        }
        if (!doomed.isEmpty()) {
            messages.deleteAll(doomed);
        }
        if (!directIds.isEmpty()) {
            List<ConversationMember> rows = members.findByConversation_IdIn(directIds);
            members.deleteAll(rows);
            conversations.deleteAll(directs);
        }
        // 大厅成员关系也指向该用户，不删除会撞外键，导致删除用户整体失败
        members.deleteAll(members.findByUser_Id(userId));
        users.delete(user);
        profileService.deleteCustomAvatar(user);
        for (String storedName : storedNames) {
            files.deleteQuietly(storedName);
        }
        return new DeleteOutcome(user.getId(), user.getUsername(), directIds);
    }

    @Transactional
    public void clear() {
        messages.deleteAllInBatch();
        members.deleteAllInBatch();
        conversations.deleteAllInBatch(conversations.findByType(ConversationType.DIRECT));
        files.deleteAll();
    }

    public static final class DeleteOutcome {
        private final long userId;
        private final String username;
        private final List<Long> conversationIds;

        public DeleteOutcome(long userId, String username, List<Long> conversationIds) {
            this.userId = userId;
            this.username = username;
            this.conversationIds = conversationIds;
        }

        public long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public List<Long> getConversationIds() {
            return conversationIds;
        }
    }
}
