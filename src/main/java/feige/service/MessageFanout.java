package feige.service;

import feige.domain.Conversation;
import feige.domain.ConversationType;
import feige.repo.ConversationMemberRepository;
import feige.repo.ConversationRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MessageFanout {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ClientPush push;

    public MessageFanout(ConversationRepository conversations,
                         ConversationMemberRepository members,
                         ClientPush push) {
        this.conversations = conversations;
        this.members = members;
        this.push = push;
    }

    public void deliver(MessageView view) {
        Conversation conversation = conversations.findById(view.getConversationId()).orElse(null);
        if (conversation == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "message");
        payload.put("message", view);
        deliverToConversation(conversation, payload);
    }

    public void deliverRecall(MessageView view) {
        Conversation conversation = conversations.findById(view.getConversationId()).orElse(null);
        if (conversation == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "recall");
        payload.put("messageId", view.getId());
        payload.put("conversationId", view.getConversationId());
        payload.put("senderName", view.getSenderName());
        deliverToConversation(conversation, payload);
    }

    public void deliverRead(long userId, long conversationId, long lastReadMessageId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "read");
        payload.put("conversationId", conversationId);
        payload.put("lastReadMessageId", lastReadMessageId);
        push.sendToUsers(Collections.singletonList(userId), payload);
    }

    public void deliverConversationCleared(long conversationId) {
        Conversation conversation = conversations.findById(conversationId).orElse(null);
        if (conversation == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("type", "conversation.cleared");
        payload.put("conversationId", conversationId);
        deliverToConversation(conversation, payload);
    }

    private void deliverToConversation(Conversation conversation, Map<String, Object> payload) {
        if (conversation.getType() == ConversationType.HALL) {
            push.sendToAll(payload);
            return;
        }
        push.sendToUsers(members.findUserIdsByConversationId(conversation.getId()), payload);
    }
}
