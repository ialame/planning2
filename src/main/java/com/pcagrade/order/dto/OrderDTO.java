package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Order DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private UUID id;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private LocalDateTime deliveryDeadline;
    private LocalDateTime orderDate;
    private String status;
    private Integer cardCount;
    private Integer estimatedProcessingMinutes;
    private Long priorityScore;
}