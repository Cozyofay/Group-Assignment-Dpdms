package zw.ac.uz.dpdms.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import zw.ac.uz.dpdms.alert.domain.AlertChannel;
import zw.ac.uz.dpdms.alert.domain.AlertLog;
import zw.ac.uz.dpdms.alert.domain.AlertLogRepository;
import zw.ac.uz.dpdms.alert.domain.DeliveryStatus;
import zw.ac.uz.dpdms.alert.service.AlertDispatchService;
import zw.ac.uz.dpdms.alert.service.RecipientLookup;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.AlertRecipient;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AlertDispatchServiceTest {

    @Autowired
    private AlertDispatchService dispatchService;
    @Autowired
    private AlertLogRepository repository;
    @MockBean
    private RecipientLookup recipientLookup;

    private AlertEvent event() {
        IncidentSummary summary = new IncidentSummary(HazardType.FLOOD, 7L, "Ward 1", "Rushinga",
                "Mashonaland Central", LocalDateTime.of(2026, 3, 1, 8, 30), Severity.HIGH,
                zw.ac.uz.dpdms.common.domain.IncidentStatus.PENDING, -16.62, 32.08, "recorder.flood.ward1",
                "Flood in Ward 1 (Mazowe): peak 2.8 m", Map.of("peakWaterLevelMetres", 2.8));
        return AlertEvent.of(summary, "Peak water level of 2.8 m is above the danger threshold");
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(recipientLookup.recipientsFor(any(HazardType.class), anyString())).thenReturn(List.of(
                new AlertRecipient("supervisor.flood", "Flood Supervisor", Role.PROVINCIAL_SUPERVISOR,
                        "supervisor.flood@dpdms.local", "+263771234567"),
                new AlertRecipient("admin", "Provincial Administrator", Role.PROVINCIAL_ADMIN,
                        "admin@dpdms.local", null)));
    }

    @Test
    @DisplayName("One log entry per recipient per channel, with channel, recipient, time and status")
    void everyAttemptIsLogged() {
        dispatchService.dispatch(event());

        List<AlertLog> entries = repository.findAll();
        assertEquals(4, entries.size(), "2 recipients x 2 channels");
        assertTrue(entries.stream().allMatch(e -> e.getSentAt() != null));
        assertTrue(entries.stream().anyMatch(e -> e.getChannel() == AlertChannel.EMAIL));
        assertTrue(entries.stream().anyMatch(e -> e.getChannel() == AlertChannel.WHATSAPP));
        assertTrue(entries.stream().allMatch(e -> e.getHazard() == HazardType.FLOOD && e.getIncidentId() == 7L));
    }

    @Test
    @DisplayName("Disabled channels and missing addresses are recorded as SKIPPED, never lost")
    void disabledChannelsAreSkipped() {
        dispatchService.dispatch(event());

        AlertLog noPhone = repository.findAll().stream()
                .filter(e -> e.getChannel() == AlertChannel.WHATSAPP && e.getRecipientUsername().equals("admin"))
                .findFirst().orElseThrow();
        assertEquals(DeliveryStatus.SKIPPED, noPhone.getStatus());
        assertEquals("No phone number on file", noPhone.getErrorMessage());
    }

    @Test
    @DisplayName("A redelivered message does not send the same alert twice")
    void deliveryIsIdempotent() {
        AlertEvent event = event();
        dispatchService.dispatch(event);
        dispatchService.dispatch(event);
        assertEquals(4, repository.findAll().size());
    }
}
