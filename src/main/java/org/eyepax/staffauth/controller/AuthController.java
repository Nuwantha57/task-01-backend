package org.eyepax.staffauth.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@RestController
public class AuthController {

    private final String clientId = "65ggmh18gfb0rp0878jrtk9rjg";
    private final String clientSecret = "c0jhldbf269g7k5qlnj0iur2uescau8h5vdck6la6s4amdpo6vb";
    private final String redirectUri = "http://localhost:3000";
    private final String tokenEndpoint = "https://eu-north-1exi0aq7ov.auth.eu-north-1.amazoncognito.com/oauth2/token";

    @PostMapping("/auth/token")
    public ResponseEntity<?> exchangeCode(@RequestParam String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        // Request body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // Exchange code for tokens
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenEndpoint, request, Map.class);

        return ResponseEntity.ok(response.getBody());
    }
}
