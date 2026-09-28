package zw.ac.uz.dpdms.alert.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.dto.AlertRecipient;
import zw.ac.uz.dpdms.common.security.InternalApiKeyFilter;

import java.util.List;

/**
 * Asks the auth-service who should be told about an incident: that hazard's supervisor, that
 * hazard's recorders in the affected ward, the provincial admin and national users.
 * This is a service-to-service call, authenticated with the shared internal API key.
 */
@Component
public class RecipientLookup {

    private static final Logger log = LoggerFactory.getLogger(RecipientLookup.class);

    private final RestTemplate restTemplate;
    private final String internalApiKey;

    public RecipientLookup(@Qualifier("serviceRestTemplate") RestTemplate restTemplate,
                           @Value("${dpdms.security.internal-api-key:}") String internalApiKey) {
        this.restTemplate = restTemplate;
        this.internalApiKey = internalApiKey;
    }

    public List<AlertRecipient> recipientsFor(HazardType hazard, String ward) {
        String url = UriComponentsBuilder.fromUriString("http://auth-service/internal/users/alert-recipients")
                .queryParam("hazard", hazard.name())
                .queryParam("ward", ward)
                .toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.set(InternalApiKeyFilter.HEADER, internalApiKey);
        try {
            ResponseEntity<List<AlertRecipient>> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<AlertRecipient>>() {
                    });
            return response.getBody() == null ? List.of() : response.getBody();
        } catch (RestClientException ex) {
            log.error("Could not load alert recipients from auth-service: {}", ex.getMessage());
            throw ex;   // let the message be retried, then dead-lettered
        }
    }
}
