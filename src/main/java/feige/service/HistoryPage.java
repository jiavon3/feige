package feige.service;

import java.util.List;

public class HistoryPage {

    private final List<MessageView> messages;
    private final boolean hasMore;

    public HistoryPage(List<MessageView> messages, boolean hasMore) {
        this.messages = messages;
        this.hasMore = hasMore;
    }

    public List<MessageView> getMessages() {
        return messages;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
