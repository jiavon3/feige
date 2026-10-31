package feige.web;

import feige.domain.FeigeUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/chat";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal FeigeUser user) {
        if (user != null) {
            return "redirect:/chat";
        }
        return "forward:/login.html";
    }

    @GetMapping("/register")
    public String register(@AuthenticationPrincipal FeigeUser user) {
        if (user != null) {
            return "redirect:/chat";
        }
        return "forward:/register.html";
    }

    @GetMapping("/chat")
    public String chat() {
        return "forward:/chat.html";
    }

    @GetMapping("/admin")
    public String admin() {
        return "forward:/admin.html";
    }
}
