package zw.ac.uz.dpdms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Role;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Service-level guard that runs on EVERY request, before any controller:
 * <ol>
 *   <li>A NATIONAL_VIEWER may only use read methods (GET/HEAD/OPTIONS); anything else is 403.</li>
 *   <li>If this service manages a hazard (dpdms.security.service-hazard), recorders and supervisors
 *       of a different hazard get 403. A flood supervisor calling the drought-service is refused
 *       by the drought-service itself, whatever the front end shows.</li>
 * </ol>
 * Record-level rules (ward, ownership, status) are checked afterwards in the service layer
 * using HazardAccessPolicy.
 */
public class HazardScopeFilter extends OncePerRequestFilter {

    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final HazardType serviceHazard;
    private final List<String> readOnlyExemptPaths;
    private final ObjectMapper objectMapper;

    public HazardScopeFilter(HazardType serviceHazard, List<String> readOnlyExemptPaths, ObjectMapper objectMapper) {
        this.serviceHazard = serviceHazard;
        this.readOnlyExemptPaths = readOnlyExemptPaths == null ? List.of() : List.copyOf(readOnlyExemptPaths);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {

            if (user.role() == Role.NATIONAL_VIEWER
                    && !READ_METHODS.contains(request.getMethod())
                    && !isExempt(request.getRequestURI())) {
                SecurityResponses.write(response, objectMapper, HttpStatus.FORBIDDEN,
                        "National users have read-only access; write operations are not permitted",
                        request.getRequestURI());
                return;
            }

            if (serviceHazard != null && !HazardAccessPolicy.canAccessHazard(user, serviceHazard)) {
                SecurityResponses.write(response, objectMapper, HttpStatus.FORBIDDEN,
                        "Your account is scoped to " + user.hazard().getLabel()
                                + " records and cannot access the " + serviceHazard.getLabel() + " service",
                        request.getRequestURI());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isExempt(String path) {
        return readOnlyExemptPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}
