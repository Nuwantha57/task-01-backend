package org.eyepax.staffauth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final LoginAuditRepository loginAuditRepository;

    public AuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    public void logLogin(AppUser user, String ipAddress) {
        LoginAudit audit = LoginAudit.builder()
                .user(user)
                .eventType("LOGIN")
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .build();

        loginAuditRepository.save(audit);
    }

    // NEW METHOD: Log role changes
    public void logRoleChange(AppUser user, List<String> oldRoles, List<String> newRoles, String ipAddress) {
        String oldRolesStr = oldRoles != null ? String.join(", ", oldRoles) : "None";
        String newRolesStr = newRoles != null ? String.join(", ", newRoles) : "None";
        
        LoginAudit audit = LoginAudit.builder()
                .user(user)
                .eventType("ROLE_CHANGE")
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .loginStatus(String.format("Roles changed from [%s] to [%s]", oldRolesStr, newRolesStr))
                .build();

        loginAuditRepository.save(audit);
    }
}