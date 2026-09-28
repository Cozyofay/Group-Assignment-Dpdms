package zw.ac.uz.dpdms.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import zw.ac.uz.dpdms.common.web.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes the standard JSON error body from inside security filters (before controllers run). */
public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status,
                             String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getOutputStream(), ApiError.of(status, message, path));
    }
}
