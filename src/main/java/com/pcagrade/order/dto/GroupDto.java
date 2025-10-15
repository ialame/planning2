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
 * Team DTO Classes for API communication
 */
public class GroupDto {

    /**
     * Basic Team Information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Team information")
    public static class GroupInfo {

        @Schema(description = "Team ID (ULID format)", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW")
        private String id;

        @NotBlank(message = "Team name is required")
        @Size(min = 2, max = 50, message = "Team name must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Z_][A-Z0-9_]*$", message = "Team name must be uppercase with underscores only")
        @Schema(description = "Team name (uppercase, underscores allowed)", example = "CARD_PROCESSOR")
        private String name;

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        @Schema(description = "Team description", example = "Employees responsible for processing Pokemon cards")
        private String description;

        @NotNull(message = "Active status is required")
        @Schema(description = "Whether the group is active", example = "true")
        private Boolean active;

        @Min(value = 1, message = "Permission level must be at least 1")
        @Max(value = 10, message = "Permission level cannot exceed 10")
        @Schema(description = "Permission level (1=lowest, 10=highest)", example = "5")
        private Integer permissionLevel;

        @Schema(description = "Number of employees in this group", example = "12")
        private Long employeeCount;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Creation date", example = "2025-06-01T10:30:00")
        private LocalDateTime creationDate;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Last modification date", example = "2025-06-15T14:20:00")
        private LocalDateTime modificationDate;
    }

    /**
     * Team with Employee Details DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Team with employee details")
    public static class GroupWithEmployees {

        @Schema(description = "Team basic information")
        private GroupInfo groupInfo;

        @Schema(description = "List of employees in this group")
        private List<EmployeeDto.EmployeeBasic> employees;

        @Schema(description = "Total number of employees", example = "8")
        private Integer totalEmployees;
    }

    /**
     * Team Creation Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to create a new group")
    public static class CreateGroupRequest {

        @NotBlank(message = "Team name is required")
        @Size(min = 2, max = 50, message = "Team name must be between 2 and 50 characters")
        @Pattern(regexp = "^[A-Z_][A-Z0-9_]*$", message = "Team name must be uppercase with underscores only")
        @Schema(description = "Team name", example = "QUALITY_CONTROL", required = true)
        private String name;

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        @Schema(description = "Team description", example = "Quality control team for card grading")
        private String description;

        @Min(value = 1, message = "Permission level must be at least 1")
        @Max(value = 10, message = "Permission level cannot exceed 10")
        @Schema(description = "Permission level (1=lowest, 10=highest)", example = "6")
        private Integer permissionLevel = 1;

        @Schema(description = "Whether the group is active", example = "true")
        private Boolean active = true;
    }

    /**
     * Team Update Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to update an existing group")
    public static class UpdateGroupRequest {

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        @Schema(description = "Team description", example = "Updated description for the group")
        private String description;

        @Min(value = 1, message = "Permission level must be at least 1")
        @Max(value = 10, message = "Permission level cannot exceed 10")
        @Schema(description = "Permission level (1=lowest, 10=highest)", example = "7")
        private Integer permissionLevel;

        @Schema(description = "Whether the group is active", example = "false")
        private Boolean active;
    }

    /**
     * Employee-Team Assignment Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to assign employee to teams")
    public static class EmployeeGroupAssignmentRequest {

        @NotNull(message = "Employee ID is required")
        @Schema(description = "Employee ID", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW", required = true)
        private String employeeId;

        @Schema(description = "List of group IDs to assign to employee")
        private List<String> groupIds;
    }

    /**
     * Team Statistics DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Team statistics information")
    public static class GroupStatistics {

        @Schema(description = "Team ID", example = "01HKQJ9X4YMQR8QY8X4HQJF4ZW")
        private String groupId;

        @Schema(description = "Team name", example = "ADMIN")
        private String groupName;

        @Schema(description = "Number of employees in group", example = "5")
        private Long employeeCount;

        @Schema(description = "Permission level", example = "10")
        private Integer permissionLevel;

        @Schema(description = "Average efficiency of employees in group", example = "1.2")
        private Double averageEfficiency;

        @Schema(description = "Total work hours per day for all employees in group", example = "40")
        private Integer totalWorkHoursPerDay;
    }

    /**
     * Team Search Request DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Team search parameters")
    public static class GroupSearchRequest {

        @Schema(description = "Search term for name or description", example = "processor")
        private String searchTerm;

        @Min(value = 1, message = "Minimum permission level must be at least 1")
        @Max(value = 10, message = "Maximum permission level cannot exceed 10")
        @Schema(description = "Minimum permission level filter", example = "3")
        private Integer minPermissionLevel;

        @Min(value = 1, message = "Maximum permission level must be at least 1")
        @Max(value = 10, message = "Maximum permission level cannot exceed 10")
        @Schema(description = "Maximum permission level filter", example = "8")
        private Integer maxPermissionLevel;

        @Schema(description = "Filter by active status", example = "true")
        private Boolean active;

        @Schema(description = "Include only teams with employees", example = "true")
        private Boolean hasEmployees;

        @Min(value = 0, message = "Page number cannot be negative")
        @Schema(description = "Page number (0-based)", example = "0")
        private Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        @Schema(description = "Page size", example = "20")
        private Integer size = 20;

        @Pattern(regexp = "^(name|permissionLevel|creationDate|employeeCount)$",
                message = "Sort field must be one of: name, permissionLevel, creationDate, employeeCount")
        @Schema(description = "Sort field", example = "name")
        private String sortBy = "name";

        @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
        @Schema(description = "Sort direction", example = "asc")
        private String sortDirection = "asc";
    }

    /**
     * Permission Level Information DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Permission level information")
    public static class PermissionLevelInfo {

        @Schema(description = "Permission level", example = "8")
        private Integer level;

        @Schema(description = "Permission name", example = "ADMIN")
        private String name;

        @Schema(description = "Permission description", example = "Full system access with all privileges")
        private String description;

        @Schema(description = "Color code for UI", example = "#dc3545")
        private String colorCode;

        public static PermissionLevelInfo forLevel(int level) {
            return switch (level) {
                case 10 -> PermissionLevelInfo.builder()
                        .level(10).name("SUPER_ADMIN").description("Super administrator with unrestricted access")
                        .colorCode("#6f42c1").build();
                case 9 -> PermissionLevelInfo.builder()
                        .level(9).name("ADMIN").description("System administrator with full management rights")
                        .colorCode("#dc3545").build();
                case 8 -> PermissionLevelInfo.builder()
                        .level(8).name("SENIOR_ADMIN").description("Senior administrator with most privileges")
                        .colorCode("#fd7e14").build();
                case 7 -> PermissionLevelInfo.builder()
                        .level(7).name("MANAGER").description("Team manager with planning and oversight privileges")
                        .colorCode("#ffc107").build();
                case 6 -> PermissionLevelInfo.builder()
                        .level(6).name("SENIOR_SUPERVISOR").description("Senior supervisor with extended monitoring rights")
                        .colorCode("#20c997").build();
                case 5 -> PermissionLevelInfo.builder()
                        .level(5).name("SUPERVISOR").description("Supervisor with monitoring and basic management")
                        .colorCode("#28a745").build();
                case 4 -> PermissionLevelInfo.builder()
                        .level(4).name("SENIOR_PROCESSOR").description("Senior processor with advanced processing rights")
                        .colorCode("#17a2b8").build();
                case 3 -> PermissionLevelInfo.builder()
                        .level(3).name("PROCESSOR").description("Card processor with standard processing access")
                        .colorCode("#007bff").build();
                case 2 -> PermissionLevelInfo.builder()
                        .level(2).name("JUNIOR_PROCESSOR").description("Junior processor with limited processing access")
                        .colorCode("#6c757d").build();
                default -> PermissionLevelInfo.builder()
                        .level(1).name("VIEWER").description("Read-only access for viewing and reports")
                        .colorCode("#868e96").build();
            };
        }
    }
}