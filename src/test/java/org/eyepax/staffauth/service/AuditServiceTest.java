package org.eyepax.staffauth.service;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private LoginAuditRepository loginAuditRepository;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<LoginAudit> auditCaptor;

    @Test
    @DisplayName("logLogin creates and saves audit entry with correct data")
    @Description("Verify that logLogin correctly creates a LoginAudit with user, IP, and current timestamp")
    @Severity(SeverityLevel.CRITICAL)
    void logLogin_ShouldCreateAuditWithCorrectData() {
        AppUser user = AppUser.builder()
                .id(1L)
                .cognitoId("cog123")
                .email("test@example.com")
                .build();
        String ipAddress = "192.168.1.1";

        auditService.logLogin(user, ipAddress);

        verify(loginAuditRepository).save(auditCaptor.capture());
        LoginAudit savedAudit = auditCaptor.getValue();

        assertThat(savedAudit.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedAudit.getUser().getCognitoId()).isEqualTo(user.getCognitoId());
        assertThat(savedAudit.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedAudit.getEventType()).isEqualTo("LOGIN");
        assertThat(savedAudit.getLoginTime()).isNotNull();
    }

    @Test
    @DisplayName("logLogin with null user throws IllegalArgumentException")
    @Description("Verify that logLogin validates user parameter and throws appropriate exception")
    @Severity(SeverityLevel.NORMAL)
    void logLogin_WithNullUser_ShouldThrowException() {
        String ipAddress = "192.168.1.1";

        assertThatThrownBy(() -> auditService.logLogin(null, ipAddress))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User cannot be null");

        verifyNoInteractions(loginAuditRepository);
    }

    @Test
    @DisplayName("logLogin with null IP throws IllegalArgumentException")
    @Description("Verify that logLogin validates ipAddress parameter and throws appropriate exception")
    @Severity(SeverityLevel.NORMAL)
    void logLogin_WithNullIpAddress_ShouldThrowException() {
        AppUser user = AppUser.builder().id(1L).cognitoId("cog123").build();

        assertThatThrownBy(() -> auditService.logLogin(user, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("IP address cannot be null");

        verifyNoInteractions(loginAuditRepository);
    }

    @Test
    @DisplayName("logLogin handles repository exception")
    @Description("Verify that logLogin propagates repository exceptions")
    @Severity(SeverityLevel.BLOCKER)
    void logLogin_WhenRepositoryThrows_ShouldPropagateException() {
        AppUser user = AppUser.builder()
                .id(1L)
                .cognitoId("cog123")
                .build();
        String ipAddress = "192.168.1.1";

        when(loginAuditRepository.save(any())).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> auditService.logLogin(user, ipAddress))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("DB Error");
    }
}
