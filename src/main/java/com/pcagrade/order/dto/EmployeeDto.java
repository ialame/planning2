package com.pcagrade.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Employee DTO Classes for API communication
 */
public class EmployeeDto {

    /**
     * Basic Employee Information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Basic employee information")
    public static class EmployeeBasic {

        @Schema(description = "Employee ID (ULID format)", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW")
        private String id;

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        @Schema(description = "Employee first name", example = "John")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        @Schema(description = "Employee last name", example = "Doe")
        private String lastName;

        @Schema(description = "Employee full name", example = "John Doe")
        private String fullName;

        @Schema(description = "Employee initials", example = "JD")
        private String initials;

        @Email(message = "Email should be valid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        @Schema(description = "Employee email address", example = "john.doe@company.com")
        private String email;

        @NotNull(message = "Active status is required")
        @Schema(description = "Whether the employee is active", example = "true")
        private Boolean active;

        @Min(value = 1, message = "Work hours per day must be at least 1")
        @Max(value = 12, message = "Work hours per day cannot exceed 12")
        @Schema(description = "Work hours per day", example = "8")
        private Integer workHoursPerDay;

        @DecimalMin(value = "0.1", message = "Efficiency rating must be at least 0.1")
        @DecimalMax(value = "3.0", message = "Efficiency rating cannot exceed 3.0")
        @Schema(description = "Efficiency rating (1.0 = 100%)", example = "1.2")
        private Double efficiencyRating;

        @Schema(description = "Daily capacity in minutes", example = "576")
        private Integer dailyCapacityMinutes;

        @Schema(description = "Daily card processing capacity", example = "192")
        private Integer dailyCardCapacity;
    }

    /**
     * Employee with Groups Information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Employee with group membership information")
    public static class EmployeeWithGroups {

        @Schema(description = "Employee basic information")
        private EmployeeBasic employeeBasic;

        @Schema(description = "List of teams this employee belongs to")
        private List<GroupDto.GroupInfo> groups;

        @Schema(description = "List of active group names", example = "[\"ADMIN\", \"MANAGER\"]")
        private List<String> activeGroupNames;

        @Schema(description = "Highest permission level from all teams", example = "10")
        private Integer highestPermissionLevel;

        @Schema(description = "Whether employee has admin privileges", example = "true")
        private Boolean isAdmin;

        @Schema(description = "Whether employee has manager privileges", example = "true")
        private Boolean isManager;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Creation date", example = "2025-06-01T10:30:00")
        private LocalDateTime creationDate;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Last modification date", example = "2025-06-15T14:20:00")
        private LocalDateTime modificationDate;
    }

    /**
     * Employee Summary for Lists DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Employee summary for list displays")
    public static class EmployeeSummary {

        @Schema(description = "Employee ID", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW")
        private String id;

        @Schema(description = "Employee full name", example = "John Doe")
        private String fullName;

        @Schema(description = "Employee initials", example = "JD")
        private String initials;

        @Schema(description = "Employee email", example = "john.doe@company.com")
        private String email;

        @Schema(description = "Whether employee is active", example = "true")
        private Boolean active;

        @Schema(description = "Number of teams", example = "3")
        private Integer groupCount;

        @Schema(description = "Highest permission level", example = "7")
        private Integer highestPermissionLevel;

        @Schema(description = "Primary role based on highest permission", example = "MANAGER")
        private String primaryRole;

        @Schema(description = "Efficiency rating", example = "1.2")
        private Double efficiencyRating;

        @Schema(description = "Work hours per day", example = "8")
        private Integer workHoursPerDay;
    }

    /**
     * Employee Creation Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to create a new employee")
    public static class CreateEmployeeRequest {

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        @Schema(description = "Employee first name", example = "Jane", required = true)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        @Schema(description = "Employee last name", example = "Smith", required = true)
        private String lastName;

        @Email(message = "Email should be valid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        @Schema(description = "Employee email address", example = "jane.smith@company.com")
        private String email;

        @Min(value = 1, message = "Work hours per day must be at least 1")
        @Max(value = 12, message = "Work hours per day cannot exceed 12")
        @Schema(description = "Work hours per day", example = "8")
        private Integer workHoursPerDay = 8;

        @DecimalMin(value = "0.1", message = "Efficiency rating must be at least 0.1")
        @DecimalMax(value = "3.0", message = "Efficiency rating cannot exceed 3.0")
        @Schema(description = "Efficiency rating (1.0 = 100%)", example = "1.0")
        private Double efficiencyRating = 1.0;

        @Schema(description = "Whether the employee is active", example = "true")
        private Boolean active = true;

        @Schema(description = "List of group IDs to assign to employee")
        private List<String> groupIds;
    }

    /**
     * Employee Update Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to update an existing employee")
    public static class UpdateEmployeeRequest {

        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        @Schema(description = "Employee first name", example = "Jane")
        private String firstName;

        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        @Schema(description = "Employee last name", example = "Smith")
        private String lastName;

        @Email(message = "Email should be valid")
        @Size(max = 150, message = "Email cannot exceed 150 characters")
        @Schema(description = "Employee email address", example = "jane.smith@company.com")
        private String email;

        @Min(value = 1, message = "Work hours per day must be at least 1")
        @Max(value = 12, message = "Work hours per day cannot exceed 12")
        @Schema(description = "Work hours per day", example = "8")
        private Integer workHoursPerDay;

        @DecimalMin(value = "0.1", message = "Efficiency rating must be at least 0.1")
        @DecimalMax(value = "3.0", message = "Efficiency rating cannot exceed 3.0")
        @Schema(description = "Efficiency rating (1.0 = 100%)", example = "1.2")
        private Double efficiencyRating;

        @Schema(description = "Whether the employee is active", example = "true")
        private Boolean active;
    }

    /**
     * Employee Search Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Employee search parameters")
    public static class EmployeeSearchRequest {

        @Schema(description = "Search term for name or email", example = "john")
        private String searchTerm;

        @Schema(description = "Filter by active status", example = "true")
        private Boolean active;

        @Schema(description = "Filter by group ID", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW")
        private String groupId;

        @Schema(description = "Filter by group name", example = "ADMIN")
        private String groupName;

        @Min(value = 1, message = "Minimum permission level must be at least 1")
        @Max(value = 10, message = "Maximum permission level cannot exceed 10")
        @Schema(description = "Minimum permission level filter", example = "5")
        private Integer minPermissionLevel;

        @DecimalMin(value = "0.1", message = "Minimum efficiency must be at least 0.1")
        @DecimalMax(value = "3.0", message = "Maximum efficiency cannot exceed 3.0")
        @Schema(description = "Minimum efficiency rating filter", example = "1.0")
        private Double minEfficiencyRating;

        @Min(value = 0, message = "Page number cannot be negative")
        @Schema(description = "Page number (0-based)", example = "0")
        private Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        @Schema(description = "Page size", example = "20")
        private Integer size = 20;

        @Pattern(regexp = "^(firstName|lastName|fullName|email|creationDate|workHoursPerDay|efficiencyRating)$",
                message = "Sort field must be one of: firstName, lastName, fullName, email, creationDate, workHoursPerDay, efficiencyRating")
        @Schema(description = "Sort field", example = "lastName")
        private String sortBy = "lastName";

        @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
        @Schema(description = "Sort direction", example = "asc")
        private String sortDirection = "asc";
    }

    /**
     * Employee Statistics DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Employee statistics information")
    public static class EmployeeStatistics {

        @Schema(description = "Total number of employees", example = "50")
        private Long totalEmployees;

        @Schema(description = "Number of active employees", example = "45")
        private Long activeEmployees;

        @Schema(description = "Number of inactive employees", example = "5")
        private Long inactiveEmployees;

        @Schema(description = "Average efficiency rating", example = "1.15")
        private Double averageEfficiencyRating;

        @Schema(description = "Total work hours per day for all active employees", example = "360")
        private Integer totalWorkHoursPerDay;

        @Schema(description = "Total daily card processing capacity", example = "1440")
        private Integer totalDailyCardCapacity;

        @Schema(description = "Number of employees with admin privileges", example = "3")
        private Long adminEmployees;

        @Schema(description = "Number of employees with manager privileges", example = "8")
        private Long managerEmployees;

        @Schema(description = "Number of employees without any teams", example = "2")
        private Long employeesWithoutGroups;

        @Schema(description = "Distribution of employees by permission level")
        private List<PermissionLevelDistribution> permissionLevelDistribution;
    }

    /**
     * Permission Level Distribution DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Distribution of employees by permission level")
    public static class PermissionLevelDistribution {

        @Schema(description = "Permission level", example = "8")
        private Integer permissionLevel;

        @Schema(description = "Permission level name", example = "ADMIN")
        private String permissionLevelName;

        @Schema(description = "Number of employees at this level", example = "3")
        private Long employeeCount;

        @Schema(description = "Percentage of total employees", example = "6.0")
        private Double percentage;
    }

    /**
     * Employee Planning Capacity DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Employee planning and capacity information")
    public static class EmployeePlanningCapacity {

        @Schema(description = "Employee basic information")
        private EmployeeSummary employeeSummary;

        @Schema(description = "Daily capacity in minutes", example = "576")
        private Integer dailyCapacityMinutes;

        @Schema(description = "Daily card processing capacity", example = "192")
        private Integer dailyCardCapacity;

        @Schema(description = "Current workload percentage", example = "75.5")
        private Double currentWorkloadPercentage;

        @Schema(description = "Number of active planning assignments", example = "3")
        private Integer activePlanningCount;

        @Schema(description = "Available capacity in minutes", example = "144")
        private Integer availableCapacityMinutes;

        @Schema(description = "Available card processing capacity", example = "48")
        private Integer availableCardCapacity;

        @Schema(description = "Whether employee is available for new assignments", example = "true")
        private Boolean availableForAssignment;
    }
}