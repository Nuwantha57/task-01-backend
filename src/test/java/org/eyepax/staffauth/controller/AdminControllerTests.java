package org.eyepax.staffauth.controller;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.entity.Role;
import org.eyepax.staffauth.entity.UserRole;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import org.eyepax.staffauth.repository.RoleRepository;
import org.eyepax.staffauth.repository.UserRoleRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@Import(TestConfig.class)
@Epic("Controller Tests")
public class AdminControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private UserRoleRepository userRoleRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private LoginAuditRepository loginAuditRepository;

    private AppUser testUser;
    private Role testRole;
    private UserRole testUserRole;
    private LoginAudit testAudit;

    @BeforeEach
    void setUp() {
        // Mock beans are provided by Spring Boot test slice (@WebMvcTest + @MockBean)

        // Setup test user
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setCognitoId("test-cognito-id");
        testUser.setDisplayName("Test User");

        // Setup test role
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName("ROLE_USER");

        // Setup test user role
        testUserRole = new UserRole();
        testUserRole.setUser(testUser);
        testUserRole.setRole(testRole);

        // Setup test audit
        testAudit = new LoginAudit();
        testAudit.setId(1L);
        testAudit.setUser(testUser);
        testAudit.setLoginTime(LocalDateTime.now());
        testAudit.setEventType("LOGIN");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Users")
    @Severity(SeverityLevel.NORMAL)
    public void testListUsers() throws Exception {
        List<AppUser> users = Arrays.asList(testUser);
        Page<AppUser> userPage = new PageImpl<>(users);
        List<UserRole> userRoles = Arrays.asList(testUserRole);

        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);
        when(userRoleRepository.findAllByUserId(eq(1L))).thenReturn(userRoles);

        mockMvc.perform(get("/api/v1/admin/users")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("ROLE_USER"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Users")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUser() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Users")
    @Severity(SeverityLevel.CRITICAL)
    public void testUpdateUserRoles() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(testUserRole);

        mockMvc.perform(patch("/api/v1/admin/users/1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roleIds\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Users")
    @Severity(SeverityLevel.NORMAL)
    public void testGetUserNotFound() throws Exception {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/admin/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Audit")
    @Severity(SeverityLevel.NORMAL)
    public void testGetAuditLog() throws Exception {
        List<LoginAudit> audits = Arrays.asList(testAudit);
        Page<LoginAudit> auditPage = new PageImpl<>(audits);

        when(loginAuditRepository.findByLoginTimeBetween(any(), any(), any(PageRequest.class)))
            .thenReturn(auditPage);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(1))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("test@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @Feature("Admin - Audit")
    @Severity(SeverityLevel.NORMAL)
    public void testGetAuditLogWithUserId() throws Exception {
        List<LoginAudit> audits = Arrays.asList(testAudit);
        Page<LoginAudit> auditPage = new PageImpl<>(audits);

        when(loginAuditRepository.findByUserIdAndLoginTimeBetween(
            eq(1L), any(), any(), any(PageRequest.class)))
            .thenReturn(auditPage);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                .param("user_id", "1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(1));
    }

    @Test
    @Feature("Admin - Security")
    @Severity(SeverityLevel.MINOR)
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @Feature("Admin - Security")
    @Severity(SeverityLevel.MINOR)
    public void testForbiddenAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }
}