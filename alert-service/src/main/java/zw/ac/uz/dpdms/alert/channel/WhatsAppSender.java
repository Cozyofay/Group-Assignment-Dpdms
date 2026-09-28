package zw.ac.uz.dpdms.alert.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends WhatsApp messages through the WhatsApp Business Cloud API.
 * The token and phone number id are read from the environment - never hard-coded.
 * When WHATSAPP_ENABLED is false the message is only logged, so the system can be demonstrated
 * without a live WhatsApp account.
 */
@Component
public class WhatsAppSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String apiUrl;
    private final String phoneNumberId;
    private final String accessToken;

    public WhatsAppSender(@Qualifier("externalRestTemplate") RestTemplate restTemplate,
                          @Value("${dpdms.alerts.whatsapp.enabled:false}") boolean enabled,
                          @Value("${dpdms.alerts.whatsapp.api-url:https://graph.facebook.com/v21.0}") String apiUrl,
                          @Value("${dpdms.alerts.whatsapp.phone-number-id:}") String phoneNumberId,
                          @Value("${dpdms.alerts.whatsapp.access-token:}") String accessToken) {
        this.restTemplate = restTemplate;
        this.enabled = enabled;
        this.apiUrl = apiUrl;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    public boolean isEnabled() {
        return enabled && StringUtils.hasText(phoneNumberId) && StringUtils.hasText(accessToken);
    }

    /** Throws RestClientException if the gateway rejects the message; the caller logs it. */
    public void send(String phoneNumber, String text) {
        if (!isEnabled()) {
            log.info("WhatsApp is disabled; message for {} was not sent:\n{}", phoneNumber, text);
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", phoneNumber.replace("+", ""));
        body.put("type", "text");
        body.put("text", Map.of("preview_url", false, "body", text));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        restTemplate.postForEntity(apiUrl + "/" + phoneNumberId + "/messages",
                new HttpEntity<>(body, headers), String.class);
        log.info("WhatsApp alert sent to {}", phoneNumber);
    }
}
