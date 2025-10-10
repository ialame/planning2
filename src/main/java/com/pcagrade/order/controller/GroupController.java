package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Group;
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
 * Group Controller - Role Management API
 * RESTful API for managing groups and roles
 */
@RestController
@RequestMapping("/api/groups")
@Tag(name = "Group Management", description = "API for managing groups and roles")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class GroupController {

    @Autowired
    private GroupService groupService;

    // ========== CRUD OPERATIONS ==========

    /**
     * 📋 GET ALL GROUPS
     * Endpoint: GET /api/groups
     */
    @GetMapping
    @Operation(summary = "Get all active groups", description = "Retrieve all active groups with optional pagination")
    @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllGroups(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search) {

        try {
            log.info("🔍 Getting groups - page: {}, size: {}, sortBy: {}, search: '{}'",
                    page, size, sortBy, search);

            Map<String, Object> response = new HashMap<>();

            if (page < 0 || size <= 0) {
                // Return all groups without pagination
                List<Group> groups;
                if (search != null && !search.trim().isEmpty()) {
                    groups = groupService.searchGroups(search.trim());
                } else {
                    groups = groupService.getAllActiveGroups();
                }

                response.put("groups", groups);
                response.put("totalElements", groups.size());
                response.put("totalPages", 1);
                response.put("currentPage", 0);
                response.put("hasNext", false);
                response.put("hasPrevious", false);
            } else {
                // Return paginated results
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Group> groupPage;
                if (search != null && !search.trim().isEmpty()) {
                    // For search, we need to implement pagination manually since searchGroups returns List
                    List<Group> allGroups = groupService.searchGroups(search.trim());
                    int start = Math.min(page * size, allGroups.size());
                    int end = Math.min(start + size, allGroups.size());
                    List<Group> paginatedGroups = allGroups.subList(start, end);

                    response.put("groups", paginatedGroups);
                    response.put("totalElements", allGroups.size());
                    response.put("totalPages", (int) Math.ceil((double) allGroups.size() / size));
                    response.put("currentPage", page);
                    response.put("hasNext", end < allGroups.size());
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

            log.debug("✅ Successfully retrieved {} groups",
                    ((List<?>) response.get("groups")).size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving groups: " + e.getMessage()));
        }
    }

    /**
     * 🔍 GET GROUP BY ID
     * Endpoint: GET /api/groups/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID")
    @ApiResponse(responseCode = "200", description = "Group found")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<Map<String, Object>> getGroupById(@PathVariable String id) {
        try {
            log.info("🔍 Getting group by ID: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> groupOpt = groupService.findById(groupId);

            if (groupOpt.isPresent()) {
                Group group = groupOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("group", group);
                response.put("employeeCount", groupService.countActiveEmployeesInGroup(groupId));

                log.debug("✅ Group found: {}", group.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn("⚠️ Group not found: {}", id);
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
     * Endpoint: POST /api/groups
     */
    @PostMapping
    @Operation(summary = "Create new group", description = "Create a new group/role")
    @ApiResponse(responseCode = "201", description = "Group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid group data")
    public ResponseEntity<Map<String, Object>> createGroup(@Valid @RequestBody Group group) {
        try {
            log.info("➕ Creating new group: {}", group.getName());

            Group createdGroup = groupService.createGroup(group);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group created successfully");
            response.put("group", createdGroup);

            log.info("✅ Group created successfully: {} (ID: {})",
                    createdGroup.getName(), createdGroup.getUlidString());
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
     * Endpoint: PUT /api/groups/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update group", description = "Update an existing group")
    @ApiResponse(responseCode = "200", description = "Group updated successfully")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody Group group) {
        try {
            log.info("✏️ Updating group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> existingGroupOpt = groupService.findById(groupId);

            if (existingGroupOpt.isEmpty()) {
                log.warn("⚠️ Group not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }

            group.setId(groupId);
            Group updatedGroup = groupService.updateGroup(group);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group updated successfully");
            response.put("group", updatedGroup);

            log.info("✅ Group updated successfully: {} (ID: {})",
                    updatedGroup.getName(), updatedGroup.getUlidString());
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
     * Endpoint: DELETE /api/groups/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group", description = "Deactivate a group (soft delete)")
    @ApiResponse(responseCode = "200", description = "Group deleted successfully")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable String id) {
        try {
            log.info("🗑️ Deleting group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> groupOpt = groupService.findById(groupId);

            if (groupOpt.isEmpty()) {
                log.warn("⚠️ Group not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }

            groupService.deleteGroup(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group deleted successfully");

            log.info("✅ Group deleted successfully: {}", id);
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
     * Endpoint: GET /api/groups/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get groups for employee", description = "Get all groups assigned to an employee")
    public ResponseEntity<Map<String, Object>> getGroupsForEmployee(@PathVariable String employeeId) {
        try {
            log.info("👥 Getting groups for employee: {}", employeeId);

            UUID empId = UUID.fromString(employeeId);
            List<Group> groups = groupService.getGroupsForEmployee(empId);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("employeeId", employeeId);
            response.put("totalGroups", groups.size());

            log.debug("✅ Found {} groups for employee: {}", groups.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid employee ID: {}", employeeId);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid employee ID format"));
        } catch (Exception e) {
            log.error("❌ Error getting groups for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving groups: " + e.getMessage()));
        }
    }

    /**
     * 🔗 ASSIGN EMPLOYEE TO GROUP
     * Endpoint: POST /api/groups/{groupId}/employees/{employeeId}
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
     * Endpoint: DELETE /api/groups/{groupId}/employees/{employeeId}
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
     * Endpoint: PUT /api/groups/employee/{employeeId}
     */
    @PutMapping("/employee/{employeeId}")
    @Operation(summary = "Update employee groups", description = "Replace all groups for an employee")
    public ResponseEntity<Map<String, Object>> updateEmployeeGroups(
            @PathVariable String employeeId,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("🔄 Updating groups for employee: {}", employeeId);

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
            response.put("message", "Employee groups updated successfully");
            response.put("employeeId", employeeId);
            response.put("groupIds", groupIds);

            log.info("✅ Groups updated for employee {} successfully", employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Update error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating employee groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating groups: " + e.getMessage()));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * 🔧 INITIALIZE DEFAULT GROUPS
     * Endpoint: POST /api/groups/init-defaults
     */
    @PostMapping("/init-defaults")
    @Operation(summary = "Initialize default groups", description = "Create standard groups if they don't exist")
    public ResponseEntity<Map<String, Object>> initializeDefaultGroups() {
        try {
            log.info("🔧 Initializing default groups");

            groupService.initializeDefaultGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default groups initialized successfully");

            log.info("✅ Default groups initialized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error initializing default groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error initializing groups: " + e.getMessage()));
        }
    }

    /**
     * 📊 GET GROUP STATISTICS
     * Endpoint: GET /api/groups/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get group statistics", description = "Get statistics about groups and their members")
    public ResponseEntity<Map<String, Object>> getGroupStatistics() {
        try {
            log.info("📊 Getting group statistics");

            List<Object[]> stats = groupService.getGroupStatistics();
            List<Group> emptyGroups = groupService.getEmptyGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("groupStatistics", stats);
            response.put("emptyGroups", emptyGroups);
            response.put("totalGroups", stats.size());
            response.put("emptyGroupsCount", emptyGroups.size());

            log.debug("✅ Statistics retrieved: {} total groups, {} empty groups",
                    stats.size(), emptyGroups.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting group statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving statistics: " + e.getMessage()));
        }
    }
}