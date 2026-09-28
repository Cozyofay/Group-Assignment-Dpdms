package zw.ac.uz.dpdms.flood;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.JwtService;
import zw.ac.uz.dpdms.flood.domain.FloodIncidentRepository;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * These tests are the evidence that ward-level hazard scoping, the hazard-specific approval rule
 * and the approval workflow are enforced by the service itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FloodServiceIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private FloodIncidentRepository repository;

    private String floodRecorderWard1;
    private String floodRecorderWard2;
    private String droughtRecorderWard1;
    private String floodSupervisor;
    private String droughtSupervisor;
    private String national;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        floodRecorderWard1 = token("recorder.flood.ward1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        floodRecorderWard2 = token("recorder.flood.ward2", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 2");
        droughtRecorderWard1 = token("recorder.drought.ward1", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 1");
        floodSupervisor = token("supervisor.flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        droughtSupervisor = token("supervisor.drought", Role.PROVINCIAL_SUPERVISOR, HazardType.DROUGHT, null);
        national = token("national", Role.NATIONAL_VIEWER, null, null);
    }

    private String token(String username, Role role, HazardType hazard, String ward) {
        return "Bearer " + jwtService.generateToken(new AuthenticatedUser(username, role, hazard, ward));
    }

    private Map<String, Object> payload(String ward, double peakLevel, int households, String severity) {
        Map<String, Object> body = new HashMap<>();
        body.put("ward", ward);
        body.put("district", "Rushinga");
        body.put("province", "Mashonaland Central");
        body.put("occurredAt", "2026-03-01T08:30:00");
        body.put("severity", severity);
        body.put("latitude", -16.62);
        body.put("longitude", 32.08);
        body.put("peakWaterLevelMetres", peakLevel);
        body.put("riverBasin", "Mazowe");
        body.put("householdsDisplaced", households);
        body.put("areaFloodedHectares", 120.5);
        body.put("inundationDurationDays", 4);
        return body;
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    /** Captures an incident as the Ward 1 flood recorder and returns its id. */
    private long captureIncident() throws Exception {
        String response = mvc.perform(post("/api/floods")
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 1.2, 10, "MODERATE"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(response);
        return node.get("id").asLong();
    }

    private long approvedIncident() throws Exception {
        long id = captureIncident();
        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isOk());
        return id;
    }

    // ---------------- ward-level hazard scoping ----------------

    @Test
    @DisplayName("A flood recorder captures a flood record in their own ward")
    void recorderCanCapture() throws Exception {
        mvc.perform(post("/api/floods")
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 2.4, 60, "HIGH"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.hazard").value("FLOOD"))
                .andExpect(jsonPath("$.reporterUsername").value("recorder.flood.ward1"));
    }

    @Test
    @DisplayName("A drought recorder in the same ward is refused by the flood service itself")
    void otherHazardRecorderIsRefused() throws Exception {
        mvc.perform(post("/api/floods")
                        .header("Authorization", droughtRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 2.4, 60, "HIGH"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("scoped to Drought")));
    }

    @Test
    @DisplayName("A flood recorder cannot capture for another ward")
    void wrongWardIsRefused() throws Exception {
        mvc.perform(post("/api/floods")
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 2", 1.0, 5, "LOW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCoordinatesAreRejected() throws Exception {
        Map<String, Object> body = payload("Ward 1", 1.0, 5, "LOW");
        body.put("latitude", 48.85);   // Paris, not Zimbabwe
        mvc.perform(post("/api/floods")
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.latitude").exists());
    }

    // ---------------- approval workflow ----------------

    @Test
    @DisplayName("Only the flood supervisor may approve; the drought supervisor is refused")
    void onlyOwnHazardSupervisorApproves() throws Exception {
        long id = captureIncident();

        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", droughtSupervisor))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodRecorderWard1))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("supervisor.flood"));
    }

    @Test
    void rejectionNeedsAReason() throws Exception {
        long id = captureIncident();
        mvc.perform(post("/api/floods/" + id + "/reject")
                        .header("Authorization", floodSupervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", ""))))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/floods/" + id + "/reject")
                        .header("Authorization", floodSupervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "Duplicate of incident 12"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Corrections: back to the recorder, edited, resubmitted, then approved")
    void correctionsRoundTrip() throws Exception {
        long id = captureIncident();

        mvc.perform(post("/api/floods/" + id + "/request-corrections")
                        .header("Authorization", floodSupervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "Check the peak water level against the gauge"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTIONS_REQUESTED"));

        mvc.perform(put("/api/floods/" + id)
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 1.9, 12, "MODERATE"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/floods/" + id + "/resubmit").header("Authorization", floodRecorderWard1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("An approved record cannot be approved again (409)")
    void approvedIsFinal() throws Exception {
        long id = approvedIncident();
        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isConflict());
    }

    @Test
    void recorderCannotEditAfterApproval() throws Exception {
        long id = approvedIncident();
        mvc.perform(put("/api/floods/" + id)
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 9.9, 999, "CRITICAL"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherRecorderCannotEditOrDelete() throws Exception {
        long id = captureIncident();
        mvc.perform(put("/api/floods/" + id)
                        .header("Authorization", floodRecorderWard2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 2", 1.0, 1, "LOW"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/floods/" + id).header("Authorization", floodRecorderWard2))
                .andExpect(status().isForbidden());
    }

    // ---------------- visibility of pending records ----------------

    @Test
    @DisplayName("Pending records are hidden from national users and other recorders")
    void pendingRecordsAreHidden() throws Exception {
        long id = captureIncident();

        mvc.perform(get("/api/floods/" + id).header("Authorization", national))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/floods").header("Authorization", national))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/floods").header("Authorization", floodRecorderWard2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/floods").header("Authorization", floodSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Only approved records reach the dashboard, map and reports")
    void approvedFeedOnlyContainsApproved() throws Exception {
        captureIncident();              // stays PENDING
        long approved = approvedIncident();

        mvc.perform(get("/api/floods/approved").header("Authorization", national))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) approved))
                .andExpect(jsonPath("$[0].hazard").value("FLOOD"))
                .andExpect(jsonPath("$[0].latitude").exists())
                .andExpect(jsonPath("$[0].indicators.riverBasin").value("Mazowe"));
    }

    @Test
    @DisplayName("National users may read but never write")
    void nationalIsReadOnly() throws Exception {
        long id = approvedIncident();
        mvc.perform(get("/api/floods/" + id).header("Authorization", national))
                .andExpect(status().isOk());
        mvc.perform(post("/api/floods")
                        .header("Authorization", national)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 1.0, 1, "LOW"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/floods/" + id).header("Authorization", national))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/floods")).andExpect(status().isUnauthorized());
    }

    // ---------------- audit trail ----------------

    @Test
    @DisplayName("Every state transition is recorded with actor, time and changes")
    void auditTrailIsRecorded() throws Exception {
        long id = captureIncident();
        mvc.perform(put("/api/floods/" + id)
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", 2.7, 80, "HIGH"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/floods/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isOk());

        mvc.perform(get("/api/floods/" + id + "/audit").header("Authorization", floodSupervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].actorUsername").value("recorder.flood.ward1"))
                .andExpect(jsonPath("$[1].action").value("UPDATED"))
                .andExpect(jsonPath("$[1].changes", containsString("peakWaterLevelMetres")))
                .andExpect(jsonPath("$[2].action").value("APPROVED"))
                .andExpect(jsonPath("$[2].fromStatus").value("PENDING"))
                .andExpect(jsonPath("$[2].toStatus").value("APPROVED"));
    }
}
