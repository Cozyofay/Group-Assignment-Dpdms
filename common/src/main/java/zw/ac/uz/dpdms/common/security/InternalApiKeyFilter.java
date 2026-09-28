package zw.ac.uz.dpdms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects /internal/** endpoints used for service-to-service calls (for example the alert-service
 * asking the auth-service who should receive an alert). The caller must send the shared
 * INTERNAL_API_KEY in the X-Internal-Api-Key header. The gateway never routes /internal/**,
 * so these endpoints are unreachable from outside.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    public static final String PATH_PREFIX = "/internal/";

    private final byte[] expectedKey;
    private final ObjectMapper objectMapper;

    public InternalApiKeyFilter(String internalApiKey, ObjectMapper objectMapper) {
        this.expectedKey = StringUtils.hasText(internalApiKey)
                ? internalApiKey.getBytes(StandardCharsets.UTF_8)
                : null;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        boolean valid = expectedKey != null
                && provided != null
                // constant-time comparison prevents timing attacks on the key
                && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            SecurityResponses.write(response, objectMapper, HttpStatus.UNAUTHORIZED,
                    "Invalid or missing internal API key", request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }
}
