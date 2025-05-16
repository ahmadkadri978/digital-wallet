package com.digitalwallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

    public class FileRequestDTO {

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity;

        @Min(value = 1000, message = "Card value must be at least 1000")
        private int value;

        @NotBlank(message = "File type is required (e.g., PDF, CSV)")
        private String type;

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

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
