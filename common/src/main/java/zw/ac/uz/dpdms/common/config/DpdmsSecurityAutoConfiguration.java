package zw.ac.uz.dpdms.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import zw.ac.uz.dpdms.common.security.HazardScopeFilter;
import zw.ac.uz.dpdms.common.security.InternalApiKeyFilter;
import zw.ac.uz.dpdms.common.security.JwtAuthenticationFilter;
import zw.ac.uz.dpdms.common.security.JwtService;
import zw.ac.uz.dpdms.common.security.SecurityResponses;
import zw.ac.uz.dpdms.common.web.CorrelationIdFilter;
import zw.ac.uz.dpdms.common.web.GlobalExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Applied automatically to every servlet service that depends on "common":
 * stateless JWT authentication, the hazard scope filter, JSON 401/403 responses,
 * the global exception handler and correlation-id logging.
 */
@AutoConfiguration(
        before = SecurityAutoConfiguration.class,
        // Actuator's auto-config would otherwise register its own default chain first and ours would be skipped
        beforeName = "org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableWebSecurity
@EnableConfigurationProperties(DpdmsSecurityProperties.class)
public class DpdmsSecurityAutoConfiguration {

    private static final List<String> DEFAULT_PUBLIC_PATHS = List.of(
            "/actuator/health", "/actuator/health/**", "/actuator/info",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/error",
            // service-to-service endpoints: no user token, protected by InternalApiKeyFilter instead
            "/internal/**");

    @Bean
    @ConditionalOnMissingBean
    public JwtService jwtService(DpdmsSecurityProperties properties) {
        if (!StringUtils.hasText(properties.getJwtSecret())) {
            throw new IllegalStateException(
                    "dpdms.security.jwt-secret is not set. Define JWT_SECRET in the .env file at the project root.");
        }
        return new JwtService(properties.getJwtSecret(), properties.getJwtExpirationMinutes());
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain dpdmsSecurityFilterChain(HttpSecurity http, JwtService jwtService,
                                                        DpdmsSecurityProperties properties,
                                                        ObjectMapper objectMapper) throws Exception {
        List<String> publicPaths = new ArrayList<>(DEFAULT_PUBLIC_PATHS);
        publicPaths.addAll(properties.getPublicPaths());

        // Created with "new" (not as @Beans) so Spring Boot does not ALSO register them as
        // plain servlet filters outside the security chain.
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService);
        HazardScopeFilter scopeFilter = new HazardScopeFilter(
                properties.getServiceHazard(), properties.getReadOnlyExemptPaths(), objectMapper);
        InternalApiKeyFilter internalFilter = new InternalApiKeyFilter(properties.getInternalApiKey(), objectMapper);

        http
                .csrf(AbstractHttpConfigurer::disable)          // stateless API with bearer tokens, no cookies
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> SecurityResponses.write(
                                response, objectMapper, HttpStatus.UNAUTHORIZED,
                                "Authentication required: supply a valid Bearer token", request.getRequestURI()))
                        .accessDeniedHandler((request, response, e) -> SecurityResponses.write(
                                response, objectMapper, HttpStatus.FORBIDDEN,
                                "Access denied", request.getRequestURI())))
                .addFilterBefore(internalFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(scopeFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler dpdmsGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> dpdmsCorrelationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
