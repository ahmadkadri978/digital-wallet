package com.digitalwallet.dto;

import java.time.LocalDateTime;

public class AgentResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String region;
    private LocalDateTime assignedSince;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getAssignedSince() {
        return assignedSince;
    }

    public void setAssignedSince(LocalDateTime assignedSince) {
        this.assignedSince = assignedSince;
    }
}
