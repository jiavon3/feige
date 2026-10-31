package feige.web;

import feige.domain.FeigeUser;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FeigeUser)) {
            return false;
        }
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        if (!sameHost(servletRequest)) {
            return false;
        }
        FeigeUser user = (FeigeUser) authentication.getPrincipal();
        attributes.put("userId", Long.valueOf(user.getId()));
        attributes.put("username", user.getUsername());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static boolean sameHost(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.trim().isEmpty()) {
            return true;
        }
        try {
            return request.getServerName().equalsIgnoreCase(new URL(origin).getHost());
        } catch (MalformedURLException ex) {
            return false;
        }
    }
}
