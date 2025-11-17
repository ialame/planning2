package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * Employee DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private boolean active;
    private double efficiencyRating;
    private int workHoursPerDay;
    private int dailyCapacityMinutes;
    private String photoUrl;
    private LocalDateTime creationDate;

    // ✅ AJOUTER
    private List<String> teams;
    private List<String> roles; // Alias pour teams

    // Getters/Setters
}