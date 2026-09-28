package zw.ac.uz.dpdms.common.security;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Role;
import zw.ac.uz.dpdms.common.domain.ScopedIncident;
import zw.ac.uz.dpdms.common.exception.HazardAccessDeniedException;
import zw.ac.uz.dpdms.common.workflow.ApprovalWorkflow;

/**
 * THE access-control rules of DPDMS, in one place, used by every hazard service.
 * Pure functions with no Spring dependencies so they are easy to unit test.
 *
 * <ul>
 *   <li>Ward recorder: authorised for exactly one (ward, hazard) pair.</li>
 *   <li>Provincial supervisor: approves / rejects / requests corrections for exactly one hazard,
 *       and cannot even view other hazards.</li>
 *   <li>National viewer: may read all hazards, may never write.</li>
 *   <li>Non-approved records: visible only to their creator, that hazard's supervisor and the
 *       provincial admin.</li>
 * </ul>
 */
public final class HazardAccessPolicy {

    private HazardAccessPolicy() {
    }

    /** May this user call the given hazard's service at all? */
    public static boolean canAccessHazard(AuthenticatedUser user, HazardType hazard) {
        return switch (user.role()) {
            case NATIONAL_VIEWER, PROVINCIAL_ADMIN -> true;
            case WARD_RECORDER, PROVINCIAL_SUPERVISOR -> user.hazard() == hazard;
        };
    }

    /** Only a recorder of this hazard, in this ward, may capture a new record. */
    public static boolean canCreate(AuthenticatedUser user, HazardType hazard, String ward) {
        return user.role() == Role.WARD_RECORDER
                && user.hazard() == hazard
                && sameWard(user.ward(), ward);
    }

    public static boolean canView(AuthenticatedUser user, HazardType hazard, ScopedIncident incident) {
        if (!canAccessHazard(user, hazard)) {
            return false;
        }
        if (incident.getStatus() == IncidentStatus.APPROVED) {
            return true;
        }
        return switch (user.role()) {
            case PROVINCIAL_ADMIN, PROVINCIAL_SUPERVISOR -> true; // supervisor already hazard-matched above
            case WARD_RECORDER -> user.username().equals(incident.getReporterUsername());
            case NATIONAL_VIEWER -> false;
        };
    }

    /** The creator may edit their own record while it is PENDING or CORRECTIONS_REQUESTED. */
    public static boolean canEdit(AuthenticatedUser user, HazardType hazard, ScopedIncident incident) {
        return user.role() == Role.WARD_RECORDER
                && user.hazard() == hazard
                && user.username().equals(incident.getReporterUsername())
                && sameWard(user.ward(), incident.getWard())
                && ApprovalWorkflow.isEditable(incident.getStatus());
    }

    public static boolean canResubmit(AuthenticatedUser user, HazardType hazard, ScopedIncident incident) {
        return canEdit(user, hazard, incident) && incident.getStatus() == IncidentStatus.CORRECTIONS_REQUESTED;
    }

    /** The creator (while editable) or the provincial admin may delete. */
    public static boolean canDelete(AuthenticatedUser user, HazardType hazard, ScopedIncident incident) {
        if (user.role() == Role.PROVINCIAL_ADMIN) {
            return true;
        }
        return canEdit(user, hazard, incident);
    }

    /** Only the supervisor of THIS hazard may approve, reject or request corrections. */
    public static boolean canReview(AuthenticatedUser user, HazardType hazard) {
        return user.role() == Role.PROVINCIAL_SUPERVISOR && user.hazard() == hazard;
    }

    public static ListScope listScope(AuthenticatedUser user) {
        return switch (user.role()) {
            case PROVINCIAL_ADMIN, PROVINCIAL_SUPERVISOR -> ListScope.ALL;
            case WARD_RECORDER -> ListScope.APPROVED_OR_OWN;
            case NATIONAL_VIEWER -> ListScope.APPROVED_ONLY;
        };
    }

    /** Throws HazardAccessDeniedException (-> 403) when a check fails. */
    public static void require(boolean allowed, String message) {
        if (!allowed) {
            throw new HazardAccessDeniedException(message);
        }
    }

    private static boolean sameWard(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
