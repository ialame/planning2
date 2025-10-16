package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Work Plan Summary DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkPlanSummaryDTO {
    private Integer totalOrders;
    private Integer totalAssignments;
    private Integer employeesInvolved;
    private LocalDateTime earliestDeadline;
    private LocalDateTime latestDeadline;
    private Integer totalEstimatedMinutes;
    private List<StageStatistics> stageStatistics;
}
