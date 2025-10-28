package org.eyepax.staffauth.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eyepax.staffauth.dto.ApiResponse;
import org.eyepax.staffauth.dto.AuditLogFilterRequest;
import org.eyepax.staffauth.dto.AuditLogResponse;
import org.eyepax.staffauth.dto.UpdateUserRolesRequest;
import org.eyepax.staffauth.dto.UpdateUserRolesResponse;
import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.entity.Role;
import org.eyepax.staffauth.entity.UserRole;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import org.eyepax.staffauth.repository.RoleRepository;
import org.eyepax.staffauth.repository.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    
    @PatchMapping(value = "/users/{id}/roles", consumes = "application/json")
    @Transactional
    public ResponseEntity<ApiResponse<?>> updateUserRoles(@PathVariable Long id, @RequestBody(required = false) UpdateUserRolesRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Request body cannot be null"));
            }

            Optional<AppUser> userOptional = userRepository.findById(id);
            if (userOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            AppUser user = userOptional.get();

            List<String> roleNames = request.getRoles();
            if (roleNames == null || roleNames.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Roles list cannot be empty"));
            }        // Get existing roles
        List<UserRole> currentUserRoles = userRoleRepository.findAllByUserId(id);
        Set<String> newRoles = new HashSet<>(roleNames);
        Set<String> existingRoles = currentUserRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());

        // Delete removed roles
        currentUserRoles.forEach(userRole -> {
            if (!newRoles.contains(userRole.getRole().getName())) {
                userRoleRepository.deleteById(userRole.getId());
            }
        });

        // Add new roles
        for (String roleName : newRoles) {
            if (!existingRoles.contains(roleName)) {
                Role role = roleRepository.findByName(roleName);
                if (role == null) {
                    role = new Role();
                    role.setName(roleName);
                    role = roleRepository.save(role);
                }
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRoleRepository.save(userRole);
            }
        }

            // Get updated roles
            List<String> updatedRoles = userRoleRepository.findAllByUserId(id).stream()
                    .map(ur -> ur.getRole().getName())
                    .collect(Collectors.toList());

            UpdateUserRolesResponse response = new UpdateUserRolesResponse(id, updatedRoles);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error updating roles: " + e.getMessage()));
        }
    }


    @GetMapping("/audit-log")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getAuditLog(
            @RequestParam(required = false, name = "user_id") Long userId,
            @RequestParam(required = false) String range,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            AuditLogFilterRequest filterRequest = new AuditLogFilterRequest();
            filterRequest.setUserId(userId);
            filterRequest.setRange(range);
            filterRequest.setPage(page);
            filterRequest.setSize(size);

            final LocalDateTime[] dateRange = getDateRange(filterRequest.getRange());
            if (dateRange == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid date range format"));
            }

            final LocalDateTime startDate = dateRange[0];
            final LocalDateTime endDate = dateRange[1];

            PageRequest pageRequest = PageRequest.of(filterRequest.getPage(), filterRequest.getSize());
            Page<LoginAudit> audits;

            try {
                if (filterRequest.getUserId() != null) {
                    audits = loginAuditRepository.findByUserIdAndLoginTimeBetween(
                            filterRequest.getUserId(), startDate, endDate, pageRequest);
                } else {
                    audits = loginAuditRepository.findByLoginTimeBetween(startDate, endDate, pageRequest);
                }

                Page<AuditLogResponse> auditResponses = audits.map(audit -> {
                    AuditLogResponse response = new AuditLogResponse();
                    response.setId(audit.getId());
                    response.setUserId(audit.getUser().getId());
                    response.setUserEmail(audit.getUser().getEmail());
                    response.setLoginTime(audit.getLoginTime());
                    response.setLoginStatus(audit.getEventType());
                    return response;
                });

                return ResponseEntity.ok(ApiResponse.success(auditResponses));
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("Error retrieving audit logs: " + e.getMessage()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error processing request: " + e.getMessage()));
        }
    }

    private LocalDateTime[] getDateRange(String range) {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();

        if (range != null && range.contains("_to_")) {
            String[] parts = range.split("_to_");
            try {
                start = LocalDate.parse(parts[0]).atStartOfDay();
                end = LocalDate.parse(parts[1]).atTime(23, 59, 59);
            } catch (Exception e) {
                return null;
            }
        }

        return new LocalDateTime[]{start, end};
    }

}