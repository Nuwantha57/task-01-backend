package org.eyepax.staffauth.controller;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(TestConfig.class)
@Epic("Controller Tests")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private AuditService auditService;

    private Jwt jwt;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        // Setup test JWT token
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-cognito-id");
        claims.put("email", "test@example.com");
        claims.put("name", "Test User");

        jwt = new Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            headers,
            claims
        );

        // Setup test user
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setCognitoId("test-cognito-id");
        testUser.setDisplayName("Test User");
    }

    @Test
    public void testGetCurrentUser() throws Exception {
        when(userRepository.findByCognitoId("test-cognito-id"))
            .thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/v1/me")
                .with(authentication(new JwtAuthenticationToken(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    public void testGetCurrentUserNewUser() throws Exception {
        when(userRepository.findByCognitoId("test-cognito-id"))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class)))
            .thenReturn(testUser);

        mockMvc.perform(get("/api/v1/me")
                .with(authentication(new JwtAuthenticationToken(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    public void testUpdateProfile() throws Exception {
        when(userRepository.findByCognitoId("test-cognito-id"))
            .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(AppUser.class)))
            .thenReturn(testUser);

        String updateJson = "{\"displayName\":\"Updated Name\",\"locale\":\"fr-FR\"}";

        mockMvc.perform(patch("/api/v1/me")
                .with(authentication(new JwtAuthenticationToken(jwt)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.locale").value("fr-FR"));
    }

    @Test
    public void testGetTokenDebug() throws Exception {
        mockMvc.perform(get("/api/v1/token-debug")
                .with(authentication(new JwtAuthenticationToken(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("test-cognito-id"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk());
    }
}
