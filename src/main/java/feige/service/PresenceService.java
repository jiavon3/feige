package feige.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PresenceService {

    private final ConcurrentHashMap<Long, AtomicInteger> counts = new ConcurrentHashMap<Long, AtomicInteger>();

    public boolean connected(long userId) {
        final boolean[] first = {false};
        counts.compute(userId, (id, count) -> {
            if (count == null) {
                first[0] = true;
                return new AtomicInteger(1);
            }
            count.incrementAndGet();
            return count;
        });
        return first[0];
    }

    public boolean disconnected(long userId) {
        final boolean[] last = {false};
        counts.computeIfPresent(userId, (id, count) -> {
            if (count.decrementAndGet() <= 0) {
                last[0] = true;
                return null;
            }
            return count;
        });
        return last[0];
    }

    public boolean isOnline(long userId) {
        AtomicInteger count = counts.get(userId);
        return count != null && count.get() > 0;
    }
}
