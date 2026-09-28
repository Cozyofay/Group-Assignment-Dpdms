package zw.ac.uz.dpdms.dashboard.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.security.CurrentUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Pulls approved incidents from every hazard service the caller is allowed to see.
 * A hazard that is unavailable, or that the caller has no authority over, is simply left out -
 * the dashboard keeps working (graceful degradation).
 */
@Component
public class HazardServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HazardServiceClient.class);

    private final RestTemplate restTemplate;

    public HazardServiceClient(RestTemplate serviceRestTemplate) {
        this.restTemplate = serviceRestTemplate;
    }

    public List<IncidentSummary> approvedIncidents(String ward, String district, Severity severity,
                                                   LocalDateTime from, LocalDateTime to) {
        List<IncidentSummary> all = new ArrayList<>();
        for (HazardType hazard : HazardType.values()) {
            all.addAll(fetch(hazard, ward, district, severity, from, to));
        }
        return all;
    }

    private List<IncidentSummary> fetch(HazardType hazard, String ward, String district, Severity severity,
                                        LocalDateTime from, LocalDateTime to) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("http://" + hazard.getServiceId() + "/api/" + hazard.getApiPath() + "/approved");
        if (ward != null && !ward.isBlank()) {
            uri.queryParam("ward", ward);
        }
        if (district != null && !district.isBlank()) {
            uri.queryParam("district", district);
        }
        if (severity != null) {
            uri.queryParam("severity", severity.name());
        }
        if (from != null) {
            uri.queryParam("from", from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (to != null) {
            uri.queryParam("to", to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        HttpHeaders headers = new HttpHeaders();
        CurrentUser.bearerToken().ifPresent(headers::setBearerAuth);

        try {
            List<IncidentSummary> body = restTemplate.exchange(uri.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(headers), new ParameterizedTypeReference<List<IncidentSummary>>() {
                    }).getBody();
            return body == null ? List.of() : body;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 403) {
                return List.of();   // the caller is scoped to another hazard
            }
            throw ex;
        } catch (RestClientException ex) {
            log.warn("{} is unavailable; the dashboard omits it for now: {}",
                    hazard.getServiceId(), ex.getMessage());
            return List.of();
        }
    }
}
