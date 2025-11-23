package com.viktor.device_service.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/devices").hasAnyAuthority("ROLE_device-service.admin")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/devices/{deviceId}").hasAnyAuthority("ROLE_device-service.admin")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/devices/{deviceId}").hasAnyAuthority("ROLE_device-service.admin")
                        .requestMatchers(HttpMethod.GET, "/api/v1/devices/{deviceId}").hasAnyAuthority("ROLE_device-service.user", "ROLE_device-service.admin")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthenticationConverter());
        return converter;
    }
}
