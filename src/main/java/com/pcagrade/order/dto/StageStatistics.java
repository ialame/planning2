package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Stage statistics DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageStatistics {
    private String stageName;
    private Integer assignmentCount;
    private Integer totalCards;
    private Integer totalMinutes;
    private Integer employeesAssigned;
}