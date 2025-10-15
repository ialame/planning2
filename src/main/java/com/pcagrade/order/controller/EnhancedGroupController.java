package com.pcagrade.order.controller;

import com.github.f4b6a3.ulid.Ulid;

import com.pcagrade.order.entity.Team;
import org.springframework.transaction.annotation.Transactional;
import com.pcagrade.order.dto.GroupDto;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.GroupRepository;
import com.pcagrade.order.service.GroupService;
import com.pcagrade.order.service.mapper.GroupMapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;



import java.util.*;


/**
 * Enhanced Team Controller - Role Management API with DTOs
 * RESTful API for managing teams and roles using DTOs
 */
@RestController
@RequestMapping("/api/v2/groups")
@Tag(name = "Team Management V2", description = "Enhanced API for managing teams and roles with DTOs")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class EnhancedGroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMapperService groupMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GroupRepository groupRepository;

    // ========== CRUD OPERATIONS ==========

    /**
     * 📋 GET ALL GROUPS
     * Endpoint: GET /api/v2/teams
     */
    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Get all active teams", description = "Retrieve all active teams with optional pagination and search")
    @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllGroups(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Include employee count") @RequestParam(defaultValue = "true") boolean includeEmployeeCount) {

        try {
            log.info("🔍 Getting teams - page: {}, size: {}, sortBy: {}, search: '{}'",
                    page, size, sortBy, search);

            Map<String, Object> response = new HashMap<>();

            if (page < 0 || size <= 0) {
                // Return all teams without pagination
                List<Team> teams;
                if (search != null && !search.trim().isEmpty()) {
                    teams = groupService.searchGroups(search.trim());
                } else {
                    teams = groupService.getAllActiveGroups();
                }

                List<GroupDto.GroupInfo> groupDtos = teams.stream()
                        .map(group -> includeEmployeeCount ?
                                groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                groupMapper.toGroupInfo(group))
                        .toList();

                response.put("groups", groupDtos);
                response.put("totalElements", groupDtos.size());
                response.put("totalPages", 1);
                response.put("currentPage", 0);
                response.put("hasNext", false);
                response.put("hasPrevious", false);
            } else {
                // Return paginated results
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                if (search != null && !search.trim().isEmpty()) {
                    // For search, we need to implement pagination manually
                    List<Team> allTeams = groupService.searchGroups(search.trim());
                    int start = Math.min(page * size, allTeams.size());
                    int end = Math.min(start + size, allTeams.size());
                    List<Team> paginatedTeams = allTeams.subList(start, end);

                    List<GroupDto.GroupInfo> groupDtos = paginatedTeams.stream()
                            .map(group -> includeEmployeeCount ?
                                    groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                    groupMapper.toGroupInfo(group))
                            .toList();

                    response.put("groups", groupDtos);
                    response.put("totalElements", allTeams.size());
                    response.put("totalPages", (int) Math.ceil((double) allTeams.size() / size));
                    response.put("currentPage", page);
                    response.put("hasNext", end < allTeams.size());
                    response.put("hasPrevious", page > 0);
                } else {
                    Page<Team> groupPage = groupService.getAllActiveGroups(pageable);

                    List<GroupDto.GroupInfo> groupDtos = groupPage.getContent().stream()
                            .map(group -> includeEmployeeCount ?
                                    groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                    groupMapper.toGroupInfo(group))
                            .toList();

                    response.put("groups", groupDtos);
                    response.put("totalElements", groupPage.getTotalElements());
                    response.put("totalPages", groupPage.getTotalPages());
                    response.put("currentPage", groupPage.getNumber());
                    response.put("hasNext", groupPage.hasNext());
                    response.put("hasPrevious", groupPage.hasPrevious());
                }
            }

            log.debug("✅ Successfully retrieved {} teams",
                    ((List<?>) response.get("teams")).size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving teams: " + e.getMessage()));
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



    /**
     * ➕ CREATE NEW GROUP
     * Endpoint: POST /api/v2/teams
     */
    @PostMapping
    @Operation(summary = "Create new group", description = "Create a new group/role using DTO")
    @ApiResponse(responseCode = "201", description = "Team created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid group data")
    public ResponseEntity<Map<String, Object>> createGroup(@Valid @RequestBody GroupDto.CreateGroupRequest request) {
        try {
            log.info("➕ Creating new team: {}", request.getName());

            // Validate and suggest team name format if needed
            if (!groupMapper.isValidGroupName(request.getName())) {
                String suggestedName = groupMapper.suggestGroupNameFormat(request.getName());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid team name format. Team name must be uppercase with underscores only.",
                                "suggestedName", suggestedName != null ? suggestedName : "EXAMPLE_GROUP_NAME"
                        ));
            }

            Team team = groupMapper.fromCreateRequest(request);
            Team createdTeam = groupService.createGroup(team);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team created successfully");
            response.put("group", groupMapper.toGroupInfo(createdTeam));

            log.info("✅ Team created successfully: {} (ID: {})",
                    createdTeam.getName(), createdTeam.getUlidString());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error creating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating group: " + e.getMessage()));
        }
    }

    /**
     * ✏️ UPDATE GROUP
     * Endpoint: PUT /api/v2/teams/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update group", description = "Update an existing group using DTO")
    @ApiResponse(responseCode = "200", description = "Team updated successfully")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody GroupDto.UpdateGroupRequest request) {
        try {
            log.info("✏️ Updating group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Team> existingGroupOpt = groupService.findById(groupId);

            if (existingGroupOpt.isEmpty()) {
                log.warn("⚠️ Team not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }

            Team existingTeam = existingGroupOpt.get();
            Team updatedTeam = groupMapper.updateFromRequest(existingTeam, request);
            updatedTeam = groupService.updateGroup(updatedTeam);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team updated successfully");
            response.put("group", groupMapper.toGroupInfo(updatedTeam));

            log.info("✅ Team updated successfully: {} (ID: {})",
                    updatedTeam.getName(), updatedTeam.getUlidString());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating group: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating group: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ DELETE GROUP
     * Endpoint: DELETE /api/v2/teams/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group", description = "Deactivate a group (soft delete)")
    @ApiResponse(responseCode = "200", description = "Team deleted successfully")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable String id) {
        try {
            log.info("🗑️ Deleting group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Team> groupOpt = groupService.findById(groupId);

            if (groupOpt.isEmpty()) {
                log.warn("⚠️ Team not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }

            groupService.deleteGroup(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team deleted successfully");

            log.info("✅ Team deleted successfully: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error deleting group: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting group: " + e.getMessage()));
        }
    }

    // ========== EMPLOYEE-GROUP MANAGEMENT ==========

// Dans EnhancedGroupController.java
    // SUPPRIMEZ l'ancienne méthode updateEmployeeGroups qui prend EmployeeGroupAssignmentRequest
    // et GARDEZ SEULEMENT celle-ci :

    /**
     * 👥 GET GROUPS FOR EMPLOYEE - CORRIGÉ
     * Endpoint: GET /api/v2/teams/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get teams for employee", description = "Get all teams assigned to an employee")
    public ResponseEntity<Map<String, Object>> getGroupsForEmployee(@PathVariable String employeeId) {
        try {
            log.info("👥 Getting teams for employee: {}", employeeId);

            // ✅ Utiliser parseIdToUuid au lieu de UUID.fromString
            UUID empId = parseIdToUuid(employeeId);
            List<Team> teams = groupService.getGroupsForEmployee(empId);
            List<GroupDto.GroupInfo> groupDtos = groupMapper.toGroupInfoList(teams);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupDtos);
            response.put("employeeId", employeeId);
            response.put("totalGroups", groupDtos.size());

            log.debug("✅ Found {} teams for employee: {}", groupDtos.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid employee ID: {}", employeeId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid employee ID format: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error getting teams for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving teams: " + e.getMessage()));
        }
    }

    /**
     * 🔄 UPDATE EMPLOYEE GROUPS - VERSION AVEC DEBUG ET VALIDATION
     * Endpoint: PUT /api/v2/teams/employee/{employeeId}
     */
    @PutMapping("/employee/{employeeId}")
    @Transactional
    @Operation(summary = "Update employee teams", description = "Replace all teams for an employee")
    public ResponseEntity<Map<String, Object>> updateEmployeeGroups(
            @PathVariable String employeeId,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("Updating teams for employee: {}", employeeId);

            // Convertir tous les IDs (employé et groupes) en UUID
            UUID empUuid = parseId(employeeId);

            @SuppressWarnings("unchecked")
            List<String> groupIdStrings = (List<String>) request.get("groupIds");

            List<UUID> groupUuids = new ArrayList<>();
            if (groupIdStrings != null) {
                for (String groupId : groupIdStrings) {
                    groupUuids.add(parseId(groupId));
                }
            }

            // Supprimer les assignations existantes
            String deleteSql = "DELETE FROM employee_group WHERE employee_id = ?";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            deleteQuery.setParameter(1, empUuid);
            int deletedCount = deleteQuery.executeUpdate();

            // Ajouter les nouvelles assignations
            int addedCount = 0;
            for (UUID groupUuid : groupUuids) {
                String insertSql = "INSERT INTO employee_group (employee_id, group_id) VALUES (?, ?)";
                Query insertQuery = entityManager.createNativeQuery(insertSql);
                insertQuery.setParameter(1, empUuid);
                insertQuery.setParameter(2, groupUuid);
                insertQuery.executeUpdate();
                addedCount++;
            }

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee teams updated successfully");
            response.put("assignmentsRemoved", deletedCount);
            response.put("assignmentsAdded", addedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating employee teams: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating teams: " + e.getMessage()));
        }
    }


    /**
     * 🔗 ASSIGN EMPLOYEE TO GROUP - CORRIGÉ
     * Endpoint: POST /api/v2/teams/{groupId}/employees/{employeeId}
     */
    @PostMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Assign employee to group", description = "Add an employee to a group")
    public ResponseEntity<Map<String, Object>> assignEmployeeToGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("🔗 Assigning employee {} to group {}", employeeId, groupId);

            // ✅ Utiliser parseIdToUuid au lieu de UUID.fromString
            UUID empId = parseIdToUuid(employeeId);
            UUID grpId = parseIdToUuid(groupId);

            groupService.assignEmployeeToGroup(empId, grpId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee assigned to group successfully");
            response.put("employeeId", employeeId);
            response.put("groupId", groupId);

            log.info("✅ Employee {} assigned to group {} successfully", employeeId, groupId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Assignment error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error assigning employee to group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error assigning employee: " + e.getMessage()));
        }
    }

    /**
     * ✂️ REMOVE EMPLOYEE FROM GROUP - CORRIGÉ
     * Endpoint: DELETE /api/v2/teams/{groupId}/employees/{employeeId}
     */
    @DeleteMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Remove employee from group", description = "Remove an employee from a group")
    public ResponseEntity<Map<String, Object>> removeEmployeeFromGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("✂️ Removing employee {} from group {}", employeeId, groupId);

            // ✅ Utiliser parseIdToUuid au lieu de UUID.fromString
            UUID empId = parseIdToUuid(employeeId);
            UUID grpId = parseIdToUuid(groupId);

            groupService.removeEmployeeFromGroup(empId, grpId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee removed from group successfully");
            response.put("employeeId", employeeId);
            response.put("groupId", groupId);

            log.info("✅ Employee {} removed from group {} successfully", employeeId, groupId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Removal error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error removing employee from group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error removing employee: " + e.getMessage()));
        }
    }

    // ========== PERMISSION MANAGEMENT ==========

    /**
     * 🔐 GET PERMISSION LEVELS
     * Endpoint: GET /api/v2/teams/permission-levels
     */
    @GetMapping("/permission-levels")
    @Transactional(readOnly = true)
    @Operation(summary = "Get permission levels", description = "Get all available permission levels with descriptions")
    public ResponseEntity<List<GroupDto.PermissionLevelInfo>> getPermissionLevels() {
        try {
            log.info("🔐 Getting permission levels");

            List<GroupDto.PermissionLevelInfo> permissionLevels = groupMapper.getAllPermissionLevels();

            log.debug("✅ Retrieved {} permission levels", permissionLevels.size());
            return ResponseEntity.ok(permissionLevels);

        } catch (Exception e) {
            log.error("❌ Error getting permission levels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 🔍 GET GROUPS BY PERMISSION LEVEL
     * Endpoint: GET /api/v2/teams/permission-level/{level}
     */
    @GetMapping("/permission-level/{level}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get teams by permission level", description = "Get teams with minimum permission level")
    public ResponseEntity<Map<String, Object>> getGroupsByPermissionLevel(@PathVariable int level) {
        try {
            log.info("🔍 Getting teams with permission level >= {}", level);

            if (level < 1 || level > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Permission level must be between 1 and 10"));
            }

            List<Team> teams = groupService.getGroupsByMinimumPermissionLevel(level);
            List<GroupDto.GroupInfo> groupDtos = groupMapper.toGroupInfoList(teams);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupDtos);
            response.put("minimumPermissionLevel", level);
            response.put("totalGroups", groupDtos.size());
            response.put("permissionLevelInfo", groupMapper.getPermissionLevelInfo(level));

            log.debug("✅ Found {} teams with permission level >= {}", groupDtos.size(), level);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting teams by permission level: {}", level, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving teams: " + e.getMessage()));
        }
    }

    // ========== STATISTICS AND REPORTING ==========

    /**
     * 📊 GET GROUP STATISTICS
     * Endpoint: GET /api/v2/teams/statistics
     */
    @GetMapping("/statistics")
    @Transactional(readOnly = true)
    @Operation(summary = "Get group statistics", description = "Get comprehensive statistics about teams and their members")
    public ResponseEntity<Map<String, Object>> getGroupStatistics() {
        try {
            log.info("📊 Getting group statistics");

            List<Object[]> rawStats = groupService.getGroupStatistics();
            List<GroupDto.GroupStatistics> stats = groupMapper.toGroupStatisticsList(rawStats);
            List<Team> emptyTeams = groupService.getEmptyGroups();
            List<GroupDto.GroupInfo> emptyGroupDtos = groupMapper.toGroupInfoList(emptyTeams);

            Map<String, Object> response = new HashMap<>();
            response.put("groupStatistics", stats);
            response.put("emptyGroups", emptyGroupDtos);
            response.put("totalGroups", stats.size());
            response.put("emptyGroupsCount", emptyGroupDtos.size());
            response.put("totalEmployeesInGroups", stats.stream()
                    .mapToLong(GroupDto.GroupStatistics::getEmployeeCount)
                    .sum());

            // Add permission level distribution
            Map<Integer, Long> permissionDistribution = stats.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            GroupDto.GroupStatistics::getPermissionLevel,
                            java.util.stream.Collectors.summingLong(GroupDto.GroupStatistics::getEmployeeCount)
                    ));
            response.put("permissionLevelDistribution", permissionDistribution);

            log.debug("✅ Statistics retrieved: {} total teams, {} empty teams",
                    stats.size(), emptyGroupDtos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting group statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving statistics: " + e.getMessage()));
        }
    }

    /**
     * 📈 GET GROUP SUMMARY
     * Endpoint: GET /api/v2/teams/summary
     */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    @Operation(summary = "Get group summary", description = "Get high-level summary of group system")
    public ResponseEntity<Map<String, Object>> getGroupSummary() {
        try {
            log.info("📈 Getting group summary");

            List<Team> allTeams = groupService.getAllActiveGroups();
            List<Object[]> rawStats = groupService.getGroupStatistics();
            List<Team> emptyTeams = groupService.getEmptyGroups();

            long totalEmployeesInGroups = rawStats.stream()
                    .mapToLong(stat -> ((Number) stat[2]).longValue())
                    .sum();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalActiveGroups", allTeams.size());
            summary.put("totalEmployeesInGroups", totalEmployeesInGroups);
            summary.put("emptyGroupsCount", emptyTeams.size());
            summary.put("groupsWithEmployeesCount", allTeams.size() - emptyTeams.size());

            // Average employees per group (excluding empty teams)
            double avgEmployeesPerGroup = allTeams.size() > emptyTeams.size() ?
                    (double) totalEmployeesInGroups / (allTeams.size() - emptyTeams.size()) : 0.0;
            summary.put("averageEmployeesPerGroup", Math.round(avgEmployeesPerGroup * 100.0) / 100.0);

            // Permission level breakdown
            Map<Integer, Integer> permissionLevelCounts = allTeams.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Team::getPermissionLevel,
                            java.util.stream.Collectors.summingInt(g -> 1)
                    ));
            summary.put("groupsByPermissionLevel", permissionLevelCounts);

            // Health indicators
            summary.put("healthIndicators", Map.of(
                    "hasAdminGroups", allTeams.stream().anyMatch(g -> g.getPermissionLevel() >= 8),
                    "hasManagerGroups", allTeams.stream().anyMatch(g -> g.getPermissionLevel() >= 5),
                    "hasProcessorGroups", allTeams.stream().anyMatch(g -> g.getPermissionLevel() <= 3),
                    "emptyGroupsPercentage", allTeams.isEmpty() ? 0.0 :
                            Math.round((double) emptyTeams.size() / allTeams.size() * 10000.0) / 100.0
            ));

            log.debug("✅ Team summary generated successfully");
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("❌ Error getting group summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving summary: " + e.getMessage()));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * 🔧 INITIALIZE DEFAULT GROUPS
     * Endpoint: POST /api/v2/teams/init-defaults
     */
    @PostMapping("/init-defaults")
    @Operation(summary = "Initialize default teams", description = "Create standard teams if they don't exist")
    public ResponseEntity<Map<String, Object>> initializeDefaultGroups() {
        try {
            log.info("🔧 Initializing default teams");

            groupService.initializeDefaultGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default teams initialized successfully");
            response.put("defaultGroups", List.of("ADMIN", "MANAGER", "SUPERVISOR", "PROCESSOR", "VIEWER"));

            log.info("✅ Default teams initialized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error initializing default teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error initializing teams: " + e.getMessage()));
        }
    }

    /**
     * ✅ VALIDATE GROUP NAME
     * Endpoint: POST /api/v2/teams/validate-name
     */
    @PostMapping("/validate-name")
    @Operation(summary = "Validate group name", description = "Validate and suggest group name format")
    public ResponseEntity<Map<String, Object>> validateGroupName(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            log.info("✅ Validating group name: {}", name);

            Map<String, Object> response = new HashMap<>();
            response.put("originalName", name);
            response.put("isValid", groupMapper.isValidGroupName(name));

            if (!groupMapper.isValidGroupName(name)) {
                String suggestedName = groupMapper.suggestGroupNameFormat(name);
                response.put("suggestedName", suggestedName);
                response.put("reason", "Team name must be uppercase with underscores only (A-Z, 0-9, _)");
            }

            // Check if name already exists
            if (groupMapper.isValidGroupName(name)) {
                boolean exists = groupService.findByName(name).isPresent();
                response.put("alreadyExists", exists);
                if (exists) {
                    response.put("reason", "Team name already exists");
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error validating group name", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error validating name: " + e.getMessage()));
        }
    }

    /**
     * 🔍 SEARCH GROUPS
     * Endpoint: POST /api/v2/teams/search
     */
    @PostMapping("/search")
    @Operation(summary = "Advanced group search", description = "Search teams with advanced filters using DTO")
    public ResponseEntity<Map<String, Object>> searchGroups(@Valid @RequestBody GroupDto.GroupSearchRequest searchRequest) {
        try {
            log.info("🔍 Advanced group search: {}", searchRequest.getSearchTerm());

            // For now, use basic search - can be enhanced with more complex filtering
            List<Team> teams;
            if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().trim().isEmpty()) {
                teams = groupService.searchGroups(searchRequest.getSearchTerm());
            } else {
                teams = groupService.getAllActiveGroups();
            }

            // Apply additional filters
            if (searchRequest.getMinPermissionLevel() != null) {
                teams = teams.stream()
                        .filter(g -> g.getPermissionLevel() >= searchRequest.getMinPermissionLevel())
                        .toList();
            }

            if (searchRequest.getMaxPermissionLevel() != null) {
                teams = teams.stream()
                        .filter(g -> g.getPermissionLevel() <= searchRequest.getMaxPermissionLevel())
                        .toList();
            }

            if (searchRequest.getActive() != null) {
                teams = teams.stream()
                        .filter(g -> g.getActive().equals(searchRequest.getActive()))
                        .toList();
            }

            if (searchRequest.getHasEmployees() != null) {
                if (searchRequest.getHasEmployees()) {
                    teams = teams.stream()
                            .filter(g -> !g.getEmployees().isEmpty())
                            .toList();
                } else {
                    teams = teams.stream()
                            .filter(g -> g.getEmployees().isEmpty())
                            .toList();
                }
            }

            // Apply pagination
            int totalElements = teams.size();
            int page = searchRequest.getPage();
            int size = searchRequest.getSize();
            int start = Math.min(page * size, totalElements);
            int end = Math.min(start + size, totalElements);

            List<Team> paginatedTeams = teams.subList(start, end);
            List<GroupDto.GroupInfo> groupDtos = groupMapper.toGroupInfoList(paginatedTeams);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupDtos);
            response.put("totalElements", totalElements);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("currentPage", page);
            response.put("hasNext", end < totalElements);
            response.put("hasPrevious", page > 0);
            response.put("searchCriteria", searchRequest);

            log.debug("✅ Found {} teams matching search criteria", totalElements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error searching teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error searching teams: " + e.getMessage()));
        }
    }

    private UUID parseId(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            String cleanId = idString.trim();

            // Format ULID (26 caractères)
            if (cleanId.length() == 26 && cleanId.matches("[0-9A-Z]+")) {
                Ulid ulid = Ulid.from(cleanId);
                return ulid.toUuid();
            }

            // Format UUID avec tirets (36 caractères)
            if (cleanId.length() == 36 && cleanId.contains("-")) {
                return UUID.fromString(cleanId);
            }

            // Format HEX sans tirets (32 caractères) - pour transition
            if (cleanId.length() == 32 && cleanId.matches("[0-9A-Fa-f]+")) {
                String formatted = cleanId.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            throw new IllegalArgumentException("Invalid ID format: " + cleanId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID: " + idString, e);
        }
    }


    /**
     * Convertit un String ID (hex ou UUID) vers UUID standard
     */
    private UUID parseIdToUuid(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        idString = idString.trim();

        try {
            // Format UUID avec tirets (36 caractères) - déjà correct
            if (idString.length() == 36 && idString.contains("-")) {
                return UUID.fromString(idString);
            }

            // Format Hex sans tirets (32 caractères) - conversion nécessaire
            if (idString.length() == 32 && idString.matches("[0-9A-Fa-f]+")) {
                // Convertir hex vers UUID format standard
                String formatted = idString.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                log.debug("🔄 Converting hex ID {} to UUID {}", idString, formatted);
                return UUID.fromString(formatted);
            }

            // Format ULID (26 caractères) - si applicable
            if (idString.length() == 26 && idString.matches("[0-9A-Z]+")) {
                try {
                    Ulid ulid = Ulid.from(idString);
                    UUID uuid = ulid.toUuid();
                    log.debug("🔄 Converting ULID {} to UUID {}", idString, uuid);
                    return uuid;
                } catch (Exception e) {
                    log.warn("Failed to parse as ULID: {}", idString);
                }
            }

            throw new IllegalArgumentException("Invalid ID format: " + idString + " (length: " + idString.length() + ")");

        } catch (Exception e) {
            log.error("❌ Error parsing ID: {} - {}", idString, e.getMessage());
            throw new IllegalArgumentException("Invalid ID: " + idString, e);
        }
    }

    /**
     * 🔍 GET GROUP BY ID - CORRIGÉ
     * Endpoint: GET /api/v2/teams/{id}
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID with employee details")
    @ApiResponse(responseCode = "200", description = "Team found")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> getGroupById(
            @PathVariable String id,
            @Parameter(description = "Include employee details") @RequestParam(defaultValue = "true") boolean includeEmployees) {
        try {
            log.info("🔍 Getting team by ID: {} (includeEmployees: {})", id, includeEmployees);

            // ✅ Utiliser parseIdToUuid pour supporter les IDs hexadécimaux
            UUID groupId = parseIdToUuid(id);
            Optional<Team> groupOpt = groupService.findById(groupId);

            if (groupOpt.isEmpty()) {
                log.warn("⚠️ Team not found: {}", id);
                return ResponseEntity.notFound().build();
            }

            Team team = groupOpt.get();
            Map<String, Object> response = new HashMap<>();

            // Basic team info
            response.put("id", team.getUlidString() != null ? team.getUlidString() : team.getId().toString());
            response.put("name", team.getName());
            response.put("description", team.getDescription());
            response.put("permissionLevel", team.getPermissionLevel());
            response.put("active", team.getActive());
            response.put("creationDate", team.getCreationDate());
            response.put("modificationDate", team.getModificationDate());

            if (includeEmployees) {
                // ✅ Charger et inclure les employés du groupe
                List<Employee> employees = groupService.getEmployeesInGroup(groupId);

                // Convertir en format attendu par le frontend
                List<Map<String, Object>> employeeList = employees.stream()
                        .map(emp -> {
                            Map<String, Object> empMap = new HashMap<>();
                            empMap.put("id", emp.getUlidString() != null ? emp.getUlidString() : emp.getId().toString());
                            empMap.put("firstName", emp.getFirstName());
                            empMap.put("lastName", emp.getLastName());
                            empMap.put("fullName", emp.getFullName());
                            empMap.put("email", emp.getEmail());
                            empMap.put("active", emp.getActive());
                            empMap.put("workHoursPerDay", emp.getWorkHoursPerDay());
                            empMap.put("efficiencyRating", emp.getEfficiencyRating());
                            return empMap;
                        })
                        .toList();

                response.put("employees", employeeList);
                response.put("employeeCount", employeeList.size());

                log.debug("✅ Team {} loaded with {} employees", team.getName(), employeeList.size());
            } else {
                // Juste le count sans les détails
                int employeeCount = Math.toIntExact(groupService.countActiveEmployeesInGroup(groupId));
                response.put("employeeCount", employeeCount);
            }

            log.info("✅ Team retrieved successfully: {} (ID: {})", team.getName(), id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid group ID format: " + e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error getting group by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving group: " + e.getMessage()));
        }
    }


}