package com.viktor.device_service.it;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.config.KeycloakTestTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MyAuthSecurityIT extends AbstractIntegrationTest {

    @Autowired
    private KeycloakTestTokenProvider tokenProvider;

    @Autowired
    private TestRestTemplate rest;

    private String authUrl() {
        return keycloak.getAuthServerUrl()
               + "/realms/iot/protocol/openid-connect/token";
    }

    private DeviceDto sampleDevice() {
        return new DeviceDto("dev123", "TYPE", System.currentTimeMillis(), "meta");
    }

    @Test
    @DisplayName("given admin when requesting token then token contains ROLE_device-service.admin")
    void givenAdmin_whenGenerateToken_thenAdminRolePresent() throws ParseException {
        String token = tokenProvider.getAdminToken(authUrl());

        JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();
        List<String> roles = (List<String>) claims.getClaim("roles");

        assertNotNull(roles, "roles claim must exist");
        assertTrue(roles.contains("ROLE_device-service.admin"));
    }

    @Test
    @DisplayName("given user when requesting token then token contains ROLE_device-service.user")
    void givenUser_whenGenerateToken_thenUserRolePresent() throws ParseException {
        String token = tokenProvider.getUserToken(authUrl());

        JWTClaimsSet claims = JWTParser.parse(token).getJWTClaimsSet();
        List<String> roles = (List<String>) claims.getClaim("roles");

        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_device-service.user"));
    }

    @Test
    @DisplayName("given admin when POST /devices then request is allowed")
    void givenAdmin_whenPostDevice_thenAllowed() {
        String token = tokenProvider.getAdminToken(authUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> res = rest.exchange(
                "/api/v1/devices",
                HttpMethod.POST,
                new HttpEntity<>(sampleDevice(), headers),
                String.class
        );

        assertTrue(res.getStatusCode().is2xxSuccessful(),
                "Admin must be allowed to POST /api/v1/devices");
    }

    @Test
    @DisplayName("given user when POST /devices then request is forbidden")
    void givenUser_whenPostDevice_thenForbidden() {
        String token = tokenProvider.getUserToken(authUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> res = rest.exchange(
                "/api/v1/devices",
                HttpMethod.POST,
                new HttpEntity<>(sampleDevice(), headers),
                String.class
        );

        assertTrue(res.getStatusCode().is4xxClientError(),
                "User must NOT be allowed to POST /api/v1/devices");
    }

    @Test
    @DisplayName("given user when GET /devices/{id} then request is authenticated and returns 404")
    void givenUser_whenGetUnknownDevice_thenAuthOkAnd404() {
        String token = tokenProvider.getUserToken(authUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> res = rest.exchange(
                "/api/v1/devices/123",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertNotEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode(), "User must not receive 401");
        assertNotEquals(HttpStatus.FORBIDDEN, res.getStatusCode(), "User must not receive 403");
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    @DisplayName("given admin when GET /devices/{id} then request is authenticated and returns 404")
    void givenAdmin_whenGetUnknownDevice_thenAuthOkAnd404() {
        String token = tokenProvider.getAdminToken(authUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> res = rest.exchange(
                "/api/v1/devices/123",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertNotEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode(), "Admin must not receive 401");
        assertNotEquals(HttpStatus.FORBIDDEN, res.getStatusCode(), "Admin must not receive 403");
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }
}