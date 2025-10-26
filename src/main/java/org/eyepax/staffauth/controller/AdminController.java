package org.eyepax.staffauth.controller;


import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.Role;
import org.eyepax.staffauth.entity.UserRole;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private LoginAuditRepository loginAuditRepository;

    @GetMapping("/users")
    public ResponseEntity<Page<AppUser>> listUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userRepository.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AppUser> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/users/{id}/roles")
    public ResponseEntity<?> assignRoles(@PathVariable Long id, @RequestBody List<String> roleNames) {
        AppUser user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        roleNames.forEach(roleName -> {
            Role role = roleRepository.findByName(roleName);
            if (role == null) {
                role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<List<LoginAudit>> getAuditLog(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String range) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        if (range != null) {
            // Parse range as needed
        }
        return ResponseEntity.ok(loginAuditRepository.findByUserIdAndLoginTimeBetween(userId, start, end));
    }
}