package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Planning Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanningResponse {
    private Boolean success;
    private String message;
    private Integer assignmentsCreated;
    private WorkPlanSummaryDTO summary;
    private List<WorkAssignmentDTO> assignments;
}