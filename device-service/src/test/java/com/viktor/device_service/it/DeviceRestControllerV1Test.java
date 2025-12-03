package com.viktor.device_service.it;

import com.viktor.device_service.config.KeycloakTestTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DeviceRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    KeycloakTestTokenProvider tokens;

    @Test
    @DisplayName("GET /devices/{id} with valid token but unknown device returns 404 (auth succeeded)")
    void givenValidUserToken_whenRequestingUnknownDevice_thenReturn404() {

        String authUrl = keycloak.getAuthServerUrl()
                         + "/realms/iot/protocol/openid-connect/token";

        String token = tokens.getUserToken(authUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/devices/unknown",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assert response.getStatusCode().value() == 404;
    }
}
