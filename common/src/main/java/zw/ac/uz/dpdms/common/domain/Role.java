package zw.ac.uz.dpdms.common.domain;

/**
 * DPDMS roles.
 * <ul>
 *   <li>WARD_RECORDER - captures incidents for exactly one (ward, hazard) pair.</li>
 *   <li>PROVINCIAL_SUPERVISOR - approves/rejects/requests corrections for exactly one hazard.</li>
 *   <li>PROVINCIAL_ADMIN - manages users; may see pending records within each hazard.</li>
 *   <li>NATIONAL_VIEWER - read-only across all five hazards; every write is 403.</li>
 * </ul>
 */
public enum Role {
    WARD_RECORDER(true, true),
    PROVINCIAL_SUPERVISOR(true, false),
    PROVINCIAL_ADMIN(false, false),
    NATIONAL_VIEWER(false, false);

    private final boolean requiresHazard;
    private final boolean requiresWard;

    Role(boolean requiresHazard, boolean requiresWard) {
        this.requiresHazard = requiresHazard;
        this.requiresWard = requiresWard;
    }

    public boolean requiresHazard() {
        return requiresHazard;
    }

    public boolean requiresWard() {
        return requiresWard;
    }
}
