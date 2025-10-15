package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Team Repository - Role Management
 * Data access layer for Team entity
 */
@Repository
public interface GroupRepository extends JpaRepository<Team, UUID> {

    // ========== BASIC QUERIES ==========

    /**
     * Find group by name (case-insensitive)
     * @param name group name
     * @return group if found
     */
    @Query("SELECT g FROM Team g WHERE UPPER(g.name) = UPPER(:name)")
    Optional<Team> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Find group by exact name
     * @param name group name
     * @return group if found
     */
    Optional<Team> findByName(String name);

    /**
     * Check if group exists by name (case-insensitive)
     * @param name group name
     * @return true if exists
     */
    @Query("SELECT COUNT(g) > 0 FROM Team g WHERE UPPER(g.name) = UPPER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // ========== ACTIVE GROUPS ==========

    /**
     * Find all active teams
     * @return list of active teams
     */
    List<Team> findByActiveTrue();

    /**
     * Find all active teams with pagination
     * @param pageable pagination parameters
     * @return page of active teams
     */
    Page<Team> findByActiveTrue(Pageable pageable);

    /**
     * Find active teams ordered by name
     * @return list of active teams ordered by name
     */
    @Query("SELECT g FROM Team g WHERE g.active = true ORDER BY g.name")
    List<Team> findActiveGroupsOrderedByName();

    // ========== PERMISSION LEVEL QUERIES ==========

    /**
     * Find teams by minimum permission level
     * @param minLevel minimum permission level
     * @return list of teams with permission level >= minLevel
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND g.permissionLevel >= :minLevel ORDER BY g.permissionLevel DESC")
    List<Team> findByMinimumPermissionLevel(@Param("minLevel") Integer minLevel);

    /**
     * Find admin teams (permission level >= 8)
     * @return list of admin teams
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND g.permissionLevel >= 8 ORDER BY g.permissionLevel DESC")
    List<Team> findAdminGroups();

    /**
     * Find manager teams (permission level >= 5)
     * @return list of manager teams
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND g.permissionLevel >= 5 ORDER BY g.permissionLevel DESC")
    List<Team> findManagerGroups();

    // ========== EMPLOYEE RELATIONSHIP QUERIES ==========

    /**
     * Find teams by employee ID
     * @param employeeId employee ID
     * @return list of teams where employee is member
     */
    @Query("SELECT g FROM Team g JOIN g.employees e WHERE e.id = :employeeId AND g.active = true ORDER BY g.name")
    List<Team> findByEmployeeId(@Param("employeeId") UUID employeeId);

    /**
     * Find teams not assigned to employee
     * @param employeeId employee ID
     * @return list of teams where employee is not member
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND g.id NOT IN " +
            "(SELECT gr.id FROM Team gr JOIN gr.employees e WHERE e.id = :employeeId) " +
            "ORDER BY g.name")
    List<Team> findGroupsNotAssignedToEmployee(@Param("employeeId") UUID employeeId);

    /**
     * Count employees in group
     * @param groupId group ID
     * @return number of employees in the group
     */
    @Query("SELECT COUNT(e) FROM Team g JOIN g.employees e WHERE g.id = :groupId AND e.active = true")
    Long countActiveEmployeesInGroup(@Param("groupId") UUID groupId);

    // ========== SEARCH QUERIES ==========

    /**
     * Find teams by name containing (case-insensitive)
     * @param name partial name to search
     * @return list of matching teams
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND UPPER(g.name) LIKE UPPER(CONCAT('%', :name, '%')) ORDER BY g.name")
    List<Team> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find teams by description containing (case-insensitive)
     * @param description partial description to search
     * @return list of matching teams
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND UPPER(g.description) LIKE UPPER(CONCAT('%', :description, '%')) ORDER BY g.name")
    List<Team> findByDescriptionContainingIgnoreCase(@Param("description") String description);

    /**
     * Search teams by name or description (case-insensitive)
     * @param searchTerm search term
     * @return list of matching teams
     */
    @Query("SELECT g FROM Team g WHERE g.active = true AND " +
            "(UPPER(g.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
            "UPPER(g.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY g.name")
    List<Team> searchGroups(@Param("searchTerm") String searchTerm);

    // ========== STATISTICS QUERIES ==========

    /**
     * Get group statistics
     * @return list of group statistics [groupId, groupName, employeeCount, permissionLevel]
     */
    @Query("SELECT g.id, g.name, COUNT(e), g.permissionLevel " +
            "FROM Team g LEFT JOIN g.employees e " +
            "WHERE g.active = true " +
            "GROUP BY g.id, g.name, g.permissionLevel " +
            "ORDER BY g.permissionLevel DESC, g.name")
    List<Object[]> getGroupStatistics();

    /**
     * Find teams with no employees
     * @return list of empty teams
     */
    @Query("SELECT g FROM Team g LEFT JOIN g.employees e " +
            "WHERE g.active = true " +
            "GROUP BY g " +
            "HAVING COUNT(e) = 0 " +
            "ORDER BY g.name")
    List<Team> findEmptyGroups();

    /**
     * Find most populated teams
     * @param limit maximum number of results
     * @return list of teams ordered by employee count (descending)
     */
    @Query(value = "SELECT g.* FROM group g " +
            "LEFT JOIN employee_group eg ON g.id = eg.group_id " +
            "LEFT JOIN employee e ON eg.employee_id = e.id " +
            "WHERE g.active = true " +
            "GROUP BY g.id " +
            "ORDER BY COUNT(e.id) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Team> findMostPopulatedGroups(@Param("limit") int limit);
}