package zw.ac.uz.dpdms.ui.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.List;

/** Shared helpers: the logged-in user and the hazards they are allowed to see. */
public abstract class BaseController {

    protected SessionUser user(HttpSession session) {
        SessionUser user = (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
        if (user == null) {
            throw new IllegalStateException("Not logged in");
        }
        return user;
    }

    /** A recorder or supervisor sees only their hazard; the admin and national users see all five. */
    protected List<HazardType> visibleHazards(SessionUser user) {
        return user.hazard() != null ? List.of(user.hazard()) : List.of(HazardType.values());
    }

    @ModelAttribute("currentUser")
    public SessionUser currentUser(HttpSession session) {
        return (SessionUser) session.getAttribute(SessionUser.SESSION_KEY);
    }

    @ModelAttribute("allHazards")
    public HazardType[] allHazards() {
        return HazardType.values();
    }

    protected void addNav(Model model, SessionUser user) {
        model.addAttribute("hazards", visibleHazards(user));
    }
}
