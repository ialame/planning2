package com.pcagrade.order.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Registration request DTO for creating new employees
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Min(value = 1, message = "Hours per day must be at least 1")
    @Max(value = 24, message = "Hours per day cannot exceed 24")
    private Double hoursPerDay;

    @DecimalMin(value = "0.1", message = "Efficiency must be at least 0.1")
    @DecimalMax(value = "2.0", message = "Efficiency cannot exceed 2.0")
    private Double efficiency;

    @NotEmpty(message = "At least one role is required")
    private List<String> roles;
}