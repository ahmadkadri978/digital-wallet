package com.digitalwallet.dto;

import jakarta.validation.constraints.NotNull;

public class AssociateCardsRequestDTO {

    @NotNull(message = "Agent ID is required")
    private Long agentId;

    @NotNull(message = "File ID is required")
    private Long fileId;


    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
