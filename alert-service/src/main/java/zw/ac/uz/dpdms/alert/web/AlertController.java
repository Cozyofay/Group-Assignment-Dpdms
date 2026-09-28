package zw.ac.uz.dpdms.alert.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.ac.uz.dpdms.alert.domain.AlertChannel;
import zw.ac.uz.dpdms.alert.domain.DeliveryStatus;
import zw.ac.uz.dpdms.alert.dto.AlertLogResponse;
import zw.ac.uz.dpdms.alert.service.AlertLogService;
import zw.ac.uz.dpdms.common.domain.HazardType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "The log of every alert sent by email and WhatsApp")
public class AlertController {

    private final AlertLogService alertLogService;

    public AlertController(AlertLogService alertLogService) {
        this.alertLogService = alertLogService;
    }

    @GetMapping
    @Operation(summary = "Alert log: channel, recipient, timestamp and delivery status")
    public List<AlertLogResponse> list(
            @RequestParam(required = false) HazardType hazard,
            @RequestParam(required = false) AlertChannel channel,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return alertLogService.list(hazard, channel, status, from, to);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Counts of sent, failed and skipped alerts")
    public Map<String, Object> statistics() {
        return alertLogService.statistics();
    }
}
