package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.GroupRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Team Service - Role Management
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
     * Create a new team
     * @param team the team to create
     * @return created team
     */
    public Team createGroup(@Valid @NotNull Team team) {
        try {
            log.info("Creating new team: {}", team.getName());

            // Validate business rules
            validateNewGroup(team);

            // Set default values if not provided
            if (team.getActive() == null) {
                team.setActive(true);
            }
            if (team.getPermissionLevel() == null) {
                team.setPermissionLevel(MIN_PERMISSION_LEVEL);
            }
            if (team.getCreationDate() == null) {
                team.setCreationDate(LocalDateTime.now());
            }
            if (team.getModificationDate() == null) {
                team.setModificationDate(LocalDateTime.now());
            }

            Team savedTeam = groupRepository.save(team);
            log.info("Team created successfully with ID: {}", savedTeam.getUlidString());
            return savedTeam;

        } catch (Exception e) {
            log.error("Error creating team", e);
            throw new RuntimeException("Error creating team: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing team
     * @param team the team to update
     * @return updated team
     */
    public Team updateGroup(@Valid @NotNull Team team) {
        log.info("Updating team: {}", team.getUlidString());

        if (team.getId() == null) {
            throw new IllegalArgumentException("Team ID cannot be null for update");
        }

        // Validate permission level
        if (team.getPermissionLevel() != null) {
            validatePermissionLevel(team.getPermissionLevel());
        }

        team.setModificationDate(LocalDateTime.now());

        Team updatedTeam = groupRepository.save(team);
        log.info("Team updated successfully: {}", updatedTeam.getUlidString());
        return updatedTeam;
    }

    /**
     * Delete a group (soft delete - set inactive)
     * @param groupId group ID
     */
    public void deleteGroup(UUID groupId) {
        log.info("Deleting group: {}", groupId);

        Optional<Team> groupOpt = groupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            Team team = groupOpt.get();

            // Remove all employee associations before deactivating
            team.getEmployees().clear();
            team.setActive(false);
            team.setModificationDate(LocalDateTime.now());

            groupRepository.save(team);
            log.info("Team deactivated successfully: {}", groupId);
        } else {
            throw new IllegalArgumentException("Team not found: " + groupId);
        }
    }

    // ========== RETRIEVAL OPERATIONS ==========

    /**
     * Get all active teams
     * @return list of active teams
     */
    @Transactional(readOnly = true)
    public List<Team> getAllActiveGroups() {
        log.debug("Getting all active teams");
        return groupRepository.findActiveGroupsOrderedByName();
    }

    /**
     * Get active teams with pagination
     * @param pageable pagination parameters
     * @return page of active teams
     */
    @Transactional(readOnly = true)
    public Page<Team> getAllActiveGroups(Pageable pageable) {
        log.debug("Getting active teams with pagination");
        return groupRepository.findByActiveTrue(pageable);
    }

    /**
     * Get group by ID
     * @param groupId group ID
     * @return group if found
     */
    @Transactional(readOnly = true)
    public Optional<Team> findById(UUID groupId) {
        log.debug("Finding group by ID: {}", groupId);
        return groupRepository.findById(groupId);
    }

    /**
     * Get group by name
     * @param name group name
     * @return group if found
     */
    @Transactional(readOnly = true)
    public Optional<Team> findByName(String name) {
        log.debug("Finding group by name: {}", name);
        return groupRepository.findByNameIgnoreCase(name);
    }

    /**
     * Search teams by term
     * @param searchTerm search term
     * @return list of matching teams
     */
    @Transactional(readOnly = true)
    public List<Team> searchGroups(String searchTerm) {
        log.debug("Searching teams with term: {}", searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllActiveGroups();
        }
        return groupRepository.searchGroups(searchTerm.trim());
    }

    // ========== PERMISSION-BASED QUERIES ==========

    /**
     * Get admin teams
     * @return list of admin teams
     */
    @Transactional(readOnly = true)
    public List<Team> getAdminGroups() {
        log.debug("Getting admin teams");
        return groupRepository.findAdminGroups();
    }

    /**
     * Get manager teams
     * @return list of manager teams
     */
    @Transactional(readOnly = true)
    public List<Team> getManagerGroups() {
        log.debug("Getting manager teams");
        return groupRepository.findManagerGroups();
    }

    /**
     * Get teams by minimum permission level
     * @param minLevel minimum permission level
     * @return list of teams with sufficient permission level
     */
    @Transactional(readOnly = true)
    public List<Team> getGroupsByMinimumPermissionLevel(int minLevel) {
        log.debug("Getting teams with minimum permission level: {}", minLevel);
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
        log.info("Assigning employee {} to team {}", employeeId, groupId);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Optional<Team> groupOpt = groupRepository.findById(groupId);

        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Team not found: " + groupId);
        }

        Employee employee = employeeOpt.get();
        Team team = groupOpt.get();

        if (!team.getActive()) {
            throw new IllegalArgumentException("Cannot assign to inactive team: " + team.getName());
        }

        employee.addTeam(team);
        employeeRepository.save(employee);

        log.info("Employee {} assigned to team {} successfully", employeeId, groupId);
    }

    /**
     * Remove employee from group
     * @param employeeId employee ID
     * @param groupId group ID
     */
    public void removeEmployeeFromGroup(UUID employeeId, UUID groupId) {
        log.info("Removing employee {} from team {}", employeeId, groupId);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        Optional<Team> groupOpt = groupRepository.findById(groupId);

        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
        if (groupOpt.isEmpty()) {
            throw new IllegalArgumentException("Team not found: " + groupId);
        }

        Employee employee = employeeOpt.get();
        Team team = groupOpt.get();

        employee.removeTeam(team);
        employeeRepository.save(employee);

        log.info("Employee {} removed from team {} successfully", employeeId, groupId);
    }

    /**
     * Get teams for employee
     * @param employeeId employee ID
     * @return list of teams where employee is member
     */
    @Transactional(readOnly = true)
    public List<Team> getGroupsForEmployee(UUID employeeId) {
        log.debug("Getting teams for employee: {}", employeeId);
        return groupRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get available teams for employee (teams not assigned)
     * @param employeeId employee ID
     * @return list of teams where employee is not member
     */
    @Transactional(readOnly = true)
    public List<Team> getAvailableGroupsForEmployee(UUID employeeId) {
        log.debug("Getting available teams for employee: {}", employeeId);
        return groupRepository.findGroupsNotAssignedToEmployee(employeeId);
    }

    /**
     * Update employee teams (replace all teams)
     * @param employeeId employee ID
     * @param groupIds list of group IDs
     */
    public void updateEmployeeGroups(UUID employeeId, List<UUID> groupIds) {
        log.info("Updating teams for employee {}: {}", employeeId, groupIds);

        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }

        Employee employee = employeeOpt.get();

        // Clear existing teams
        employee.getTeams().clear();

        // Add new teams
        if (groupIds != null && !groupIds.isEmpty()) {
            List<Team> teams = groupRepository.findAllById(groupIds);

            // Validate all teams exist and are active
            if (teams.size() != groupIds.size()) {
                throw new IllegalArgumentException("Some teams were not found");
            }

            for (Team team : teams) {
                if (!team.getActive()) {
                    throw new IllegalArgumentException("Cannot assign to inactive team: " + team.getName());
                }
                employee.addTeam(team);
            }
        }

        employeeRepository.save(employee);
        log.info("Groups updated successfully for employee {}", employeeId);
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate new team business rules
     */
    private void validateNewGroup(Team team) {
        if (team.getName() == null || team.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Team name is required");
        }

        // Check for duplicate name
        if (groupRepository.existsByNameIgnoreCase(team.getName())) {
            throw new IllegalArgumentException("Team with this name already exists: " + team.getName());
        }

        // Validate permission level
        if (team.getPermissionLevel() != null) {
            validatePermissionLevel(team.getPermissionLevel());
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
     * Get empty teams (teams with no employees)
     * @return list of empty teams
     */
    @Transactional(readOnly = true)
    public List<Team> getEmptyGroups() {
        log.debug("Getting empty teams");
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
     * Initialize default teams
     * Creates standard teams if they don't exist
     */
    public void initializeDefaultGroups() {
        log.info("Initializing default teams");

        createDefaultGroupIfNotExists("ADMIN", "System administrators", ADMIN_PERMISSION_LEVEL);
        createDefaultGroupIfNotExists("MANAGER", "Team managers", MANAGER_PERMISSION_LEVEL);
        createDefaultGroupIfNotExists("PROCESSOR", "Card processors", 3);
        createDefaultGroupIfNotExists("VIEWER", "Read-only access", 1);

        log.info("Default teams initialization completed");
    }

    /**
     * Create default group if it doesn't exist
     */
    private void createDefaultGroupIfNotExists(String name, String description, int permissionLevel) {
        if (!groupRepository.existsByNameIgnoreCase(name)) {
            Team team = Team.builder()
                    .name(name)
                    .description(description)
                    .permissionLevel(permissionLevel)
                    .active(true)
                    .build();

            createGroup(team);
            log.info("Created default team: {}", name);
        }
    }

    /**
     * Get all employees in a specific group
     */
    public List<Employee> getEmployeesInGroup(UUID groupId) {
        try {
            log.debug("Getting employees for team: {}", groupId);

            Optional<Team> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                log.warn("Team not found: {}", groupId);
                return new ArrayList<>();
            }

            Team team = groupOpt.get();
            List<Employee> employees = new ArrayList<>(team.getEmployees());

            log.debug("Found {} employees in team {}", employees.size(), team.getName());
            return employees;

        } catch (Exception e) {
            log.error("Error getting employees for group: {}", groupId, e);
            return new ArrayList<>();
        }
    }


}