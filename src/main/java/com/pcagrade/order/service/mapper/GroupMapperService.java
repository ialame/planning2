package com.pcagrade.order.service.mapper;

import com.pcagrade.order.dto.EmployeeDto;
import com.pcagrade.order.dto.GroupDto;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Group Mapper Service
 * Handles mapping between Group entities and DTOs
 */
@Service
@Slf4j
public class GroupMapperService {

    // ========== GROUP ENTITY TO DTO MAPPING ==========

    /**
     * Convert Group entity to GroupInfo DTO
     * @param group the group entity
     * @return GroupInfo DTO
     */
    public GroupDto.GroupInfo toGroupInfo(Group group) {
        if (group == null) {
            return null;
        }

        return GroupDto.GroupInfo.builder()
                .id(group.getUlidString())
                .name(group.getName())
                .description(group.getDescription())
                .active(group.getActive())
                .permissionLevel(group.getPermissionLevel())
                .employeeCount((long) (group.getEmployees() != null ? group.getEmployees().size() : 0))
                .creationDate(group.getCreationDate())
                .modificationDate(group.getModificationDate())
                .build();
    }

    /**
     * Convert Group entity to GroupInfo DTO with employee count
     * @param group the group entity
     * @param employeeCount actual employee count from database
     * @return GroupInfo DTO
     */
    public GroupDto.GroupInfo toGroupInfo(Group group, Long employeeCount) {
        GroupDto.GroupInfo groupInfo = toGroupInfo(group);
        if (groupInfo != null && employeeCount != null) {
            groupInfo.setEmployeeCount(employeeCount);
        }
        return groupInfo;
    }

    /**
     * Convert Group entity to GroupWithEmployees DTO
     * @param group the group entity
     * @return GroupWithEmployees DTO
     */
    public GroupDto.GroupWithEmployees toGroupWithEmployees(Group group) {
        if (group == null) {
            return null;
        }

        List<EmployeeDto.EmployeeBasic> employees = group.getEmployees() != null ?
                group.getEmployees().stream()
                        .filter(Employee::getActive)
                        .map(this::toEmployeeBasic)
                        .collect(Collectors.toList()) :
                List.of();

        return GroupDto.GroupWithEmployees.builder()
                .groupInfo(toGroupInfo(group))
                .employees(employees)
                .totalEmployees(employees.size())
                .build();
    }

    /**
     * Convert list of Group entities to GroupInfo DTOs
     * @param groups list of group entities
     * @return list of GroupInfo DTOs
     */
    public List<GroupDto.GroupInfo> toGroupInfoList(List<Group> groups) {
        if (groups == null) {
            return List.of();
        }

        return groups.stream()
                .map(this::toGroupInfo)
                .collect(Collectors.toList());
    }

    // ========== DTO TO GROUP ENTITY MAPPING ==========

    /**
     * Convert CreateGroupRequest DTO to Group entity
     * @param request the create request DTO
     * @return Group entity
     */
    public Group fromCreateRequest(GroupDto.CreateGroupRequest request) {
        if (request == null) {
            return null;
        }

        return Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissionLevel(request.getPermissionLevel())
                .active(request.getActive())
                .build();
    }

    /**
     * Update Group entity from UpdateGroupRequest DTO
     * @param group the group entity to update
     * @param request the update request DTO
     * @return updated Group entity
     */
    public Group updateFromRequest(Group group, GroupDto.UpdateGroupRequest request) {
        if (group == null || request == null) {
            return group;
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getPermissionLevel() != null) {
            group.setPermissionLevel(request.getPermissionLevel());
        }
        if (request.getActive() != null) {
            group.setActive(request.getActive());
        }

        return group;
    }

    // ========== EMPLOYEE MAPPING HELPERS ==========

    /**
     * Convert Employee entity to EmployeeBasic DTO
     * @param employee the employee entity
     * @return EmployeeBasic DTO
     */
    public EmployeeDto.EmployeeBasic toEmployeeBasic(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeDto.EmployeeBasic.builder()
                .id(employee.getUlidString())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .fullName(employee.getFullName())
                .initials(employee.getInitials())
                .email(employee.getEmail())
                .active(employee.getActive())
                .workHoursPerDay(employee.getWorkHoursPerDay())
                .efficiencyRating(employee.getEfficiencyRating())
                .dailyCapacityMinutes(employee.getDailyCapacityMinutes())
                .dailyCardCapacity(employee.getDailyCardCapacity(3)) // 3 minutes per card
                .build();
    }

    /**
     * Convert Employee entity to EmployeeSummary DTO
     * @param employee the employee entity
     * @return EmployeeSummary DTO
     */
    public EmployeeDto.EmployeeSummary toEmployeeSummary(Employee employee) {
        if (employee == null) {
            return null;
        }

        return EmployeeDto.EmployeeSummary.builder()
                .id(employee.getUlidString())
                .fullName(employee.getFullName())
                .initials(employee.getInitials())
                .email(employee.getEmail())
                .active(employee.getActive())
                .groupCount(employee.getGroups() != null ? employee.getGroups().size() : 0)
                .highestPermissionLevel(employee.getHighestPermissionLevel())
                .primaryRole(getPrimaryRoleName(employee.getHighestPermissionLevel()))
                .efficiencyRating(employee.getEfficiencyRating())
                .workHoursPerDay(employee.getWorkHoursPerDay())
                .build();
    }

