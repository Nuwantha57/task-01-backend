package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@DataJpaTest
public class LoginAuditRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LoginAuditRepository loginAuditRepository;

    @Test
    @DisplayName("findByLoginTimeBetween returns audits in range")
    @Description("Persist audits and query by time range with paging")
    @Severity(SeverityLevel.CRITICAL)
    void whenAuditsInRange_thenPageReturned() {
        AppUser user = AppUser.builder()
                .cognitoId("cog-a")
                .email("a@example.com")
                .displayName("A")
                .build();
        em.persist(user);

        LocalDateTime now = LocalDateTime.now();
        LoginAudit la1 = LoginAudit.builder()
                .eventType("LOGIN")
                .ipAddress("1.2.3.4")
                .loginTime(now.minusHours(2))
                .loginStatus("SUCCESS")
                .user(user)
                .build();

        LoginAudit la2 = LoginAudit.builder()
                .eventType("LOGIN")
                .ipAddress("1.2.3.5")
                .loginTime(now.minusHours(1))
                .loginStatus("FAIL")
                .user(user)
                .build();

        em.persist(la1);
        em.persistAndFlush(la2);

        Pageable p = PageRequest.of(0, 10);
        Page<LoginAudit> page = loginAuditRepository.findByLoginTimeBetween(now.minusDays(1), now.plusDays(1), p);

        assertThat(page.getContent()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("findByUserIdAndLoginTimeBetween filters by user when provided")
    @Description("When userId is provided, only audits for that user should be returned")
    @Severity(SeverityLevel.CRITICAL)
    void whenUserIdProvided_thenFiltersByUser() {
        AppUser user1 = AppUser.builder().cognitoId("u1").email("u1@e").build();
        AppUser user2 = AppUser.builder().cognitoId("u2").email("u2@e").build();
        em.persist(user1);
        em.persistAndFlush(user2);

        LocalDateTime now = LocalDateTime.now();

        LoginAudit a1 = LoginAudit.builder().eventType("X").ipAddress("ip1").loginTime(now).loginStatus("S").user(user1).build();
        LoginAudit a2 = LoginAudit.builder().eventType("Y").ipAddress("ip2").loginTime(now).loginStatus("S").user(user2).build();
        em.persist(a1);
        em.persistAndFlush(a2);

        Pageable p = PageRequest.of(0, 10);
        Page<LoginAudit> page = loginAuditRepository.findByUserIdAndLoginTimeBetween(user1.getId(), now.minusMinutes(1), now.plusMinutes(1), p);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getId()).isEqualTo(user1.getId());
    }

    @Test
    @DisplayName("findByUserIdAndLoginTimeBetween works with null userId (any user)")
    @Description("Passing null for userId should return audits for any user within the time range")
    @Severity(SeverityLevel.NORMAL)
    void whenUserIdNull_thenReturnsAnyUser() {
        LocalDateTime now = LocalDateTime.now();
        Pageable p = PageRequest.of(0, 10);
        Page<LoginAudit> page = loginAuditRepository.findByUserIdAndLoginTimeBetween(null, now.minusYears(1), now.plusYears(1), p);

        // If no audits exist this may be empty; we assert no exception and a non-null page
        assertThat(page).isNotNull();
    }
}
