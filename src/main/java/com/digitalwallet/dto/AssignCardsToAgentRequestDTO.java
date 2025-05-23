package com.digitalwallet.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignCardsToAgentRequestDTO {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotEmpty(message = "At least one card QR code is required")
    private List<String> cardCodes;

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public List<String> getCardCodes() {
        return cardCodes;
    }

    public void setCardCodes(List<String> cardIds) {
        this.cardCodes = cardIds;
    }
}

