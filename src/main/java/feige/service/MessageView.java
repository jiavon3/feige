package feige.service;

import java.time.Instant;

public class MessageView {

    private final long id;
    private final long conversationId;
    private final long senderId;
    private final String senderName;
    private final String kind;
    private final String body;
    private final Long sizeBytes;
    private final boolean recalled;
    private final Long quoteMessageId;
    private final String quoteSenderName;
    private final String quoteKind;
    private final String quoteBody;
    private final Instant createdAt;

    public MessageView(long id, long conversationId, long senderId, String senderName,
                       String kind, String body, Long sizeBytes, boolean recalled,
                       Long quoteMessageId, String quoteSenderName, String quoteKind, String quoteBody,
                       Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.kind = kind;
        this.body = body;
        this.sizeBytes = sizeBytes;
        this.recalled = recalled;
        this.quoteMessageId = quoteMessageId;
        this.quoteSenderName = quoteSenderName;
        this.quoteKind = quoteKind;
        this.quoteBody = quoteBody;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getConversationId() {
        return conversationId;
    }

    public long getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getKind() {
        return kind;
    }

    public String getBody() {
        return body;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public boolean isRecalled() {
        return recalled;
    }

    public Long getQuoteMessageId() {
        return quoteMessageId;
    }

    public String getQuoteSenderName() {
        return quoteSenderName;
    }

    public String getQuoteKind() {
        return quoteKind;
    }

    public String getQuoteBody() {
        return quoteBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
