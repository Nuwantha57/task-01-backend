package org.eyepax.staffauth.service;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {

    private final LoginAuditRepository loginAuditRepository;

    public AuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    public void logLogin(AppUser user, String ipAddress) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (ipAddress == null) {
            throw new IllegalArgumentException("IP address cannot be null");
        }
        
        LoginAudit audit = LoginAudit.builder()
                .user(user)
                .eventType("LOGIN")
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .build();

        loginAuditRepository.save(audit);
    }
}
