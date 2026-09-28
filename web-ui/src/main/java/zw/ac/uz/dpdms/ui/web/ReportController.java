package zw.ac.uz.dpdms.ui.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.ui.client.ApiClient;
import zw.ac.uz.dpdms.ui.client.ApiException;
import zw.ac.uz.dpdms.ui.model.SessionUser;

import java.util.List;
import java.util.Map;

/** Report screen: choose filters, preview the data, then download PDF, Word, Excel or CSV. */
@Controller
public class ReportController extends BaseController {

    private final ApiClient api;

    public ReportController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) List<String> hazards,
                          @RequestParam(required = false) String ward,
                          @RequestParam(required = false) String district,
                          @RequestParam(required = false) String severity,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          @RequestParam(required = false) Boolean preview,
                          HttpSession session, Model model) {
        SessionUser user = user(session);
        addNav(model, user);
        model.addAttribute("title", "Reports");
        model.addAttribute("severities", Severity.values());
        model.addAttribute("selectedHazards", hazards == null ? List.of() : hazards);
        model.addAttribute("ward", ward);
        model.addAttribute("district", district);
        model.addAttribute("severity", severity);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        String q = query(hazards, ward, district, severity, from, to);
        model.addAttribute("query", q);
        for (String format : new String[] {"pdf", "docx", "xlsx", "csv"}) {
            model.addAttribute("download" + format,
                    "/reports/download" + (q.isEmpty() ? "?" : q + "&") + "format=" + format);
        }

        if (Boolean.TRUE.equals(preview)) {
            try {
                model.addAttribute("incidents", api.getList(
                        "/api/reports/preview" + query(hazards, ward, district, severity, from, to), user.token()));
            } catch (ApiException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return "reports";
    }

    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> download(@RequestParam String format,
                                           @RequestParam(required = false) List<String> hazards,
                                           @RequestParam(required = false) String ward,
                                           @RequestParam(required = false) String district,
                                           @RequestParam(required = false) String severity,
                                           @RequestParam(required = false) String from,
                                           @RequestParam(required = false) String to,
                                           HttpSession session) {
        SessionUser user = user(session);
        ResponseEntity<byte[]> response = api.download(
                "/api/reports/" + format + query(hazards, ward, district, severity, from, to), user.token());

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }

    private String query(List<String> hazards, String ward, String district,
                         String severity, String from, String to) {
        StringBuilder query = new StringBuilder();
        if (hazards != null) {
            hazards.forEach(hazard -> append(query, "hazards", hazard));
        }
        append(query, "ward", ward);
        append(query, "district", district);
        append(query, "severity", severity);
        append(query, "from", dateTime(from, "T00:00:00"));
        append(query, "to", dateTime(to, "T23:59:59"));
        return query.toString();
    }

    private void append(StringBuilder query, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.append(query.isEmpty() ? "?" : "&").append(name).append("=").append(value.trim());
    }

    /** The date inputs give "2026-03-01"; the API expects a full date and time. */
    private String dateTime(String date, String suffix) {
        return date == null || date.isBlank() ? null : date.trim() + suffix;
    }

    @GetMapping("/alerts")
    public String alerts(@RequestParam(required = false) String status,
                         HttpSession session, Model model) {
        SessionUser user = user(session);
        addNav(model, user);
        model.addAttribute("title", "Alert log");
        model.addAttribute("status", status);
        try {
            String path = "/api/alerts" + (status == null || status.isBlank() ? "" : "?status=" + status);
            model.addAttribute("alerts", api.getList(path, user.token()));
            model.addAttribute("stats", (Map<String, Object>) api.getMap("/api/alerts/statistics", user.token()));
        } catch (ApiException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("alerts", List.of());
            model.addAttribute("stats", Map.of());
        }
        return "alerts";
    }
}
