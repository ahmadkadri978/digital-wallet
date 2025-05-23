package com.digitalwallet.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AssignCardsToCenterRequestDTO {

    @NotNull(message = "purchaseCenterId is required")
    private Long purchaseCenterId;

    @NotEmpty(message = "At least one card QR code must be provided")
    private List<String> cardQRCodes;

    public Long getPurchaseCenterId() {
        return purchaseCenterId;
    }

    public void setPurchaseCenterId(Long purchaseCenterId) {
        this.purchaseCenterId = purchaseCenterId;
    }

    public List<String> getCardQRCodes() {
        return cardQRCodes;
    }

    public void setCardQRCodes(List<String> cardQRCodes) {
        this.cardQRCodes = cardQRCodes;
    }
}
