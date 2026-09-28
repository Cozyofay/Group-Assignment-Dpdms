package zw.ac.uz.dpdms.ui.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * The only place the UI talks to the backend. Every call goes through the API gateway and carries
 * the user's JWT from the session, so the services apply exactly the same rules as for any API client.
 */
@Component
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String gatewayUrl;

    public ApiClient(RestTemplate restTemplate, ObjectMapper objectMapper,
                     @Value("${dpdms.gateway-url:http://localhost:8080}") String gatewayUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.gatewayUrl = gatewayUrl;
    }

    public JsonNode get(String path, String token) {
        return exchange(path, HttpMethod.GET, null, token, JsonNode.class);
    }

    public List<Map<String, Object>> getList(String path, String token) {
        HttpEntity<Object> entity = new HttpEntity<>(headers(token));
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    gatewayUrl + path, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw toApiException(ex);
        } catch (ResourceAccessException ex) {
            throw unavailable(path, ex);
        }
    }

    public Map<String, Object> getMap(String path, String token) {
        HttpEntity<Object> entity = new HttpEntity<>(headers(token));
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    gatewayUrl + path, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return response.getBody() == null ? Map.of() : response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw toApiException(ex);
        } catch (ResourceAccessException ex) {
            throw unavailable(path, ex);
        }
    }

    public JsonNode post(String path, Object body, String token) {
        return exchange(path, HttpMethod.POST, body, token, JsonNode.class);
    }

    public JsonNode put(String path, Object body, String token) {
        return exchange(path, HttpMethod.PUT, body, token, JsonNode.class);
    }

    public void delete(String path, String token) {
        exchange(path, HttpMethod.DELETE, null, token, Void.class);
    }

    /** Streams a generated report straight through to the browser. */
    public ResponseEntity<byte[]> download(String path, String token) {
        try {
            return restTemplate.exchange(gatewayUrl + path, HttpMethod.GET,
                    new HttpEntity<>(headers(token)), byte[].class);
        } catch (HttpStatusCodeException ex) {
            throw toApiException(ex);
        } catch (ResourceAccessException ex) {
            throw unavailable(path, ex);
        }
    }

    private <T> T exchange(String path, HttpMethod method, Object body, String token, Class<T> type) {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers(token));
        try {
            return restTemplate.exchange(gatewayUrl + path, method, entity, type).getBody();
        } catch (HttpStatusCodeException ex) {
            throw toApiException(ex);
        } catch (ResourceAccessException ex) {
            throw unavailable(path, ex);
        }
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /** Turns the service's JSON error body into a message the user can actually read. */
    private ApiException toApiException(HttpStatusCodeException ex) {
        String message = ex.getStatusText();
        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            if (body.hasNonNull("message")) {
                message = body.get("message").asText();
            }
            if (body.has("fieldErrors") && body.get("fieldErrors").fieldNames().hasNext()) {
                StringBuilder details = new StringBuilder(message).append(" - ");
                body.get("fieldErrors").fields().forEachRemaining(field ->
                        details.append(field.getKey()).append(" ").append(field.getValue().asText()).append("; "));
                message = details.toString();
            }
        } catch (Exception ignored) {
            log.debug("Error body was not JSON: {}", ex.getResponseBodyAsString());
        }
        return new ApiException(ex.getStatusCode().value(), message);
    }

    private ApiException unavailable(String path, Exception ex) {
        log.warn("Backend call to {} failed: {}", path, ex.getMessage());
        return new ApiException(503, "The service is not reachable right now. Please try again shortly.");
    }
}
