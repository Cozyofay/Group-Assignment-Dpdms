package zw.ac.uz.dpdms.report.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.security.CurrentUser;
import zw.ac.uz.dpdms.report.dto.ReportFilter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collects approved incidents from the five hazard services. The caller's own token is forwarded,
 * so hazard scoping still applies: if a flood supervisor asks for "all hazards", the drought
 * service refuses and that hazard is simply left out of their report.
 */
@Component
public class HazardServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HazardServiceClient.class);

    private final RestTemplate restTemplate;

    public HazardServiceClient(RestTemplate serviceRestTemplate) {
        this.restTemplate = serviceRestTemplate;
    }

    public List<IncidentSummary> collect(ReportFilter filter) {
        List<IncidentSummary> all = new ArrayList<>();
        for (HazardType hazard : filter.hazardsOrAll()) {
            all.addAll(fetch(hazard, filter));
        }
        all.sort(Comparator.comparing(IncidentSummary::occurredAt).reversed());
        return all;
    }

    private List<IncidentSummary> fetch(HazardType hazard, ReportFilter filter) {
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString("http://" + hazard.getServiceId() + "/api/" + hazard.getApiPath() + "/approved");
        if (filter.ward() != null && !filter.ward().isBlank()) {
            uri.queryParam("ward", filter.ward());
        }
        if (filter.district() != null && !filter.district().isBlank()) {
            uri.queryParam("district", filter.district());
        }
        if (filter.severity() != null) {
            uri.queryParam("severity", filter.severity().name());
        }
        if (filter.from() != null) {
            uri.queryParam("from", filter.from().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (filter.to() != null) {
            uri.queryParam("to", filter.to().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        HttpHeaders headers = new HttpHeaders();
        CurrentUser.bearerToken().ifPresent(token -> headers.setBearerAuth(token));

        try {
            List<IncidentSummary> body = restTemplate.exchange(uri.toUriString(), HttpMethod.GET,
                    new HttpEntity<>(headers), new ParameterizedTypeReference<List<IncidentSummary>>() {
                    }).getBody();
            return body == null ? List.of() : body;
        } catch (HttpClientErrorException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 403) {
                log.info("Caller is not authorised for {}; that hazard is omitted from the report", hazard);
                return List.of();
            }
            throw ex;
        } catch (RestClientException ex) {
            // Reliability: one unavailable hazard service must not fail the whole report
            log.warn("{} is unavailable, so it is omitted from the report: {}", hazard.getServiceId(), ex.getMessage());
            return List.of();
        }
    }
}
