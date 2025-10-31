package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.Role;
import org.eyepax.staffauth.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@DataJpaTest
public class UserRoleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("findAllByUserId returns roles for a user")
    @Description("Persist AppUser, Role and UserRole, then verify repository finder")
    @Severity(SeverityLevel.CRITICAL)
    void whenUserHasRoles_thenFindsThem() {
        AppUser user = AppUser.builder()
                .cognitoId("cog-1")
                .email("u1@example.com")
                .displayName("U1")
                .build();
        em.persist(user);

        Role role = new Role();
        role.setName("USER");
        em.persist(role);

        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        em.persistAndFlush(ur);

        List<UserRole> roles = userRoleRepository.findAllByUserId(user.getId());
        assertThat(roles).isNotEmpty();
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("USER");
    }

    @Test
    @DisplayName("findAllByUserId returns empty list when none")
    @Description("Query for user without roles should return empty list")
    @Severity(SeverityLevel.NORMAL)
    void whenNoRoles_thenEmptyList() {
        List<UserRole> roles = userRoleRepository.findAllByUserId(9999L);
        assertThat(roles).isEmpty();
    }
}
