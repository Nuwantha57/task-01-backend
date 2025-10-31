package org.eyepax.staffauth.controller;

import org.eyepax.staffauth.config.TestSecurityConfig;
import org.eyepax.staffauth.dto.ApiResponse;
import org.eyepax.staffauth.dto.UpdateUserRolesRequest;
import org.eyepax.staffauth.entity.*;
import org.eyepax.staffauth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AppUserRepository userRepository;
    @MockBean private UserRoleRepository userRoleRepository;
    @MockBean private RoleRepository roleRepository;
    @MockBean private LoginAuditRepository loginAuditRepository;

    private AppUser testUser;
    private Role testRole;
    private UserRole testUserRole;
    private LoginAudit testAudit;

    @BeforeEach
    void setup() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setDisplayName("Test User");
        testUser.setLocale("en-US");

        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("Admin");

        testUserRole = new UserRole();
        testUserRole.setId(1L);
        testUserRole.setUser(testUser);
        testUserRole.setRole(testRole);

        testAudit = new LoginAudit();
        testAudit.setId(1L);
        testAudit.setUser(testUser);
        testAudit.setEventType("LOGIN");
        testAudit.setLoginTime(LocalDateTime.now());
    }

    @Test
    void testListUsers() throws Exception {
        Page<AppUser> usersPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(usersPage);
        when(userRoleRepository.findAllByUserId(1L)).thenReturn(List.of(testUserRole));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("Admin"));
    }

    @Test
    void testGetUserByIdFound() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserRolesSuccess() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoleRepository.findAllByUserId(1L)).thenReturn(new ArrayList<>());
        when(roleRepository.findByName("Admin")).thenReturn(testRole);
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        String requestJson = "{\"roles\": [\"Admin\"]}";

        mockMvc.perform(patch("/api/v1/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void testUpdateUserRolesUserNotFound() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        String requestJson = "{\"roles\": [\"Admin\"]}";

        mockMvc.perform(patch("/api/v1/admin/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAuditLog() throws Exception {
        Page<LoginAudit> auditPage = new PageImpl<>(List.of(testAudit));
        when(loginAuditRepository.findByLoginTimeBetween(any(), any(), any(PageRequest.class)))
                .thenReturn(auditPage);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .param("range", "2024-10-01_to_2024-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("test@example.com"));
    }
}
