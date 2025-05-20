package com.digitalwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AgentProfile {

    @Id
    private Long userId;

    @OneToOne
    @MapsId // Same ID in user table
    @JoinColumn(name = "user_id")
    private User user;

    private String region;

    private LocalDateTime assignedSince;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
