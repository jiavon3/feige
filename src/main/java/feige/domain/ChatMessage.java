package feige.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageKind kind;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(length = 36)
    private String storedName;

    private Long sizeBytes;

    @Column(nullable = false)
    private boolean recalled;

    private Long quoteMessageId;

    @Column(nullable = false)
    private Instant createdAt;

    public ChatMessage() {
    }

    public Long getId() {
        return id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public UserAccount getSender() {
        return sender;
    }

    public void setSender(UserAccount sender) {
        this.sender = sender;
    }

    public MessageKind getKind() {
        return kind;
    }

    public void setKind(MessageKind kind) {
        this.kind = kind;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public void setRecalled(boolean recalled) {
        this.recalled = recalled;
    }

    public Long getQuoteMessageId() {
        return quoteMessageId;
    }

    public void setQuoteMessageId(Long quoteMessageId) {
        this.quoteMessageId = quoteMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
