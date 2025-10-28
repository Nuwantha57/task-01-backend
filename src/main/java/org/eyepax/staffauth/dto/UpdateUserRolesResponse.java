package org.eyepax.staffauth.dto;

import java.util.List;

public class UpdateUserRolesResponse {
    private Long userId;
    private List<String> roles;

    public UpdateUserRolesResponse(Long userId, List<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}