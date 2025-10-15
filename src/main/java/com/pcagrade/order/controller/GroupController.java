package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Team;
import com.pcagrade.order.service.GroupService;
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

import java.util.*;

/**
 * Team Controller - Role Management API
 * RESTful API for managing teams and roles
 */
@RestController
@RequestMapping("/api/groups")
@Tag(name = "Team Management", description = "API for managing teams and roles")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class GroupController {

    @Autowired
    private GroupService groupService;

    // ========== CRUD OPERATIONS ==========

    /**
     * 📋 GET ALL GROUPS
     * Endpoint: GET /api/teams
     */
    @GetMapping
    @Operation(summary = "Get all active teams", description = "Retrieve all active teams with optional pagination")
    @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllGroups(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search) {

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

                response.put("groups", teams);
                response.put("totalElements", teams.size());
                response.put("totalPages", 1);
                response.put("currentPage", 0);
                response.put("hasNext", false);
                response.put("hasPrevious", false);
            } else {
                // Return paginated results
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Team> groupPage;
                if (search != null && !search.trim().isEmpty()) {
                    // For search, we need to implement pagination manually since searchGroups returns List
                    List<Team> allTeams = groupService.searchGroups(search.trim());
                    int start = Math.min(page * size, allTeams.size());
                    int end = Math.min(start + size, allTeams.size());
                    List<Team> paginatedTeams = allTeams.subList(start, end);

                    response.put("groups", paginatedTeams);
                    response.put("totalElements", allTeams.size());
                    response.put("totalPages", (int) Math.ceil((double) allTeams.size() / size));
                    response.put("currentPage", page);
                    response.put("hasNext", end < allTeams.size());
                    response.put("hasPrevious", page > 0);
                } else {
                    groupPage = groupService.getAllActiveGroups(pageable);

                    response.put("groups", groupPage.getContent());
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
     * 🔍 GET GROUP BY ID
     * Endpoint: GET /api/teams/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID")
    @ApiResponse(responseCode = "200", description = "Team found")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> getGroupById(@PathVariable String id) {
        try {
            log.info("🔍 Getting group by ID: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Team> groupOpt = groupService.findById(groupId);

            if (groupOpt.isPresent()) {
                Team team = groupOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("group", team);
                response.put("employeeCount", groupService.countActiveEmployeesInGroup(groupId));

                log.debug("✅ Team found: {}", team.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ Team not found: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group ID format: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid group ID format"));
        } catch (Exception e) {
            log.error("❌ Error getting group by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving group: " + e.getMessage()));
        }
    }

    /**
     * ➕ CREATE NEW GROUP
     * Endpoint: POST /api/teams
     */
    @PostMapping
    @Operation(summary = "Create new team", description = "Create a new team/role")
    @ApiResponse(responseCode = "201", description = "Team created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid team data")
    public ResponseEntity<Map<String, Object>> createGroup(@Valid @RequestBody Team team) {
        try {
            log.info("➕ Creating new team: {}", team.getName());

            Team createdTeam = groupService.createGroup(team);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team created successfully");
            response.put("group", createdTeam);

            log.info("✅ Team created successfully: {} (ID: {})",
                    createdTeam.getName(), createdTeam.getUlidString());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid team data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error creating team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating team: " + e.getMessage()));
        }
    }

    /**
     * ✏️ UPDATE GROUP
     * Endpoint: PUT /api/teams/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update team", description = "Update an existing team")
    @ApiResponse(responseCode = "200", description = "Team updated successfully")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody Team team) {
        try {
            log.info("✏️ Updating team: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Team> existingGroupOpt = groupService.findById(groupId);

            if (existingGroupOpt.isEmpty()) {
                log.warn("⚠️ Team not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }

            team.setId(groupId);
            Team updatedTeam = groupService.updateGroup(team);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team updated successfully");
            response.put("group", updatedTeam);

            log.info("✅ Team updated successfully: {} (ID: {})",
                    updatedTeam.getName(), updatedTeam.getUlidString());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid team data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating team: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating team: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ DELETE GROUP
     * Endpoint: DELETE /api/teams/{id}
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

    /**
     * 👥 GET GROUPS FOR EMPLOYEE
     * Endpoint: GET /api/teams/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get teams for employee", description = "Get all teams assigned to an employee")
    public ResponseEntity<Map<String, Object>> getGroupsForEmployee(@PathVariable String employeeId) {
        try {
            log.info("👥 Getting teams for employee: {}", employeeId);

            UUID empId = UUID.fromString(employeeId);
            List<Team> teams = groupService.getGroupsForEmployee(empId);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", teams);
            response.put("employeeId", employeeId);
            response.put("totalGroups", teams.size());

            log.debug("✅ Found {} teams for employee: {}", teams.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid employee ID: {}", employeeId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid employee ID format"));
        } catch (Exception e) {
            log.error("❌ Error getting teams for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving teams: " + e.getMessage()));
        }
    }

    /**
     * 🔗 ASSIGN EMPLOYEE TO GROUP
     * Endpoint: POST /api/teams/{groupId}/employees/{employeeId}
     */
    @PostMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Assign employee to group", description = "Add an employee to a group")
    public ResponseEntity<Map<String, Object>> assignEmployeeToGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("🔗 Assigning employee {} to group {}", employeeId, groupId);

            UUID empId = UUID.fromString(employeeId);
            UUID grpId = UUID.fromString(groupId);

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
     * ✂️ REMOVE EMPLOYEE FROM GROUP
     * Endpoint: DELETE /api/teams/{groupId}/employees/{employeeId}
     */
    @DeleteMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Remove employee from group", description = "Remove an employee from a group")
    public ResponseEntity<Map<String, Object>> removeEmployeeFromGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("✂️ Removing employee {} from group {}", employeeId, groupId);

            UUID empId = UUID.fromString(employeeId);
            UUID grpId = UUID.fromString(groupId);

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

    /**
     * 🔄 UPDATE EMPLOYEE GROUPS
     * Endpoint: PUT /api/teams/employee/{employeeId}
     */
    @PutMapping("/employee/{employeeId}")
    @Operation(summary = "Update employee teams", description = "Replace all teams for an employee")
    public ResponseEntity<Map<String, Object>> updateEmployeeGroups(
            @PathVariable String employeeId,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("🔄 Updating teams for employee: {}", employeeId);

            UUID empId = UUID.fromString(employeeId);

            @SuppressWarnings("unchecked")
            List<String> groupIdStrings = (List<String>) request.get("groupIds");

            List<UUID> groupIds = new ArrayList<>();
            if (groupIdStrings != null) {
                for (String groupIdStr : groupIdStrings) {
                    groupIds.add(UUID.fromString(groupIdStr));
                }
            }

            groupService.updateEmployeeGroups(empId, groupIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee teams updated successfully");
            response.put("employeeId", employeeId);
            response.put("groupIds", groupIds);

            log.info("✅ Groups updated for employee {} successfully", employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Update error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating employee teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating teams: " + e.getMessage()));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * 🔧 INITIALIZE DEFAULT GROUPS
     * Endpoint: POST /api/teams/init-defaults
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

            log.info("✅ Default teams initialized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error initializing default teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error initializing teams: " + e.getMessage()));
        }
    }

    /**
     * 📊 GET GROUP STATISTICS
     * Endpoint: GET /api/teams/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get group statistics", description = "Get statistics about teams and their members")
    public ResponseEntity<Map<String, Object>> getGroupStatistics() {
        try {
            log.info("📊 Getting group statistics");

            List<Object[]> stats = groupService.getGroupStatistics();
            List<Team> emptyTeams = groupService.getEmptyGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("groupStatistics", stats);
            response.put("emptyGroups", emptyTeams);
            response.put("totalGroups", stats.size());
            response.put("emptyGroupsCount", emptyTeams.size());

            log.debug("✅ Statistics retrieved: {} total teams, {} empty teams",
                    stats.size(), emptyTeams.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting group statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving statistics: " + e.getMessage()));
        }
    }
}