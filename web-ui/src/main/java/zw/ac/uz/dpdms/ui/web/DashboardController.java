package zw.ac.uz.dpdms.ui.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import zw.ac.uz.dpdms.ui.client.ApiClient;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.List;
import java.util.Map;

/** The live dashboard: counts, trend, recent incidents and the interactive map. */
@Controller
public class DashboardController extends BaseController {

    private final ApiClient api;

    public DashboardController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/")
    public String dashboard(@RequestParam(required = false) String ward,
                            @RequestParam(required = false) String severity,
                            HttpSession session, Model model) {
        SessionUser user = user(session);
        addNav(model, user);
        model.addAttribute("title", "Dashboard");
        model.addAttribute("ward", ward);
        model.addAttribute("severity", severity);

        try {
            model.addAttribute("summary", api.getMap("/api/dashboard/summary" + query(ward, severity), user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", "The dashboard service is not available: " + ex.getMessage());
            model.addAttribute("summary", Map.of());
        }
        return "dashboard";
    }

    /** The browser fetches map markers from here, so the JWT never leaves the server. */
    @GetMapping(value = "/map-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> mapData(@RequestParam(required = false) String ward,
                                             @RequestParam(required = false) String severity,
                                             HttpSession session) {
        try {
            return api.getList("/api/dashboard/map" + query(ward, severity), user(session).token());
        } catch (ApiException ex) {
            return List.of();
        }
    }

    private String query(String ward, String severity) {
        StringBuilder query = new StringBuilder();
        if (ward != null && !ward.isBlank()) {
            query.append(query.isEmpty() ? "?" : "&").append("ward=").append(ward.trim());
        }
        if (severity != null && !severity.isBlank()) {
            query.append(query.isEmpty() ? "?" : "&").append("severity=").append(severity);
        }
        return query.toString();
    }
}
