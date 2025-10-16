package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Card Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class CardRequest {
    private String cardName;
    private String cardSet;
    private String cardNumber;
}