    /**
     * Convert Employee entity to EmployeeWithGroups DTO
     * @param employee the employee entity
     * @return EmployeeWithGroups DTO
     */
    public EmployeeDto.EmployeeWithGroups toEmployeeWithGroups(Employee employee) {
        if (employee == null) {
            return null;
        }

        List<GroupDto.GroupInfo> groups = employee.getGroups() != null ?
                employee.getGroups().stream()
                        .filter(Group::getActive)
                        .map(this::toGroupInfo)
                        .collect(Collectors.toList()) :
                List.of();

        return EmployeeDto.EmployeeWithGroups.builder()
                .employeeBasic(toEmployeeBasic(employee))
                .groups(groups)
                .activeGroupNames(employee.getActiveGroupNames())
                .highestPermissionLevel(employee.getHighestPermissionLevel())
                .isAdmin(employee.isAdmin())
                .isManager(employee.isManager())
                .creationDate(employee.getCreationDate())
                .modificationDate(employee.getModificationDate())
                .build();
    }

    // ========== STATISTICS MAPPING ==========

    /**
     * Convert group statistics raw data to GroupStatistics DTO
     * @param rawData raw statistics data [groupId, groupName, employeeCount, permissionLevel]
     * @return GroupStatistics DTO
     */
    public GroupDto.GroupStatistics toGroupStatistics(Object[] rawData) {
        if (rawData == null || rawData.length < 4) {
            return null;
        }

        try {
            return GroupDto.GroupStatistics.builder()
                    .groupId(rawData[0] != null ? rawData[0].toString() : null)
                    .groupName(rawData[1] != null ? rawData[1].toString() : null)
                    .employeeCount(rawData[2] != null ? ((Number) rawData[2]).longValue() : 0L)
                    .permissionLevel(rawData[3] != null ? ((Number) rawData[3]).intValue() : 1)
                    .build();
        } catch (Exception e) {
            log.warn("Error converting group statistics: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert list of group statistics raw data to GroupStatistics DTOs
     * @param rawDataList list of raw statistics data
     * @return list of GroupStatistics DTOs
     */
    public List<GroupDto.GroupStatistics> toGroupStatisticsList(List<Object[]> rawDataList) {
        if (rawDataList == null) {
            return List.of();
        }

        return rawDataList.stream()
                .map(this::toGroupStatistics)
                .filter(stats -> stats != null)
                .collect(Collectors.toList());
    }

    // ========== UTILITY METHODS ==========

    /**
     * Get primary role name based on permission level
     * @param permissionLevel the permission level
     * @return role name
     */
    private String getPrimaryRoleName(int permissionLevel) {
        return switch (permissionLevel) {
            case 10 -> "SUPER_ADMIN";
            case 9 -> "ADMIN";
            case 8 -> "SENIOR_ADMIN";
            case 7 -> "MANAGER";
            case 6 -> "SENIOR_SUPERVISOR";
            case 5 -> "SUPERVISOR";
            case 4 -> "SENIOR_PROCESSOR";
            case 3 -> "PROCESSOR";
            case 2 -> "JUNIOR_PROCESSOR";
            default -> "VIEWER";
        };
    }

    /**
     * Get permission level info for a given level
     * @param level the permission level
     * @return PermissionLevelInfo DTO
     */
    public GroupDto.PermissionLevelInfo getPermissionLevelInfo(int level) {
        return GroupDto.PermissionLevelInfo.forLevel(level);
    }

    /**
     * Get all available permission levels
     * @return list of PermissionLevelInfo DTOs
     */
    public List<GroupDto.PermissionLevelInfo> getAllPermissionLevels() {
        return List.of(
                GroupDto.PermissionLevelInfo.forLevel(10),
                GroupDto.PermissionLevelInfo.forLevel(9),
                GroupDto.PermissionLevelInfo.forLevel(8),
                GroupDto.PermissionLevelInfo.forLevel(7),
                GroupDto.PermissionLevelInfo.forLevel(6),
                GroupDto.PermissionLevelInfo.forLevel(5),
                GroupDto.PermissionLevelInfo.forLevel(4),
                GroupDto.PermissionLevelInfo.forLevel(3),
                GroupDto.PermissionLevelInfo.forLevel(2),
                GroupDto.PermissionLevelInfo.forLevel(1)
        );
    }

    // ========== VALIDATION HELPERS ==========

    /**
     * Validate group name format
     * @param name the group name
     * @return true if valid
     */
    public boolean isValidGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Group name should be uppercase with underscores only
        return name.matches("^[A-Z_][A-Z0-9_]*$");
    }

    /**
     * Suggest group name format from input
     * @param input the input name
     * @return suggested formatted name
     */
    public String suggestGroupNameFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        return input.trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Convert permission level to color code for UI
     * @param permissionLevel the permission level
     * @return color code
     */
    public String getPermissionLevelColor(int permissionLevel) {
        return getPermissionLevelInfo(permissionLevel).getColorCode();
    }
}