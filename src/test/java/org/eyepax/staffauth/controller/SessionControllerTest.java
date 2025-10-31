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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionController.class)
@Import(TestConfig.class)
@Epic("Controller Tests")
public class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    public void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/logout"))
                .andExpect(status().isOk());
    }

    @Test
    public void testLogoutUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/logout"))
                .andExpect(status().isUnauthorized());
    }
}