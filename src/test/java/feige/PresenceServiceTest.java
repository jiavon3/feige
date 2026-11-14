package feige;

import feige.service.PresenceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresenceServiceTest {

    @Test
    void staysOnlineUntilTheLastSessionCloses() {
        PresenceService presence = new PresenceService();
        assertTrue(presence.connected(9101));
        assertFalse(presence.connected(9101));
        assertTrue(presence.isOnline(9101));
        assertFalse(presence.disconnected(9101));
        assertTrue(presence.disconnected(9101));
        assertFalse(presence.isOnline(9101));
        assertFalse(presence.disconnected(9101));
    }
}
