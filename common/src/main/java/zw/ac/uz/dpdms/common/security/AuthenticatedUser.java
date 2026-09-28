package zw.ac.uz.dpdms.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import java.util.List;
import java.util.Objects;

/**
 * The caller, as decoded from the signed JWT. A recorder always has a hazard AND a ward;
 * a supervisor always has a hazard; admin and national users have neither.
 */
public record AuthenticatedUser(String username, Role role, HazardType hazard, String ward) {

    public AuthenticatedUser {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
        if (role.requiresHazard() && hazard == null) {
            throw new IllegalArgumentException(role + " must be assigned exactly one hazard");
        }
        if (role.requiresWard() && (ward == null || ward.isBlank())) {
            throw new IllegalArgumentException(role + " must be assigned a ward");
        }
        if (!role.requiresHazard()) {
            hazard = null;
        }
        if (!role.requiresWard()) {
            ward = null;
        }
    }

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
