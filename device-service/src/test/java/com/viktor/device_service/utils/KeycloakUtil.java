package com.viktor.device_service.utils;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class KeycloakUtil {

    public static HttpEntity<?> buildPasswordGrantRequest(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "device-service");
        form.add("client_secret", "test-secret");
        form.add("username", username);
        form.add("password", password);
        return new HttpEntity<>(form, headers);
    }
}
