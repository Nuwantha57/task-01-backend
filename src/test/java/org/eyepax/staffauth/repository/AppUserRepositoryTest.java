package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@DataJpaTest
public class AppUserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @DisplayName("findByCognitoId returns user when present")
    @Description("Persist an AppUser and verify findByCognitoId returns it")
    @Severity(SeverityLevel.CRITICAL)
    void whenExistingCognitoId_thenFindsUser() {
        AppUser user = AppUser.builder()
                .cognitoId("cog-123")
                .displayName("Alice")
                .email("alice@example.com")
                .locale("en")
                .build();

        em.persistAndFlush(user);

        Optional<AppUser> found = appUserRepository.findByCognitoId("cog-123");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("findByCognitoId returns empty when not found")
    @Description("Query with a non-existing cognito id should return empty Optional")
    @Severity(SeverityLevel.NORMAL)
    void whenMissingCognitoId_thenEmptyOptional() {
        Optional<AppUser> found = appUserRepository.findByCognitoId("no-such-id");
        assertThat(found).isNotPresent();
    }

    @Test
    @DisplayName("findByCognitoId with empty string returns empty")
    @Description("Edge case: empty string should not match persisted user")
    @Severity(SeverityLevel.MINOR)
    void whenEmptyCognitoId_thenEmpty() {
        Optional<AppUser> found = appUserRepository.findByCognitoId("");
        assertThat(found).isNotPresent();
    }
}
