package org.eyepax.staffauth.repository;

import java.time.LocalDateTime;

import org.eyepax.staffauth.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {

    @Query("SELECT la FROM LoginAudit la JOIN FETCH la.user WHERE " +
            "(:userId IS NULL OR la.user.id = :userId) AND " +
            "la.loginTime >= :start AND la.loginTime <= :end")
    Page<LoginAudit> findByUserIdAndLoginTimeBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("SELECT la FROM LoginAudit la JOIN FETCH la.user WHERE " +
            "la.loginTime >= :start AND la.loginTime <= :end")
    Page<LoginAudit> findByLoginTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );
}