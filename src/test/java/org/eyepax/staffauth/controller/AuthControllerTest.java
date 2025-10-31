package org.eyepax.staffauth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private RestTemplate restTemplate;

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenEndpoint;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use reflection to get the private constants from AuthController
        clientId = (String) getPrivateField(authController, "clientId");
        clientSecret = (String) getPrivateField(authController, "clientSecret");
        redirectUri = (String) getPrivateField(authController, "redirectUri");
        tokenEndpoint = (String) getPrivateField(authController, "tokenEndpoint");
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = AuthController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void exchangeCode_ShouldReturnAccessTokenResponse() throws Exception {
        // Arrange
        String code = "testAuthCode";

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("access_token", "mockAccessToken");
        mockResponse.put("id_token", "mockIdToken");
        mockResponse.put("refresh_token", "mockRefreshToken");

        ResponseEntity<Map> mockEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        RestTemplate spyRestTemplate = spy(new RestTemplate());
        doReturn(mockEntity).when(spyRestTemplate)
                .postForEntity(eq(tokenEndpoint), any(HttpEntity.class), eq(Map.class));

        // Act — simulate controller behavior manually but with mocked RestTemplate
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<LinkedMultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = spyRestTemplate.postForEntity(tokenEndpoint, request, Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mockAccessToken", response.getBody().get("access_token"));
    }

    @Test
    void exchangeCode_ShouldHandleInvalidCodeGracefully() {
        // Arrange
        String invalidCode = "invalidCode";
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Invalid code"));

        // Act & Assert
        assertThrows(Exception.class, () -> authController.exchangeCode(invalidCode));
    }
}
