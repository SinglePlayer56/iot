package com.viktor.device_service.config;

import com.viktor.device_service.utils.KeycloakUtil;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KeycloakTestTokenProvider {

    private final TestRestTemplate rest;

    public KeycloakTestTokenProvider(TestRestTemplate rest) {
        this.rest = rest;
    }

    public String getAdminToken(String authUrl) {
        return getToken(authUrl, "admin", "admin");
    }

    public String getUserToken(String authUrl) {
        return getToken(authUrl, "user", "user"); // если есть пользователь user
    }

    private String getToken(String authUrl, String username, String password) {
        HttpEntity<?> entity = KeycloakUtil.buildPasswordGrantRequest(username, password);
        Map body = rest.postForObject(authUrl, entity, Map.class);
        return (String) body.get("access_token");
    }
}
