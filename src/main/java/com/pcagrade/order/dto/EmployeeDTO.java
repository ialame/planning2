package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;


/**
 * Employee DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Set<String> roles;
    private Boolean active;
    private Integer dailyCapacityMinutes;
    private Integer currentWorkloadMinutes;
}
