package zw.ac.uz.dpdms.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import zw.ac.uz.dpdms.common.domain.HazardType;

import java.util.ArrayList;
import java.util.List;

/**
 * dpdms.security.* settings, e.g. in flood-service:
 * <pre>
 * dpdms:
 *   security:
 *     jwt-secret: ${JWT_SECRET}
 *     service-hazard: FLOOD
 * </pre>
 */
@ConfigurationProperties(prefix = "dpdms.security")
public class DpdmsSecurityProperties {

    /** HMAC secret shared by all services (from the JWT_SECRET environment variable). */
    private String jwtSecret;

    /** Token lifetime in minutes. */
    private long jwtExpirationMinutes = 480;

    /** The hazard this service manages; null for non-hazard services (auth, report, alert, dashboard). */
    private HazardType serviceHazard;

    /** Extra paths that need no token, e.g. /api/auth/login. */
    private List<String> publicPaths = new ArrayList<>();

    /**
     * Paths a NATIONAL_VIEWER may call with a write method even though the role is read-only,
     * e.g. /api/auth/change-password (changing your own password is not modifying hazard data).
     */
    private List<String> readOnlyExemptPaths = new ArrayList<>();

    /** Shared key for service-to-service calls on /internal/** (from INTERNAL_API_KEY). */
    private String internalApiKey;

    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }

    public long getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public void setJwtExpirationMinutes(long jwtExpirationMinutes) { this.jwtExpirationMinutes = jwtExpirationMinutes; }

    public HazardType getServiceHazard() { return serviceHazard; }
    public void setServiceHazard(HazardType serviceHazard) { this.serviceHazard = serviceHazard; }

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public List<String> getReadOnlyExemptPaths() { return readOnlyExemptPaths; }
    public void setReadOnlyExemptPaths(List<String> readOnlyExemptPaths) { this.readOnlyExemptPaths = readOnlyExemptPaths; }

    public String getInternalApiKey() { return internalApiKey; }
    public void setInternalApiKey(String internalApiKey) { this.internalApiKey = internalApiKey; }
}
