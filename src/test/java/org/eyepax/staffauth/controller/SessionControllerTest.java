package org.eyepax.staffauth.controller;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock OAuth2 beans if your project includes security auto-configuration
    @MockBean
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @WithMockUser // ✅ Authenticated mock user
    @DisplayName("Logout endpoint returns 200 OK")
    @Description("Ensures /api/v1/sessions/logout endpoint responds with HTTP 200 and empty body")
    @Severity(SeverityLevel.NORMAL)
    void logout_ReturnsOkResponse() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/logout")
                .with(csrf()) // ✅ Fix: add CSRF token
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("")); // Expect empty response body
    }
}
