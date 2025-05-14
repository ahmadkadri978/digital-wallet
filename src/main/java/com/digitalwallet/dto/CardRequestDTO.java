package com.digitalwallet.dto;

import jakarta.validation.constraints.Min;

public class CardRequestDTO {
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
    @Min(value = 1000, message = "Value must be at least 1000")
    private int value;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
