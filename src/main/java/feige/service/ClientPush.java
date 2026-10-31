package feige.service;

import java.util.Collection;

public interface ClientPush {

    void sendToUsers(Collection<Long> userIds, Object payload);

    void sendToAll(Object payload);
}
