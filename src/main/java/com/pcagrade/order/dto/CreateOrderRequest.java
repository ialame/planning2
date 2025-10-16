package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Create Order Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class CreateOrderRequest {
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private LocalDateTime deliveryDeadline;
    private List<CardRequest> cards;
}