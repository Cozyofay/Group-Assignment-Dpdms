package zw.ac.uz.dpdms.zoonotic;

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
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncidentRepository;

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
 * Evidence that ward-level hazard scoping, the hazard-specific approval rule and the approval
 * workflow are enforced by the zoonotic-disease-service itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ZoonoticDiseaseServiceIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ZoonoticDiseaseIncidentRepository repository;

    private String recorderWard1;
    private String recorderWard2;
    private String floodRecorderWard1;
    private String supervisor;
    private String floodSupervisor;
    private String national;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        recorderWard1 = token("recorder.zoonotic.ward1", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 1");
        recorderWard2 = token("recorder.zoonotic.ward1.ward2", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 2");
        floodRecorderWard1 = token("recorder.flood.ward1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        supervisor = token("supervisor.zoonotic", Role.PROVINCIAL_SUPERVISOR, HazardType.ZOONOTIC_DISEASE, null);
        floodSupervisor = token("supervisor.flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        national = token("national", Role.NATIONAL_VIEWER, null, null);
    }

    private String token(String username, Role role, HazardType hazard, String ward) {
        return "Bearer " + jwtService.generateToken(new AuthenticatedUser(username, role, hazard, ward));
    }

    private Map<String, Object> payload(String ward, String severity) {
        Map<String, Object> body = new HashMap<>();
        body.put("ward", ward);
        body.put("district", "Rushinga");
        body.put("province", "Mashonaland Central");
        body.put("occurredAt", "2026-03-01T08:30:00");
        body.put("severity", severity);
        body.put("latitude", -16.62);
        body.put("longitude", 32.08);
        body.put("pathogenName", "Anthrax");
        body.put("animalSpeciesAffected", "Cattle");
        body.put("confirmedHumanCases", 4);
        body.put("confirmedAnimalCases", 21);
        body.put("classification", "OUTBREAK");
        return body;
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private long capture() throws Exception {
        String response = mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "MODERATE"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(response);
        return node.get("id").asLong();
    }

    private long approved() throws Exception {
        long id = capture();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", supervisor))
                .andExpect(status().isOk());
        return id;
    }

    @Test
    @DisplayName("A zoonotic disease recorder captures in their own ward")
    void recorderCanCapture() throws Exception {
        mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "HIGH"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.hazard").value("ZOONOTIC_DISEASE"))
                .andExpect(jsonPath("$.reporterUsername").value("recorder.zoonotic.ward1"));
    }

    @Test
    @DisplayName("A flood recorder in the same ward is refused by this service itself")
    void otherHazardRecorderIsRefused() throws Exception {
        mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", floodRecorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "HIGH"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("scoped to Flood")));
    }

    @Test
    void wrongWardIsRefused() throws Exception {
        mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 2", "LOW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCoordinatesAreRejected() throws Exception {
        Map<String, Object> body = payload("Ward 1", "LOW");
        body.put("latitude", 48.85);
        mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.latitude").exists());
    }

    @Test
    @DisplayName("Only this hazard's supervisor may approve")
    void onlyOwnHazardSupervisorApproves() throws Exception {
        long id = capture();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", floodSupervisor))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", recorderWard1))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value("supervisor.zoonotic"));
    }

    @Test
    void rejectionNeedsAReason() throws Exception {
        long id = capture();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/reject")
                        .header("Authorization", supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", ""))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/reject")
                        .header("Authorization", supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "Duplicate record"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Corrections: back to the recorder, edited, resubmitted, then approved")
    void correctionsRoundTrip() throws Exception {
        long id = capture();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/request-corrections")
                        .header("Authorization", supervisor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("comment", "Please verify the figures"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CORRECTIONS_REQUESTED"));
        mvc.perform(put("/api/zoonotic-diseases/" + id)
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "LOW"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/resubmit").header("Authorization", recorderWard1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", supervisor))
                .andExpect(status().isOk());
    }

    @Test
    void approvedIsFinal() throws Exception {
        long id = approved();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", supervisor))
                .andExpect(status().isConflict());
    }

    @Test
    void recorderCannotEditAfterApproval() throws Exception {
        long id = approved();
        mvc.perform(put("/api/zoonotic-diseases/" + id)
                        .header("Authorization", recorderWard1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "CRITICAL"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherRecorderCannotEditOrDelete() throws Exception {
        long id = capture();
        mvc.perform(put("/api/zoonotic-diseases/" + id)
                        .header("Authorization", recorderWard2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 2", "LOW"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/zoonotic-diseases/" + id).header("Authorization", recorderWard2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Pending records are hidden from national users and other recorders")
    void pendingRecordsAreHidden() throws Exception {
        long id = capture();
        mvc.perform(get("/api/zoonotic-diseases/" + id).header("Authorization", national))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/zoonotic-diseases").header("Authorization", national))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/zoonotic-diseases").header("Authorization", recorderWard2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/zoonotic-diseases").header("Authorization", supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Only approved records reach the dashboard, map and reports")
    void approvedFeedOnlyContainsApproved() throws Exception {
        capture();
        long id = approved();
        mvc.perform(get("/api/zoonotic-diseases/approved").header("Authorization", national))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value((int) id))
                .andExpect(jsonPath("$[0].hazard").value("ZOONOTIC_DISEASE"))
                .andExpect(jsonPath("$[0].latitude").exists());
    }

    @Test
    void nationalIsReadOnly() throws Exception {
        long id = approved();
        mvc.perform(get("/api/zoonotic-diseases/" + id).header("Authorization", national))
                .andExpect(status().isOk());
        mvc.perform(post("/api/zoonotic-diseases")
                        .header("Authorization", national)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload("Ward 1", "LOW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutATokenAreRejected() throws Exception {
        mvc.perform(get("/api/zoonotic-diseases")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Every state transition is recorded in the audit trail")
    void auditTrailIsRecorded() throws Exception {
        long id = capture();
        mvc.perform(post("/api/zoonotic-diseases/" + id + "/approve").header("Authorization", supervisor))
                .andExpect(status().isOk());
        mvc.perform(get("/api/zoonotic-diseases/" + id + "/audit").header("Authorization", supervisor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[0].actorUsername").value("recorder.zoonotic.ward1"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"))
                .andExpect(jsonPath("$[1].toStatus").value("APPROVED"));
    }
}
