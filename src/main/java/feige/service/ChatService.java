package feige.service;

import feige.domain.ChatMessage;
import feige.domain.Conversation;
import feige.domain.ConversationMember;
import feige.domain.ConversationType;
import feige.domain.MessageKind;
import feige.domain.UserAccount;
import feige.repo.ChatMessageRepository;
import feige.repo.ConversationMemberRepository;
import feige.repo.ConversationRepository;
import feige.repo.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    public static final String EMPTY_TEXT = "不能发送空消息";
    public static final String TEXT_TOO_LONG = "消息不能超过 2000 个字符";
    public static final String FORBIDDEN = "无权访问该会话";
    public static final String RECALL_TOO_LATE = "超过两分钟，无法撤回";
    public static final String RECALL_NOT_OWNER = "只能撤回自己的消息";
    public static final String BAD_QUOTE = "无法引用该消息";
    public static final String NOT_IMAGE = "请选择图片文件";
    public static final int MAX_TEXT = 2000;
    public static final int DEFAULT_PAGE = 50;
    public static final Duration RECALL_WINDOW = Duration.ofMinutes(2);

    private static final Set<String> IMAGE_TYPES = imageTypes();
    private static final Set<String> IMAGE_EXT = imageExt();

    private final UserAccountRepository users;
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ChatMessageRepository messages;
    private final FileStorage files;
    private final TransactionTemplate transactions;
    private final ConcurrentHashMap<String, Object> directLocks = new ConcurrentHashMap<String, Object>();

    public ChatService(UserAccountRepository users,
                       ConversationRepository conversations,
                       ConversationMemberRepository members,
                       ChatMessageRepository messages,
                       FileStorage files,
                       PlatformTransactionManager transactionManager) {
        this.users = users;
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.files = files;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public long hallId() {
        return requireHall().getId();
    }

    @Transactional
    public Conversation ensureHall() {
        Conversation hall = conversations.findFirstByType(ConversationType.HALL).orElseGet(() -> {
            Conversation created = new Conversation();
            created.setType(ConversationType.HALL);
            created.setCreatedAt(Instant.now());
            return conversations.save(created);
        });
        ensureAllUsersInHall(hall);
        return hall;
    }

    @Transactional
    public void ensureHallMember(long userId) {
        Conversation hall = requireHall();
        UserAccount user = requireUser(userId);
        if (!members.existsByConversation_IdAndUser_Id(hall.getId(), userId)) {
            members.save(member(hall, user));
        }
    }

    public ConversationView openDirect(long selfId, long otherId) {
        if (selfId == otherId) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能和自己私聊");
        }
        requireUser(selfId);
        users.findById(otherId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        long low = Math.min(selfId, otherId);
        long high = Math.max(selfId, otherId);
        Object lock = directLocks.computeIfAbsent(low + ":" + high, key -> new Object());
        synchronized (lock) {
            return transactions.execute(status -> createOrGet(selfId, otherId, low, high));
        }
    }

    @Transactional
    public MessageView sendText(long senderId, long conversationId, String text, Long quoteMessageId) {
        UserAccount sender = requireUser(senderId);
        Conversation conversation = requireConversation(conversationId);
        assertAccess(senderId, conversation);
        String body = text == null ? "" : text.trim();
        if (body.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, EMPTY_TEXT);
        }
        if (body.length() > MAX_TEXT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, TEXT_TOO_LONG);
        }
        Long quoteId = resolveQuote(conversationId, quoteMessageId);
        return toView(saveMessage(conversation, sender, MessageKind.TEXT, body, null, null, quoteId));
    }

    @Transactional
    public MessageView saveUpload(long senderId, long conversationId, String originalName,
                                  InputStream input, Long quoteMessageId) {
        return saveBinary(senderId, conversationId, originalName, input, quoteMessageId, MessageKind.FILE);
    }

    @Transactional
    public MessageView saveImage(long senderId, long conversationId, String originalName,
                                 String contentType, InputStream input, Long quoteMessageId) {
        if (!isImage(originalName, contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, NOT_IMAGE);
        }
        return saveBinary(senderId, conversationId, originalName, input, quoteMessageId, MessageKind.IMAGE);
    }

    @Transactional
    public MessageView recall(long userId, long messageId) {
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "消息不存在"));
        assertAccess(userId, message.getConversation());
        if (message.getSender().getId() != userId) {
            throw new ApiException(HttpStatus.FORBIDDEN, RECALL_NOT_OWNER);
        }
        if (message.isRecalled()) {
            return toView(message);
        }
        if (Duration.between(message.getCreatedAt(), Instant.now()).compareTo(RECALL_WINDOW) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, RECALL_TOO_LATE);
        }
        String stored = message.getStoredName();
        message.setRecalled(true);
        message.setBody("");
        message.setStoredName(null);
        message.setSizeBytes(null);
        messages.save(message);
        if (stored != null) {
            files.deleteQuietly(stored);
        }
        return toView(message);
    }

    @Transactional
    public long markRead(long userId, long conversationId) {
        Conversation conversation = requireConversation(conversationId);
        assertAccess(userId, conversation);
        ConversationMember membership = requireMembership(userId, conversation);
        long latest = messages.findFirstByConversation_IdOrderByIdDesc(conversationId)
                .map(ChatMessage::getId)
                .orElse(0L);
        if (latest > 0) {
            membership.setLastReadMessageId(latest);
            members.save(membership);
        }
        return latest;
    }

    @Transactional(readOnly = true)
    public HistoryPage history(long userId, long conversationId, Long beforeId, int limit) {
        Conversation conversation = requireConversation(conversationId);
        assertAccess(userId, conversation);
        int pageSize = limit <= 0 ? DEFAULT_PAGE : Math.min(limit, 200);
        List<ChatMessage> page;
        if (beforeId == null) {
            page = messages.findLatest(conversationId, PageRequest.of(0, pageSize + 1));
        } else {
            page = messages.findBefore(conversationId, beforeId, PageRequest.of(0, pageSize + 1));
        }
        boolean hasMore = page.size() > pageSize;
        if (hasMore) {
            page = new ArrayList<ChatMessage>(page.subList(0, pageSize));
        }
        Collections.reverse(page);
        List<MessageView> views = new ArrayList<MessageView>();
        for (ChatMessage message : page) {
            views.add(toView(message));
        }
        return new HistoryPage(views, hasMore);
    }

    @Transactional(readOnly = true)
    public int unreadCount(long userId, long conversationId) {
        ConversationMember membership = members.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElse(null);
        Long after = membership == null ? null : membership.getLastReadMessageId();
        long count = after == null
                ? messages.countUnreadAll(conversationId, userId)
                : messages.countUnreadAfter(conversationId, userId, after);
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    @Transactional(readOnly = true)
    public Long directConversationId(long selfId, long otherId) {
        long low = Math.min(selfId, otherId);
        long high = Math.max(selfId, otherId);
        List<Conversation> found = conversations.findDirectBetween(low, high);
        return found.isEmpty() ? null : found.get(0).getId();
    }

    @Transactional(readOnly = true)
    public Download requireDownload(long userId, long messageId) {
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING));
        if (message.isRecalled()) {
            throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
        }
        if (message.getKind() != MessageKind.FILE && message.getKind() != MessageKind.IMAGE) {
            throw new ApiException(HttpStatus.NOT_FOUND, FileStorage.MISSING);
        }
        assertAccess(userId, message.getConversation());
        return new Download(files.require(message.getStoredName()), message.getBody(),
                message.getKind() == MessageKind.IMAGE);
    }

    private MessageView saveBinary(long senderId, long conversationId, String originalName,
                                   InputStream input, Long quoteMessageId, MessageKind kind) {
        UserAccount sender = requireUser(senderId);
        Conversation conversation = requireConversation(conversationId);
        assertAccess(senderId, conversation);
        Long quoteId = resolveQuote(conversationId, quoteMessageId);
        String stored = files.save(input);
        try {
            ChatMessage saved = saveMessage(conversation, sender, kind,
                    displayName(originalName), stored, files.require(stored).length(), quoteId);
            return toView(saved);
        } catch (RuntimeException ex) {
            files.deleteQuietly(stored);
            throw ex;
        }
    }

    private void ensureAllUsersInHall(Conversation hall) {
        for (UserAccount user : users.findAll()) {
            if (!members.existsByConversation_IdAndUser_Id(hall.getId(), user.getId())) {
                members.save(member(hall, user));
            }
        }
    }

    private ConversationView createOrGet(long selfId, long otherId, long low, long high) {
        List<Conversation> existing = conversations.findDirectBetween(low, high);
        if (!existing.isEmpty()) {
            Conversation found = existing.get(0);
            return new ConversationView(found.getId(), found.getType().name());
        }
        UserAccount self = requireUser(selfId);
        UserAccount other = requireUser(otherId);
        Conversation created = new Conversation();
        created.setType(ConversationType.DIRECT);
        created.setCreatedAt(Instant.now());
        created = conversations.saveAndFlush(created);
        UserAccount lowUser = self.getId() == low ? self : other;
        UserAccount highUser = self.getId() == high ? self : other;
        members.save(member(created, lowUser));
        members.save(member(created, highUser));
        members.flush();
        return new ConversationView(created.getId(), created.getType().name());
    }

    private ConversationMember member(Conversation conversation, UserAccount user) {
        ConversationMember membership = new ConversationMember();
        membership.setConversation(conversation);
        membership.setUser(user);
        return membership;
    }

    private ConversationMember requireMembership(long userId, Conversation conversation) {
        return members.findByConversation_IdAndUser_Id(conversation.getId(), userId)
                .orElseGet(() -> {
                    if (conversation.getType() != ConversationType.HALL) {
                        throw new ApiException(HttpStatus.FORBIDDEN, FORBIDDEN);
                    }
                    return members.save(member(conversation, requireUser(userId)));
                });
    }

    private Long resolveQuote(long conversationId, Long quoteMessageId) {
        if (quoteMessageId == null) {
            return null;
        }
        ChatMessage quoted = messages.findById(quoteMessageId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, BAD_QUOTE));
        if (quoted.getConversation().getId() != conversationId) {
            throw new ApiException(HttpStatus.BAD_REQUEST, BAD_QUOTE);
        }
        return quoted.getId();
    }

    private ChatMessage saveMessage(Conversation conversation, UserAccount sender, MessageKind kind,
                                    String body, String storedName, Long sizeBytes, Long quoteMessageId) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setKind(kind);
        message.setBody(body);
        message.setStoredName(storedName);
        message.setSizeBytes(sizeBytes);
        message.setRecalled(false);
        message.setQuoteMessageId(quoteMessageId);
        message.setCreatedAt(Instant.now());
        return messages.save(message);
    }

    private void assertAccess(long userId, Conversation conversation) {
        if (conversation.getType() == ConversationType.HALL) {
            return;
        }
        if (!members.existsByConversation_IdAndUser_Id(conversation.getId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, FORBIDDEN);
        }
    }

    private UserAccount requireUser(long userId) {
        return users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private Conversation requireConversation(long conversationId) {
        return conversations.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));
    }

    private Conversation requireHall() {
        return conversations.findFirstByType(ConversationType.HALL)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "大厅不存在"));
    }

    private MessageView toView(ChatMessage message) {
        String body = message.isRecalled() ? "" : message.getBody();
        Long size = message.isRecalled() ? null : message.getSizeBytes();
        String quoteSender = null;
        String quoteKind = null;
        String quoteBody = null;
        if (message.getQuoteMessageId() != null) {
            ChatMessage quoted = messages.findById(message.getQuoteMessageId()).orElse(null);
            if (quoted == null || quoted.isRecalled()) {
                quoteBody = "引用内容已撤回";
            } else {
                quoteSender = quoted.getSender().getUsername();
                quoteKind = quoted.getKind().name();
                quoteBody = quoteSummary(quoted);
            }
        }
        return new MessageView(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getKind().name(),
                body,
                size,
                message.isRecalled(),
                message.getQuoteMessageId(),
                quoteSender,
                quoteKind,
                quoteBody,
                message.getCreatedAt());
    }

    private static String quoteSummary(ChatMessage quoted) {
        if (quoted.getKind() == MessageKind.IMAGE) {
            return "[图片]";
        }
        if (quoted.getKind() == MessageKind.FILE) {
            return "[文件] " + quoted.getBody();
        }
        String text = quoted.getBody() == null ? "" : quoted.getBody();
        return text.length() > 80 ? text.substring(0, 80) + "…" : text;
    }

    static String displayName(String original) {
        if (original == null) {
            return "未命名文件";
        }
        String name = original.replace('\\', '/').trim();
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1).trim();
        }
        if (name.isEmpty()) {
            return "未命名文件";
        }
        if (name.length() > 200) {
            return name.substring(0, 200);
        }
        return name;
    }

    static boolean isImage(String originalName, String contentType) {
        if (contentType != null) {
            String type = contentType.toLowerCase(Locale.ROOT).trim();
            int semi = type.indexOf(';');
            if (semi >= 0) {
                type = type.substring(0, semi).trim();
            }
            if (IMAGE_TYPES.contains(type)) {
                return true;
            }
        }
        if (originalName == null) {
            return false;
        }
        String name = originalName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return IMAGE_EXT.contains(name.substring(dot));
    }

    private static Set<String> imageTypes() {
        Set<String> types = new HashSet<String>();
        types.add("image/jpeg");
        types.add("image/png");
        types.add("image/gif");
        types.add("image/webp");
        return types;
    }

    private static Set<String> imageExt() {
        Set<String> ext = new HashSet<String>();
        ext.add(".jpg");
        ext.add(".jpeg");
        ext.add(".png");
        ext.add(".gif");
        ext.add(".webp");
        return ext;
    }
}
