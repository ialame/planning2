package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Planning Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class PlanningRequest {
    private LocalDateTime startDate;
    private List<UUID> orderIds;
    private Boolean includeOnlyPending;
}