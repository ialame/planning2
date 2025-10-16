package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Employee Workload Summary DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWorkloadDTO {
    private UUID employeeId;
    private String employeeName;
    private Set<String> roles;
    private Integer scheduledMinutes;
    private Integer dailyCapacityMinutes;
    private Double utilizationPercentage;
    private Integer assignmentCount;
    private LocalDateTime nextAvailableTime;
}