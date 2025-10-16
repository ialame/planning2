package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Work Assignment DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkAssignmentDTO {
    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private UUID orderId;
    private String orderNumber;
    private String processingStage;
    private Integer cardCount;
    private Integer estimatedDurationMinutes;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String status;
    private Long priorityScore;
}