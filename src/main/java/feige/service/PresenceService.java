package feige.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PresenceService {

    private final ConcurrentHashMap<Long, AtomicInteger> counts = new ConcurrentHashMap<Long, AtomicInteger>();

    public boolean connected(long userId) {
        AtomicInteger created = new AtomicInteger(0);
        AtomicInteger existing = counts.putIfAbsent(userId, created);
        AtomicInteger count = existing == null ? created : existing;
        return count.incrementAndGet() == 1;
    }

    public boolean disconnected(long userId) {
        AtomicInteger count = counts.get(userId);
        if (count == null) {
            return false;
        }
        if (count.decrementAndGet() <= 0) {
            counts.remove(userId, count);
            return true;
        }
        return false;
    }

    public boolean isOnline(long userId) {
        AtomicInteger count = counts.get(userId);
        return count != null && count.get() > 0;
    }
}
