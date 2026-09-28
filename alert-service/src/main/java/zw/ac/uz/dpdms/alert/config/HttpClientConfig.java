package zw.ac.uz.dpdms.alert.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {

    /** Calls other DPDMS services by name through Eureka, e.g. http://auth-service/internal/... */
    @Bean
    @LoadBalanced
    public RestTemplate serviceRestTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(10)).build();
    }

    /** Plain client for the external WhatsApp gateway (no service discovery involved). */
    @Bean
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(15)).build();
    }
}
