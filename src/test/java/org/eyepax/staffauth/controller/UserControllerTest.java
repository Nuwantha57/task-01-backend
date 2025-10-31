package org.eyepax.staffauth.controller;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class)
@Epic("User API Tests")
@Feature("UserController")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private AuditService auditService;

    private AppUser mockUser;

    @BeforeEach
    public void setup() {
        mockUser = new AppUser();
        mockUser.setId(1L);
        mockUser.setCognitoId("cognito-123");
        mockUser.setEmail("test@example.com");
        mockUser.setDisplayName("Test User");
        mockUser.setLocale("en-US");
    }

    @Test
    @DisplayName("GET /api/v1/me - Returns current user")
    @Severity(SeverityLevel.CRITICAL)
    public void getCurrentUser_UpdatesEmail() throws Exception {
        when(userRepository.findByCognitoId("cognito-123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(mockUser);

        mockMvc.perform(get("/api/v1/me")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "cognito-123");
                    jwt.claim("email", "test@example.com");
                    jwt.claim("name", "Test User");
                    jwt.claim("cognito:groups", List.of("Admin"));
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.roles[0]").value("Admin"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me - Updates user profile")
    @Severity(SeverityLevel.CRITICAL)
    public void updateProfile_ReturnsUpdatedUser() throws Exception {
        when(userRepository.findByCognitoId("cognito-123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(AppUser.class))).thenReturn(mockUser);

        String json = """
                {
                    "displayName": "Updated Name",
                    "locale": "fr-FR"
                }
                """;

        mockMvc.perform(patch("/api/v1/me")
                .with(jwt().jwt(jwt -> jwt.claim("sub", "cognito-123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.locale").value("fr-FR"));
    }

    @Test
    @DisplayName("GET /api/v1/token-debug - Returns JWT claims")
    @Severity(SeverityLevel.NORMAL)
    public void getTokenDebug_ReturnsClaims() throws Exception {
        mockMvc.perform(get("/api/v1/token-debug")
                .with(jwt().jwt(jwt -> {
                    jwt.claim("sub", "cognito-123");
                    jwt.claim("email", "test@example.com");
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("cognito-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
