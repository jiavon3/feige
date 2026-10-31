package feige.web;

import feige.service.ThemeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ThemeController {

    private final ThemeService themes;
    private final SocketHub hub;

    public ThemeController(ThemeService themes, SocketHub hub) {
        this.themes = themes;
        this.hub = hub;
    }

    @GetMapping("/api/theme")
    public Map<String, String> current(HttpServletResponse response) {
        String theme = themes.current();
        remember(response, theme);
        return Collections.singletonMap("theme", theme);
    }

    @PostMapping("/api/admin/theme")
    public Map<String, String> update(@RequestBody ThemeRequest request, HttpServletResponse response) {
        String theme = themes.update(request == null ? null : request.theme);
        remember(response, theme);
        Map<String, Object> event = new LinkedHashMap<String, Object>();
        event.put("type", "theme");
        event.put("theme", theme);
        hub.sendToAll(event);
        return Collections.singletonMap("theme", theme);
    }

    private static void remember(HttpServletResponse response, String theme) {
        ResponseCookie cookie = ResponseCookie.from("feige-theme", theme)
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static class ThemeRequest {
        public String theme;
    }
}
