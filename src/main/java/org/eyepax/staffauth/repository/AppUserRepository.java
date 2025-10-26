package org.eyepax.staffauth.repository;

import org.eyepax.staffauth.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByCognitoId(String cognitoId);
}