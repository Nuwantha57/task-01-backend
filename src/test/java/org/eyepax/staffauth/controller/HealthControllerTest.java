package org.eyepax.staffauth.controller;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ✅ Loads only the HealthController
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock any security filters (so no real login redirect)
    @MockBean
    private org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Test
    @WithMockUser // ✅ Simulates a logged-in user (bypasses 302 redirect)
    @DisplayName("Health check endpoint returns OK")
    @Description("Ensures /healthz endpoint responds with status 200 and body 'OK'")
    @Severity(SeverityLevel.CRITICAL)
    void healthCheck_ReturnsOkResponse() throws Exception {
        mockMvc.perform(get("/healthz")
                .contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}
