package zw.ac.uz.dpdms.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.domain.ScopedIncident;
import zw.ac.uz.dpdms.common.exception.HazardAccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static zw.ac.uz.dpdms.common.domain.HazardType.DROUGHT;
import static zw.ac.uz.dpdms.common.domain.HazardType.FLOOD;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canAccessHazard;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canCreate;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canDelete;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canEdit;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canResubmit;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canReview;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.canView;
import static zw.ac.uz.dpdms.common.security.HazardAccessPolicy.listScope;

class HazardAccessPolicyTest {

    private static final AuthenticatedUser FLOOD_RECORDER_W1 =
            new AuthenticatedUser("rec.flood.w1", Role.WARD_RECORDER, FLOOD, "Ward 1");
    private static final AuthenticatedUser FLOOD_RECORDER_W2 =
            new AuthenticatedUser("rec.flood.w2", Role.WARD_RECORDER, FLOOD, "Ward 2");
    private static final AuthenticatedUser DROUGHT_RECORDER_W1 =
            new AuthenticatedUser("rec.drought.w1", Role.WARD_RECORDER, DROUGHT, "Ward 1");
    private static final AuthenticatedUser FLOOD_SUPERVISOR =
            new AuthenticatedUser("sup.flood", Role.PROVINCIAL_SUPERVISOR, FLOOD, null);
    private static final AuthenticatedUser DROUGHT_SUPERVISOR =
            new AuthenticatedUser("sup.drought", Role.PROVINCIAL_SUPERVISOR, DROUGHT, null);
    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser("admin", Role.PROVINCIAL_ADMIN, null, null);
    private static final AuthenticatedUser NATIONAL =
            new AuthenticatedUser("national", Role.NATIONAL_VIEWER, null, null);

    private record Incident(String ward, String reporter, IncidentStatus status) implements ScopedIncident {
        @Override public String getWard() { return ward; }
        @Override public String getReporterUsername() { return reporter; }
        @Override public IncidentStatus getStatus() { return status; }
    }

    private static Incident floodW1(IncidentStatus status) {
        return new Incident("Ward 1", "rec.flood.w1", status);
    }

    @Test
    @DisplayName("Ward recorder may capture only their own hazard in their own ward")
    void recorderCreateScope() {
        assertTrue(canCreate(FLOOD_RECORDER_W1, FLOOD, "Ward 1"));
        assertTrue(canCreate(FLOOD_RECORDER_W1, FLOOD, " ward 1 "), "ward match ignores case/whitespace");
        assertFalse(canCreate(FLOOD_RECORDER_W1, DROUGHT, "Ward 1"), "other hazard, same ward");
        assertFalse(canCreate(FLOOD_RECORDER_W1, FLOOD, "Ward 2"), "same hazard, other ward");
        assertFalse(canCreate(DROUGHT_RECORDER_W1, FLOOD, "Ward 1"));
    }

    @Test
    @DisplayName("Recorders and supervisors cannot reach another hazard's service at all")
    void serviceLevelScoping() {
        assertTrue(canAccessHazard(FLOOD_RECORDER_W1, FLOOD));
        assertFalse(canAccessHazard(FLOOD_RECORDER_W1, DROUGHT));
        assertTrue(canAccessHazard(FLOOD_SUPERVISOR, FLOOD));
        assertFalse(canAccessHazard(FLOOD_SUPERVISOR, DROUGHT));
    }

    @Test
    @DisplayName("Only the supervisor of that hazard may approve/reject/request corrections")
    void reviewScope() {
        assertTrue(canReview(FLOOD_SUPERVISOR, FLOOD));
        assertFalse(canReview(FLOOD_SUPERVISOR, DROUGHT));
        assertFalse(canReview(DROUGHT_SUPERVISOR, FLOOD));
        assertFalse(canReview(FLOOD_RECORDER_W1, FLOOD));
        assertFalse(canReview(ADMIN, FLOOD));
        assertFalse(canReview(NATIONAL, FLOOD));
    }

