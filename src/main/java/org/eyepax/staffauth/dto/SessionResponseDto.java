package org.eyepax.staffauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionResponseDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionData")
    private String sessionData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionData() {
        return sessionData;
    }

    public void setSessionData(String sessionData) {
        this.sessionData = sessionData;
    }
}
