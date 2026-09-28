package zw.ac.uz.dpdms.ui.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.ui.client.ApiClient;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** User management screen. The auth-service refuses anyone who is not the provincial admin. */
@Controller
public class UserController extends BaseController {

    private final ApiClient api;

    public UserController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        SessionUser user = user(session);
        addNav(model, user);
        model.addAttribute("title", "Users");
        model.addAttribute("roles", Role.values());
        model.addAttribute("hazardOptions", HazardType.values());
        try {
            model.addAttribute("users", api.getList("/api/users", user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("users", List.of());
        }
        return "users";
    }

    @PostMapping("/users")
    public String create(@RequestParam String username, @RequestParam String password,
                         @RequestParam String fullName, @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phoneNumber, @RequestParam String role,
                         @RequestParam(required = false) String hazard, @RequestParam(required = false) String ward,
                         HttpSession session, RedirectAttributes flash) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("fullName", fullName);
        body.put("email", blankToNull(email));
        body.put("phoneNumber", blankToNull(phoneNumber));
        body.put("role", role);
        body.put("hazard", blankToNull(hazard));
        body.put("ward", blankToNull(ward));
        try {
            api.post("/api/users", body, user(session).token());
            flash.addFlashAttribute("message", "User " + username + " created.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@org.springframework.web.bind.annotation.PathVariable Long id,
                         HttpSession session, RedirectAttributes flash) {
        try {
            api.delete("/api/users/" + id, user(session).token());
            flash.addFlashAttribute("message", "User deleted.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
