package zw.ac.uz.dpdms.ui.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.SessionUser;

/** Shows backend errors (including 403s) on a normal page instead of a stack trace. */
@Controller
@ControllerAdvice
public class UiErrorController {

    @ExceptionHandler(ApiException.class)
    public String apiError(ApiException ex, HttpSession session, Model model) {
        model.addAttribute("title", "Something went wrong");
        model.addAttribute("status", ex.getStatus());
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("currentUser", session.getAttribute(SessionUser.SESSION_KEY));
        return "error-page";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String badInput(IllegalArgumentException ex, HttpSession session, Model model) {
        model.addAttribute("title", "Invalid request");
        model.addAttribute("status", 400);
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("currentUser", session.getAttribute(SessionUser.SESSION_KEY));
        return "error-page";
    }
}
