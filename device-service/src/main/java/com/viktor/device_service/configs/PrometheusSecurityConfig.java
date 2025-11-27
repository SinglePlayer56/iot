package com.viktor.device_service.configs;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class PrometheusSecurityConfig {

    @Value("${monitoring.token}")
    private String monitoringToken;

    @Bean
    @Order(1)
    public SecurityFilterChain prometheusTokenSecurity(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/actuator/prometheus")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().access((_, context) -> {
                            HttpServletRequest request = context.getRequest();
                            String token = request.getHeader("X-Monitoring-Token");

                            boolean allowed = token != null && token.equals(monitoringToken);
                            return new AuthorizationDecision(allowed);
                        })
                )
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .oauth2ResourceServer(oauth -> oauth.disable());

        return http.build();
    }
}
