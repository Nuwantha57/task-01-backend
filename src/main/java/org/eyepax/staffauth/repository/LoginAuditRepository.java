package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
    List<LoginAudit> findByUserIdAndLoginTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
