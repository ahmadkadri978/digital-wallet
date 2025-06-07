package com.digitalwallet.dto;

import jakarta.validation.constraints.NotNull;

public class ActivateCardRequestDTO {
    @NotNull(message = "Agent ID is required")
    private Long agentId;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }
}