    @ParameterizedTest
    @EnumSource(HazardType.class)
    @DisplayName("National viewer: reads every hazard, writes nothing")
    void nationalIsReadOnly(HazardType hazard) {
        Incident pending = new Incident("Ward 1", "someone", IncidentStatus.PENDING);
        Incident approved = new Incident("Ward 1", "someone", IncidentStatus.APPROVED);
        assertTrue(canAccessHazard(NATIONAL, hazard));
        assertTrue(canView(NATIONAL, hazard, approved));
        assertFalse(canView(NATIONAL, hazard, pending));
        assertFalse(canCreate(NATIONAL, hazard, "Ward 1"));
        assertFalse(canEdit(NATIONAL, hazard, pending));
        assertFalse(canDelete(NATIONAL, hazard, pending));
        assertFalse(canReview(NATIONAL, hazard));
        assertEquals(ListScope.APPROVED_ONLY, listScope(NATIONAL));
    }

    @Test
    @DisplayName("Pending record visible only to creator, that hazard's supervisor and the admin")
    void pendingVisibility() {
        Incident pending = floodW1(IncidentStatus.PENDING);
        assertTrue(canView(FLOOD_RECORDER_W1, FLOOD, pending));
        assertTrue(canView(FLOOD_SUPERVISOR, FLOOD, pending));
        assertTrue(canView(ADMIN, FLOOD, pending));

        assertFalse(canView(FLOOD_RECORDER_W2, FLOOD, pending), "other recorder of same hazard");
        assertFalse(canView(DROUGHT_SUPERVISOR, FLOOD, pending), "supervisor of other hazard");
        assertFalse(canView(DROUGHT_RECORDER_W1, FLOOD, pending), "recorder in same ward, other hazard");
        assertFalse(canView(NATIONAL, FLOOD, pending));
    }

    @Test
    @DisplayName("Approved records are visible to anyone authorised for that hazard")
    void approvedVisibility() {
        Incident approved = floodW1(IncidentStatus.APPROVED);
        assertTrue(canView(FLOOD_RECORDER_W2, FLOOD, approved));
        assertTrue(canView(NATIONAL, FLOOD, approved));
        assertFalse(canView(DROUGHT_RECORDER_W1, FLOOD, approved));
        assertFalse(canView(DROUGHT_SUPERVISOR, FLOOD, approved));
    }

    @Test
    @DisplayName("Recorder edits only their own record, and only before a final decision")
    void editRules() {
        assertTrue(canEdit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.PENDING)));
        assertTrue(canEdit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.CORRECTIONS_REQUESTED)));
        assertFalse(canEdit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.APPROVED)));
        assertFalse(canEdit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.REJECTED)));
        assertFalse(canEdit(FLOOD_RECORDER_W2, FLOOD, floodW1(IncidentStatus.PENDING)), "not the creator");
        assertFalse(canEdit(FLOOD_SUPERVISOR, FLOOD, floodW1(IncidentStatus.PENDING)), "supervisors review, not edit");
        assertFalse(canEdit(ADMIN, FLOOD, floodW1(IncidentStatus.PENDING)));
    }

    @Test
    void resubmitOnlyAfterCorrectionsRequested() {
        assertTrue(canResubmit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.CORRECTIONS_REQUESTED)));
        assertFalse(canResubmit(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.PENDING)));
    }

    @Test
    void deleteRules() {
        assertTrue(canDelete(ADMIN, FLOOD, floodW1(IncidentStatus.APPROVED)));
        assertTrue(canDelete(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.PENDING)));
        assertFalse(canDelete(FLOOD_RECORDER_W1, FLOOD, floodW1(IncidentStatus.APPROVED)));
        assertFalse(canDelete(FLOOD_SUPERVISOR, FLOOD, floodW1(IncidentStatus.PENDING)));
    }

    @Test
    void listScopes() {
        assertEquals(ListScope.ALL, listScope(FLOOD_SUPERVISOR));
        assertEquals(ListScope.ALL, listScope(ADMIN));
        assertEquals(ListScope.APPROVED_OR_OWN, listScope(FLOOD_RECORDER_W1));
    }

    @Test
    void requireThrowsForbidden() {
        assertThrows(HazardAccessDeniedException.class, () -> HazardAccessPolicy.require(false, "denied"));
    }

    @Test
    @DisplayName("Users cannot be built with an incomplete scope")
    void userScopeValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedUser("x", Role.WARD_RECORDER, FLOOD, null));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedUser("x", Role.WARD_RECORDER, null, "Ward 1"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthenticatedUser("x", Role.PROVINCIAL_SUPERVISOR, null, null));
    }
}
