package zw.ac.uz.dpdms.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * OpenAPI / Swagger description for each service, with an "Authorize" button for the JWT
 * and the gateway as the server, so "Try it out" goes through the gateway.
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
public class DpdmsOpenApiAutoConfiguration {

    private static final String BEARER = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI dpdmsOpenApi(@Value("${spring.application.name:dpdms-service}") String serviceName,
                                @Value("${dpdms.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("DPDMS - " + serviceName)
                        .version("1.0.0")
                        .description("Rushinga Provincial Disaster Monitoring and Management System. "
                                + "Log in via POST /api/auth/login, then click Authorize and paste the token."))
                .servers(List.of(new Server().url(gatewayUrl).description("API gateway")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .name(BEARER)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
