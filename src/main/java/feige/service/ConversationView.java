package feige.service;

public class ConversationView {

    private final long id;
    private final String type;

    public ConversationView(long id, String type) {
        this.id = id;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
