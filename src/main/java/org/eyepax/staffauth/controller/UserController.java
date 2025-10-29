package org.eyepax.staffauth.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        try {
            System.out.println("=== /api/v1/me called ===");

            if (jwt == null) {
                System.err.println("ERROR: JWT is null - token not authenticated");
                return ResponseEntity.status(401).body(Map.of("error", "No authentication token"));
            }

            String cognitoId = jwt.getClaimAsString("sub");
            String email = jwt.getClaimAsString("email");
            String name = jwt.getClaimAsString("name");
            String ipAddress = request.getRemoteAddr();

            System.out.println("Cognito ID: " + cognitoId);
            System.out.println("Email: " + email);
            System.out.println("Name from JWT: " + name);
            System.out.println("IP: " + ipAddress);

            // 1️⃣ Try to find existing user
            AppUser user = userRepository.findByCognitoId(cognitoId).orElse(null);

            if (user == null) {
                // 2️⃣ Create new user - only use JWT name for NEW users
                System.out.println("Creating new user for: " + email);
                user = new AppUser();
                user.setCognitoId(cognitoId);
                user.setEmail(email);
                user.setDisplayName(name != null ? name : email);
                user.setLocale("en-US");
                user = userRepository.save(user);
                System.out.println("User created with ID: " + user.getId());
            } else {
                System.out.println("User found with ID: " + user.getId());
                System.out.println("Current displayName in DB: " + user.getDisplayName());
                
                // 3️⃣ Only update email if changed (NOT displayName - user controls that via PATCH)
                boolean updated = false;
                if (email != null && !Objects.equals(user.getEmail(), email)) {
                    System.out.println("Email changed from " + user.getEmail() + " to " + email);
                    user.setEmail(email);
                    updated = true;
                }
                
                // ✅ DO NOT update displayName from JWT - user manages this themselves
                // The displayName in database takes precedence over JWT name claim
                
                if (updated) {
                    userRepository.save(user);
                    System.out.println("User email updated");
                }
            }

            // 4️⃣ Log login event
            auditService.logLogin(user, ipAddress);

            // 5️⃣ Get roles from JWT
            List<String> cognitoGroups = jwt.getClaimAsStringList("cognito:groups");
            List<String> roles = new ArrayList<>();

            if (cognitoGroups != null) {
                System.out.println("Cognito groups: " + cognitoGroups);
                roles = cognitoGroups;
            } else {
                System.out.println("No cognito:groups claim found in token");
            }

            // 6️⃣ Return user data (use displayName from database, not JWT)
            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "locale", user.getLocale(),
                    "roles", roles
            );

            System.out.println("Returning user data: " + response);
            System.out.println("=== /api/v1/me completed successfully ===");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("ERROR in /api/v1/me: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> updates) {
        try {
            System.out.println("=== PATCH /api/v1/me called ===");
            
            if (jwt == null) {
                return ResponseEntity.status(401).body(Map.of("error", "No authentication token"));
            }

            String cognitoId = jwt.getClaim("sub");
            System.out.println("Updating profile for Cognito ID: " + cognitoId);
            System.out.println("Updates: " + updates);

            AppUser user = userRepository.findByCognitoId(cognitoId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("Current displayName: " + user.getDisplayName());
            System.out.println("Current locale: " + user.getLocale());

            if (updates.containsKey("displayName")) {
                user.setDisplayName(updates.get("displayName"));
                System.out.println("Updated displayName to: " + updates.get("displayName"));
            }
            if (updates.containsKey("locale")) {
                user.setLocale(updates.get("locale"));
                System.out.println("Updated locale to: " + updates.get("locale"));
            }

            user = userRepository.save(user);
            System.out.println("Profile saved successfully");
            
            // Return the complete user object with all fields
            Map<String, Object> response = Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "locale", user.getLocale()
            );
            
            System.out.println("Returning updated user: " + response);
            System.out.println("=== PATCH /api/v1/me completed successfully ===");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("ERROR in PATCH /api/v1/me: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/token-debug")
    public Map<String, Object> debug(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return Map.of("error", "No JWT token found");
        }
        return jwt.getClaims();
    }
}