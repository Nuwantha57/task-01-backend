package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    @Query("SELECT la FROM LoginAudit la WHERE " +
            "(:userId IS NULL OR la.user.id = :userId) AND " +
            "la.loginTime BETWEEN :start AND :end")
    List<LoginAudit> findByUserIdAndLoginTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}