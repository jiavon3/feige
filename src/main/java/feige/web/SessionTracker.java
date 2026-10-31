package feige.web;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionTracker implements HttpSessionListener, ApplicationListener<InteractiveAuthenticationSuccessEvent> {

    private final ConcurrentHashMap<String, Set<HttpSession>> sessions = new ConcurrentHashMap<String, Set<HttpSession>>();

    @Override
    public void onApplicationEvent(InteractiveAuthenticationSuccessEvent event) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)) {
            return;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpSession session = attributes.getRequest().getSession(false);
        if (session == null || event.getAuthentication() == null || event.getAuthentication().getName() == null) {
            return;
        }
        String key = event.getAuthentication().getName().toLowerCase(Locale.ROOT);
        sessions.computeIfAbsent(key, name -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        for (Set<HttpSession> group : sessions.values()) {
            group.remove(session);
        }
    }

    public void expire(String username) {
        if (username == null) {
            return;
        }
        Set<HttpSession> group = sessions.remove(username.toLowerCase(Locale.ROOT));
        if (group == null) {
            return;
        }
        for (HttpSession session : group) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // 会话已经失效
            }
        }
    }
}
