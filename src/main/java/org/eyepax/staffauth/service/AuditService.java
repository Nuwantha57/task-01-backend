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
        LoginAudit audit = new LoginAudit();
        audit.setUser(user);
        audit.setEventType("LOGIN");
        audit.setIpAddress(ipAddress);
        audit.setLoginTime(LocalDateTime.now());

        loginAuditRepository.save(audit);
    }

    // NEW METHOD: Log role changes
    public void logRoleChange(AppUser user, List<String> oldRoles, List<String> newRoles, String ipAddress) {
        String oldRolesStr = oldRoles != null ? String.join(", ", oldRoles) : "None";
        String newRolesStr = newRoles != null ? String.join(", ", newRoles) : "None";
        
        LoginAudit audit = new LoginAudit();
        audit.setUser(user);
        audit.setEventType("ROLE_CHANGE");
        audit.setIpAddress(ipAddress);
        audit.setLoginTime(LocalDateTime.now());
        audit.setLoginStatus(String.format("Roles changed from [%s] to [%s]", oldRolesStr, newRolesStr));

        loginAuditRepository.save(audit);
    }
}