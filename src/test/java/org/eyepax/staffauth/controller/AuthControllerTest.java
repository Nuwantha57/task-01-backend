package org.eyepax.staffauth.controller;

import org.junit.jupiter.api.Test;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.eyepax.staffauth.config.TestConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = AuthController.class)
@Import(TestConfig.class)
@Epic("Controller Tests")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void testExchangeCodeSuccess() throws Exception {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-access-token");
        tokenResponse.put("id_token", "test-id-token");
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 3600);

        ResponseEntity<Map> response = new ResponseEntity<>(tokenResponse, HttpStatus.OK);
        
        when(restTemplate.postForEntity(
            eq("https://eu-north-1exi0aq7ov.auth.eu-north-1.amazoncognito.com/oauth2/token"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(response);

        mockMvc.perform(post("/auth/token")
                .param("code", "test-auth-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-access-token"))
                .andExpect(jsonPath("$.id_token").value("test-id-token"));
    }

    @Test
    public void testExchangeCodeMissingCode() throws Exception {
        mockMvc.perform(post("/auth/token"))
                .andExpect(status().isBadRequest());
    }
}