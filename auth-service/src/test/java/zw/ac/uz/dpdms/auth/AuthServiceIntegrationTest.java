package zw.ac.uz.dpdms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.InternalApiKeyFilter;
import zw.ac.uz.dpdms.common.security.JwtService;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    private static final String SEED_PASSWORD = "Test12345";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private JwtService jwtService;

    private String bearer(String username, Role role, HazardType hazard, String ward) {
        return "Bearer " + jwtService.generateToken(new AuthenticatedUser(username, role, hazard, ward));
    }

    private String adminToken() {
        return bearer("admin", Role.PROVINCIAL_ADMIN, null, null);
    }

    private String json(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private Map<String, Object> newUser(String username, Role role, HazardType hazard, String ward) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "Password123");
        body.put("fullName", "Test User");
        body.put("email", username + "@example.com");
        body.put("role", role);
        body.put("hazard", hazard);
        body.put("ward", ward);
        return body;
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("Login returns a signed token that carries role, hazard and ward")
    void loginIssuesScopedToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "recorder.flood.ward1", "password", SEED_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("WARD_RECORDER"))
                .andReturn();

        String token = mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        AuthenticatedUser user = jwtService.parse(token);
        assertEquals(Role.WARD_RECORDER, user.role());
        assertEquals(HazardType.FLOOD, user.hazard());
        assertEquals("Ward 1", user.ward());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "supervisor.fire", "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Account locks after 5 failed attempts, even with the right password afterwards")
    void lockoutAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", "supervisor.mining", "password", "WrongPass1"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "supervisor.mining", "password", SEED_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("locked")));
    }

    @Test
    void meRequiresAToken() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedTokenIsRejected() throws Exception {
        JwtService attacker = new JwtService("some-other-secret-that-is-long-enough-123", 60);
        String forged = attacker.generateToken(new AuthenticatedUser("admin", Role.PROVINCIAL_ADMIN, null, null));
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- user management (admin only) ----------------

    @Test
    void adminCanCreateARecorder() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("recorder.fire.ward7", Role.WARD_RECORDER, HazardType.FIRE, "Ward 7"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("recorder.fire.ward7"))
                .andExpect(jsonPath("$.hazard").value("FIRE"))
                .andExpect(jsonPath("$.ward").value("Ward 7"));
    }

    @Test
    @DisplayName("A recorder must be scoped to a ward")
    void recorderWithoutWardIsRejected() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("recorder.noward", Role.WARD_RECORDER, HazardType.FLOOD, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A supervisor must be scoped to a hazard")
    void supervisorWithoutHazardIsRejected() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("supervisor.none", Role.PROVINCIAL_SUPERVISOR, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("supervisor.flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        Map<String, Object> body = newUser("weak.user", Role.NATIONAL_VIEWER, null, null);
        body.put("password", "short");
        mvc.perform(post("/api/users")
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void recorderCannotManageUsers() throws Exception {
        mvc.perform(get("/api/users")
                        .header("Authorization", bearer("recorder.flood.ward1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorCannotManageUsers() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", bearer("supervisor.flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("sneaky", Role.PROVINCIAL_ADMIN, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("National users are read-only: every write is 403")
    void nationalCannotWrite() throws Exception {
        mvc.perform(post("/api/users")
                        .header("Authorization", bearer("national", Role.NATIONAL_VIEWER, null, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(newUser("another", Role.NATIONAL_VIEWER, null, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("read-only")));
    }

    @Test
    @DisplayName("National users may still change their own password")
    void nationalCanChangeOwnPassword() throws Exception {
        mvc.perform(post("/api/auth/change-password")
                        .header("Authorization", bearer("national", Role.NATIONAL_VIEWER, null, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", SEED_PASSWORD, "newPassword", "NewPass2026"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void passwordHashIsNeverReturned() throws Exception {
        mvc.perform(get("/api/users").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    // ---------------- internal API ----------------

    @Test
    void internalEndpointNeedsTheApiKey() throws Exception {
        mvc.perform(get("/internal/users/alert-recipients").param("hazard", "FLOOD").param("ward", "Ward 1"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/internal/users/alert-recipients").param("hazard", "FLOOD").param("ward", "Ward 1")
                        .header(InternalApiKeyFilter.HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Alert recipients: that hazard's supervisor and ward recorder, admin, national - nobody else")
    void alertRecipientsAreHazardScoped() throws Exception {
        mvc.perform(get("/internal/users/alert-recipients").param("hazard", "FLOOD").param("ward", "Ward 1")
                        .header(InternalApiKeyFilter.HEADER, "test-internal-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItem("supervisor.flood")))
                .andExpect(jsonPath("$[*].username", hasItem("recorder.flood.ward1")))
                .andExpect(jsonPath("$[*].username", hasItem("admin")))
                .andExpect(jsonPath("$[*].username", not(hasItem("supervisor.drought"))))
                .andExpect(jsonPath("$[*].username", not(hasItem("recorder.flood.ward2"))))
                .andExpect(jsonPath("$[*].username", not(hasItem("recorder.drought.ward1"))));
    }
}
