package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
}
