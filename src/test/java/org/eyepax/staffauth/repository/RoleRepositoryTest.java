package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("findByName returns role when present")
    @Description("Persist a Role and verify findByName returns the persisted role")
    @Severity(SeverityLevel.CRITICAL)
    void whenExistingName_thenFindsRole() {
        Role role = new Role();
        role.setName("ADMIN");

        em.persistAndFlush(role);

        Role found = roleRepository.findByName("ADMIN");

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("findByName returns null when not present")
    @Description("Querying a non-existing role name should return null")
    @Severity(SeverityLevel.NORMAL)
    void whenMissingName_thenNull() {
        Role found = roleRepository.findByName("NOPE");

        assertThat(found).isNull();
    }

    @Test
    @DisplayName("findByName with null returns null")
    @Description("Passing null as name should not throw and return null")
    @Severity(SeverityLevel.MINOR)
    void whenNullName_thenNull() {
        Role found = roleRepository.findByName(null);

        assertThat(found).isNull();
    }
}
