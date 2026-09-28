package zw.ac.uz.dpdms.ui.model;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import java.io.Serializable;

/** What the UI keeps in the session after login. The token never reaches the browser. */
public record SessionUser(String token, String username, String fullName, Role role,
                          HazardType hazard, String ward) implements Serializable {

    public static final String SESSION_KEY = "dpdmsUser";

    public boolean isRecorder() {
        return role == Role.WARD_RECORDER;
    }

    public boolean isSupervisor() {
        return role == Role.PROVINCIAL_SUPERVISOR;
    }

    public boolean isAdmin() {
        return role == Role.PROVINCIAL_ADMIN;
    }

    public boolean isNational() {
        return role == Role.NATIONAL_VIEWER;
    }

    /** Read-only users may not see capture or approval screens. */
    public boolean canWrite() {
        return !isNational();
    }

    public String roleLabel() {
        return switch (role) {
            case WARD_RECORDER -> "Ward recorder - " + hazard.getLabel() + ", " + ward;
            case PROVINCIAL_SUPERVISOR -> "Provincial supervisor - " + hazard.getLabel();
            case PROVINCIAL_ADMIN -> "Provincial administrator";
            case NATIONAL_VIEWER -> "National viewer (read-only)";
        };
    }
}
