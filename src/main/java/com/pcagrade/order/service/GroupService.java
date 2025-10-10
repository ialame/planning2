package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Group;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.GroupRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Group Service - Role Management
 * Handles all group-related business logic
 */
@Service
@Transactional
@Validated
@Slf4j
public class GroupService {

    private static final int MAX_PERMISSION_LEVEL = 10;
    private static final int MIN_PERMISSION_LEVEL = 1;
    private static final int ADMIN_PERMISSION_LEVEL = 8;
    private static final int MANAGER_PERMISSION_LEVEL = 5;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new group
     * @param group the group to create
     * @return created group
     */
    public Group createGroup(@Valid @NotNull Group group) {
        try {
            log.info("Creating new group: {}", group.getName());

            // Validate business rules
            validateNewGroup(group);

            // Set default values if not provided
            if (group.getActive() == null) {
                group.setActive(true);
            }
            if (group.getPermissionLevel() == null) {
                group.setPermissionLevel(MIN_PERMISSION_LEVEL);
            }
            if (group.getCreationDate() == null) {
                group.setCreationDate(LocalDateTime.now());
            }
            if (group.getModificationDate() == null) {
                group.setModificationDate(LocalDateTime.now());
            }

            Group savedGroup = groupRepository.save(group);
            log.info("Group created successfully with ID: {}", savedGroup.getUlidString());
            return savedGroup;

        } catch (Exception e) {
            log.error("Error creating group", e);
            throw new RuntimeException("Error creating group: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing group
     * @param group the group to update
     * @return updated group
     */
    public Group updateGroup(@Valid @NotNull Group group) {
        log.info("Updating group: {}", group.getUlidString());

        if (group.getId() == null) {
            throw new IllegalArgumentException("Group ID cannot be null for update");
        }

        // Validate permission level
        if (group.getPermissionLevel() != null) {
            validatePermissionLevel(group.getPermissionLevel());
        }

        group.setModificationDate(LocalDateTime.now());

        Group updatedGroup = groupRepository.save(group);
        log.info("Group updated successfully: {}", updatedGroup.getUlidString());
        return updatedGroup;
    }

    /**
     * Delete a group (soft delete - set inactive)
     * @param groupId group ID
     */
    public void deleteGroup(UUID groupId) {
        log.info("Deleting group: {}", groupId);

        Optional<Group> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            Group group = groupOpt.get();

            // Remove all employee associations before deactivating
            group.getEmployees().clear();
            group.setActive(false);
            group.setModificationDate(LocalDateTime.now());

            groupRepository.save(group);
            log.info("Group deactivated successfully: {}", groupId);
        } else {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
    }

    // ========== RETRIEVAL OPERATIONS ==========

    /**
     * Get all active groups
     * @return list of active groups
     */
    @Transactional(readOnly = true)
    public List<Group> getAllActiveGroups() {
        log.debug("Getting all active groups");
        return groupRepository.findActiveGroupsOrderedByName();
    }

    /**
     * Get active groups with pagination
     * @param pageable pagination parameters
     * @return page of active groups
     */
    @Transactional(readOnly = true)
    public Page<Group> getAllActiveGroups(Pageable pageable) {
        log.debug("Getting active groups with pagination");
        return groupRepository.findByActiveTrue(pageable);
    }

    /**
     * Get group by ID
     * @param groupId group ID
     * @return group if found
     */
    @Transactional(readOnly = true)
    public Optional<Group> findById(UUID groupId) {
        log.debug("Finding group by ID: {}", groupId);
        return groupRepository.findById(groupId);
    }

    /**
     * Get group by name
     * @param name group name
     * @return group if found
     */
    @Transactional(readOnly = true)
    public Optional<Group> findByName(String name) {
        log.debug("Finding group by name: {}", name);
        return groupRepository.findByNameIgnoreCase(name);
    }

    /**
     * Search groups by term
     * @param searchTerm search term
     * @return list of matching groups
     */
    @Transactional(readOnly = true)
    public List<Group> searchGroups(String searchTerm) {
        log.debug("Searching groups with term: {}", searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveGroups();
        }
        return groupRepository.searchGroups(searchTerm.trim());
    }

    // ========== PERMISSION-BASED QUERIES ==========

    /**
     * Get admin groups
     * @return list of admin groups
     */
    @Transactional(readOnly = true)
    public List<Group> getAdminGroups() {
        log.debug("Getting admin groups");
        return groupRepository.findAdminGroups();
    }

    /**
     * Get manager groups
     * @return list of manager groups
     */
    @Transactional(readOnly = true)
    public List<Group> getManagerGroups() {
        log.debug("Getting manager groups");
        return groupRepository.findManagerGroups();
    }

    /**
     * Get groups by minimum permission level
     * @param minLevel minimum permission level
     * @return list of groups with sufficient permission level
     */
    @Transactional(readOnly = true)
    public List<Group> getGroupsByMinimumPermissionLevel(int minLevel) {
        log.debug("Getting groups with minimum permission level: {}", minLevel);
        validatePermissionLevel(minLevel);
        return groupRepository.findByMinimumPermissionLevel(minLevel);
    }

    // ========== EMPLOYEE-GROUP MANAGEMENT ==========

    /**
     * Assign employee to group
     * @param employeeId employee ID
     * @param groupId group ID
     */
    public void assignEmployeeToGroup(UUID employeeId, UUID groupId) {
        log.info("Assigning employee {} to group {}", employeeId, groupId);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Optional<Group> groupOpt = groupRepository.findById(groupId);

        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }

        Employee employee = employeeOpt.get();
        Group group = groupOpt.get();

        if (!group.getActive()) {
            throw new IllegalArgumentException("Cannot assign to inactive group: " + group.getName());
        }

        employee.addGroup(group);
        employeeRepository.save(employee);

        log.info("Employee {} assigned to group {} successfully", employeeId, groupId);
    }

    /**
     * Remove employee from group
     * @param employeeId employee ID
     * @param groupId group ID
     */
    public void removeEmployeeFromGroup(UUID employeeId, UUID groupId) {
        log.info("Removing employee {} from group {}", employeeId, groupId);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Optional<Group> groupOpt = groupRepository.findById(groupId);

        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }

        Employee employee = employeeOpt.get();
        Group group = groupOpt.get();

        employee.removeGroup(group);
        employeeRepository.save(employee);

        log.info("Employee {} removed from group {} successfully", employeeId, groupId);
    }

    /**
     * Get groups for employee
     * @param employeeId employee ID
     * @return list of groups where employee is member
     */
    @Transactional(readOnly = true)
    public List<Group> getGroupsForEmployee(UUID employeeId) {
        log.debug("Getting groups for employee: {}", employeeId);
        return groupRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get available groups for employee (groups not assigned)
     * @param employeeId employee ID
     * @return list of groups where employee is not member
     */
    @Transactional(readOnly = true)
    public List<Group> getAvailableGroupsForEmployee(UUID employeeId) {
        log.debug("Getting available groups for employee: {}", employeeId);
        return groupRepository.findGroupsNotAssignedToEmployee(employeeId);
    }

    /**
     * Update employee groups (replace all groups)
     * @param employeeId employee ID
     * @param groupIds list of group IDs
     */
    public void updateEmployeeGroups(UUID employeeId, List<UUID> groupIds) {
        log.info("Updating groups for employee {}: {}", employeeId, groupIds);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Employee employee = employeeOpt.get();

        // Clear existing groups
        employee.getGroups().clear();

        // Add new groups
        if (groupIds != null && !groupIds.isEmpty()) {
            List<Group> groups = groupRepository.findAllById(groupIds);

            // Validate all groups exist and are active
            if (groups.size() != groupIds.size()) {
                throw new IllegalArgumentException("Some groups were not found");
            }

            for (Group group : groups) {
                if (!group.getActive()) {
                    throw new IllegalArgumentException("Cannot assign to inactive group: " + group.getName());
                }
                employee.addGroup(group);
            }
        }

        employeeRepository.save(employee);
        log.info("Groups updated successfully for employee {}", employeeId);
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate new group business rules
     */
    private void validateNewGroup(Group group) {
        if (group.getName() == null || group.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Group name is required");
        }

        // Check for duplicate name
        if (groupRepository.existsByNameIgnoreCase(group.getName())) {
            throw new IllegalArgumentException("Group with this name already exists: " + group.getName());
        }

        // Validate permission level
        if (group.getPermissionLevel() != null) {
            validatePermissionLevel(group.getPermissionLevel());
        }
    }

    /**
     * Validate permission level
     */
    private void validatePermissionLevel(int permissionLevel) {
        if (permissionLevel < MIN_PERMISSION_LEVEL || permissionLevel > MAX_PERMISSION_LEVEL) {
            throw new IllegalArgumentException(
                    "Permission level must be between " + MIN_PERMISSION_LEVEL + " and " + MAX_PERMISSION_LEVEL
            );
        }
    }

    // ========== STATISTICS METHODS ==========

    /**
     * Get group statistics
     * @return list of group statistics
     */
    @Transactional(readOnly = true)
    public List<Object[]> getGroupStatistics() {
        log.debug("Getting group statistics");
        return groupRepository.getGroupStatistics();
    }

    /**
     * Get empty groups (groups with no employees)
     * @return list of empty groups
     */
    @Transactional(readOnly = true)
    public List<Group> getEmptyGroups() {
        log.debug("Getting empty groups");
        return groupRepository.findEmptyGroups();
    }

    /**
     * Count active employees in group
     * @param groupId group ID
     * @return number of active employees in the group
     */
    @Transactional(readOnly = true)
    public Long countActiveEmployeesInGroup(UUID groupId) {
        log.debug("Counting active employees in group: {}", groupId);
        return groupRepository.countActiveEmployeesInGroup(groupId);
    }

    // ========== UTILITY METHODS ==========

    /**
     * Initialize default groups
     * Creates standard groups if they don't exist
     */
    public void initializeDefaultGroups() {
        log.info("Initializing default groups");

        createDefaultGroupIfNotExists("ADMIN", "System administrators", ADMIN_PERMISSION_LEVEL);
        createDefaultGroupIfNotExists("MANAGER", "Team managers", MANAGER_PERMISSION_LEVEL);
        createDefaultGroupIfNotExists("PROCESSOR", "Card processors", 3);
        createDefaultGroupIfNotExists("VIEWER", "Read-only access", 1);

        log.info("Default groups initialization completed");
    }

    /**
     * Create default group if it doesn't exist
     */
    private void createDefaultGroupIfNotExists(String name, String description, int permissionLevel) {
        if (!groupRepository.existsByNameIgnoreCase(name)) {
            Group group = Group.builder()
                    .name(name)
                    .description(description)
                    .permissionLevel(permissionLevel)
                    .active(true)
                    .build();

            createGroup(group);
            log.info("Created default group: {}", name);
        }
    }

    /**
     * Get all employees in a specific group
     */
    public List<Employee> getEmployeesInGroup(UUID groupId) {
        try {
            log.debug("Getting employees for group: {}", groupId);

            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("Group not found: {}", groupId);
                return new ArrayList<>();
            }

            Group group = groupOpt.get();
            List<Employee> employees = new ArrayList<>(group.getEmployees());

            log.debug("Found {} employees in group {}", employees.size(), group.getName());
            return employees;

        } catch (Exception e) {
            log.error("Error getting employees for group: {}", groupId, e);
            return new ArrayList<>();
        }
    }


}