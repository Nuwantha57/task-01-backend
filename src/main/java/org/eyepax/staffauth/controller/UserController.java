package org.eyepax.staffauth.controller;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        String cognitoId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String ipAddress = request.getRemoteAddr();

        // 1️⃣ Try to find existing user
        AppUser user = userRepository.findByCognitoId(cognitoId).orElse(null);

        if (user == null) {
            // 2️⃣ Create new user
            user = new AppUser();
            user.setCognitoId(cognitoId);
            user.setEmail(email);
            user.setDisplayName(name != null ? name : email);
            user.setLocale("en-US");
            user = userRepository.save(user);
        } else {
            // 3️⃣ Update user info if changed
            boolean updated = false;
            if (!Objects.equals(user.getEmail(), email)) {
                user.setEmail(email);
                updated = true;
            }
            if (name != null && !Objects.equals(user.getDisplayName(), name)) {
                user.setDisplayName(name);
                updated = true;
            }
            if (updated) userRepository.save(user);
        }

        // 4️⃣ Log login event
        auditService.logLogin(user, ipAddress);

        // 5️⃣ Return user data
        List<String> roles = jwt.getClaimAsStringList("cognito:groups");
        if (roles == null) roles = List.of();

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "locale", user.getLocale(),
                "roles", roles
        ));

    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> updates) {
        String cognitoId = jwt.getClaim("sub");

        AppUser user = userRepository.findByCognitoId(cognitoId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("displayName"))
            user.setDisplayName(updates.get("displayName"));
        if (updates.containsKey("locale"))
            user.setLocale(updates.get("locale"));

        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/token-debug")
    public Map<String, Object> debug(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}
