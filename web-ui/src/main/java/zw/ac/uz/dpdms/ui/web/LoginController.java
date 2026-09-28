package zw.ac.uz.dpdms.ui.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.ui.client.ApiClient;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.Map;

@Controller
public class LoginController {

    private final ApiClient api;

    public LoginController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute(SessionUser.SESSION_KEY) != null) {
            return "redirect:/";
        }
        model.addAttribute("title", "Sign in");
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password,
                        HttpSession session, Model model) {
        try {
            JsonNode response = api.post("/api/auth/login",
                    Map.of("username", username, "password", password), null);
            JsonNode profile = response.get("user");

            SessionUser user = new SessionUser(
                    response.get("accessToken").asText(),
                    profile.get("username").asText(),
                    profile.get("fullName").asText(),
                    Role.valueOf(profile.get("role").asText()),
                    profile.hasNonNull("hazard") ? HazardType.valueOf(profile.get("hazard").asText()) : null,
                    profile.hasNonNull("ward") ? profile.get("ward").asText() : null);

            session.setAttribute(SessionUser.SESSION_KEY, user);
            return "redirect:/";
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("title", "Sign in");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
