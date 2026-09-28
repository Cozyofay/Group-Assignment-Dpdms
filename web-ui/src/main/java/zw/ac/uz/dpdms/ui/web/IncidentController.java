package zw.ac.uz.dpdms.ui.web;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.ui.client.ApiClient;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.FormField;
import zw.ac.uz.dpdms.ui.model.HazardForms;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capture, list, edit, approve and reject incidents. Nothing is decided here: every action is a
 * call to the hazard service, which applies the rules and may answer 403.
 */
@Controller
public class IncidentController extends BaseController {

    private final ApiClient api;

    public IncidentController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/incidents/{hazardPath}")
    public String list(@PathVariable String hazardPath,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String ward,
                       HttpSession session, Model model) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        addNav(model, user);
        model.addAttribute("title", hazard.getLabel() + " incidents");
        model.addAttribute("hazard", hazard);
        model.addAttribute("status", status);
        model.addAttribute("ward", ward);
        model.addAttribute("statuses", List.of("PENDING", "APPROVED", "REJECTED", "CORRECTIONS_REQUESTED"));

        StringBuilder query = new StringBuilder("/api/" + hazard.getApiPath());
        boolean first = true;
        if (status != null && !status.isBlank()) {
            query.append(first ? "?" : "&").append("status=").append(status);
            first = false;
        }
        if (ward != null && !ward.isBlank()) {
            query.append(first ? "?" : "&").append("ward=").append(ward.trim());
        }
        try {
            model.addAttribute("incidents", api.getList(query.toString(), user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("incidents", List.of());
        }
        return "incidents";
    }

    @GetMapping("/incidents/{hazardPath}/new")
    public String captureForm(@PathVariable String hazardPath, HttpSession session, Model model) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        addNav(model, user);
        model.addAttribute("title", "Capture a " + hazard.getLabel().toLowerCase() + " incident");
        model.addAttribute("hazard", hazard);
        model.addAttribute("fields", HazardForms.fieldsFor(hazard));
        model.addAttribute("severities", Severity.values());
        model.addAttribute("values", new HashMap<String, String>());
        model.addAttribute("editing", false);
        return "incident-form";
    }

    @PostMapping("/incidents/{hazardPath}")
    public String create(@PathVariable String hazardPath, HttpServletRequest request,
                         HttpSession session, RedirectAttributes flash) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        try {
            JsonNode created = api.post("/api/" + hazard.getApiPath(), body(hazard, request), user.token());
            flash.addFlashAttribute("message",
                    hazard.getLabel() + " incident #" + created.get("id").asLong()
                            + " captured and sent to the provincial supervisor for approval.");
            return "redirect:/incidents/" + hazard.getApiPath();
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/incidents/" + hazard.getApiPath() + "/new";
        }
    }

    @GetMapping("/incidents/{hazardPath}/{id}")
    public String detail(@PathVariable String hazardPath, @PathVariable Long id,
                         HttpSession session, Model model) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        addNav(model, user);
        model.addAttribute("hazard", hazard);
        model.addAttribute("fields", HazardForms.fieldsFor(hazard));
        try {
            Map<String, Object> incident = api.getMap("/api/" + hazard.getApiPath() + "/" + id, user.token());
            model.addAttribute("incident", incident);
            model.addAttribute("title", hazard.getLabel() + " incident #" + id);
            model.addAttribute("audit", api.getList("/api/" + hazard.getApiPath() + "/" + id + "/audit",
                    user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("title", "Incident not available");
        }
        return "incident-detail";
    }

    @GetMapping("/incidents/{hazardPath}/{id}/edit")
    public String editForm(@PathVariable String hazardPath, @PathVariable Long id,
                           HttpSession session, Model model) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        addNav(model, user);
        model.addAttribute("title", "Edit " + hazard.getLabel().toLowerCase() + " incident #" + id);
        model.addAttribute("hazard", hazard);
        model.addAttribute("fields", HazardForms.fieldsFor(hazard));
        model.addAttribute("severities", Severity.values());
        model.addAttribute("editing", true);
        model.addAttribute("incidentId", id);
        try {
            model.addAttribute("values", api.getMap("/api/" + hazard.getApiPath() + "/" + id, user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("values", Map.of());
        }
        return "incident-form";
    }

    @PostMapping("/incidents/{hazardPath}/{id}/edit")
    public String update(@PathVariable String hazardPath, @PathVariable Long id,
                         HttpServletRequest request, HttpSession session, RedirectAttributes flash) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        try {
            api.put("/api/" + hazard.getApiPath() + "/" + id, body(hazard, request), user.token());
            flash.addFlashAttribute("message", "Incident #" + id + " updated.");
            return "redirect:/incidents/" + hazard.getApiPath() + "/" + id;
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/incidents/" + hazard.getApiPath() + "/" + id + "/edit";
        }
    }

    /** approve | reject | request-corrections | resubmit */
    @PostMapping("/incidents/{hazardPath}/{id}/{action}")
    public String workflowAction(@PathVariable String hazardPath, @PathVariable Long id,
                                 @PathVariable String action,
                                 @RequestParam(required = false) String comment,
                                 HttpSession session, RedirectAttributes flash) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        List<String> allowed = List.of("approve", "reject", "request-corrections", "resubmit");
        if (!allowed.contains(action)) {
            flash.addFlashAttribute("error", "Unknown action");
            return "redirect:/incidents/" + hazard.getApiPath() + "/" + id;
        }
        try {
            Map<String, Object> body = comment == null ? Map.of() : Map.of("comment", comment);
            api.post("/api/" + hazard.getApiPath() + "/" + id + "/" + action, body, user.token());
            flash.addFlashAttribute("message", "Incident #" + id + ": " + action.replace('-', ' ') + " done.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/incidents/" + hazard.getApiPath() + "/" + id;
    }

    @PostMapping("/incidents/{hazardPath}/{id}/delete")
    public String delete(@PathVariable String hazardPath, @PathVariable Long id,
                         HttpSession session, RedirectAttributes flash) {
        SessionUser user = user(session);
        HazardType hazard = HazardForms.fromPath(hazardPath);
        try {
            api.delete("/api/" + hazard.getApiPath() + "/" + id, user.token());
            flash.addFlashAttribute("message", "Incident #" + id + " deleted.");
        } catch (ApiException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/incidents/" + hazard.getApiPath();
    }

    /** Builds the JSON body from the form: shared metadata plus that hazard's five indicators. */
    private Map<String, Object> body(HazardType hazard, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("ward", request.getParameter("ward"));
        body.put("district", request.getParameter("district"));
        body.put("province", request.getParameter("province"));
        body.put("occurredAt", request.getParameter("occurredAt"));
        body.put("severity", request.getParameter("severity"));
        body.put("latitude", number(request.getParameter("latitude")));
        body.put("longitude", number(request.getParameter("longitude")));

        for (FormField field : HazardForms.fieldsFor(hazard)) {
            String raw = request.getParameter(field.name());
            if (raw == null || raw.isBlank()) {
                body.put(field.name(), null);
            } else if ("number".equals(field.type())) {
                body.put(field.name(), number(raw));
            } else if ("true".equals(raw) || "false".equals(raw)) {
                body.put(field.name(), Boolean.valueOf(raw));
            } else {
                body.put(field.name(), raw.trim());
            }
        }
        return body;
    }

    private Object number(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return raw.trim();   // let the backend validation produce the message
        }
    }
}
