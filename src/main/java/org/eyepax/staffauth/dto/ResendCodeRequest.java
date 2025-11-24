package org.eyepax.staffauth.dto;

public class ResendCodeRequest {
    private String username;

    public ResendCodeRequest() {}

    public ResendCodeRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}