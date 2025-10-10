package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Group;
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
 * Group Repository - Role Management
 * Data access layer for Group entity
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // ========== BASIC QUERIES ==========

    /**
     * Find group by name (case-insensitive)
     * @param name group name
     * @return group if found
     */
    @Query("SELECT g FROM Group g WHERE UPPER(g.name) = UPPER(:name)")
    Optional<Group> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Find group by exact name
     * @param name group name
     * @return group if found
     */
    Optional<Group> findByName(String name);

    /**
     * Check if group exists by name (case-insensitive)
     * @param name group name
     * @return true if exists
     */
    @Query("SELECT COUNT(g) > 0 FROM Group g WHERE UPPER(g.name) = UPPER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // ========== ACTIVE GROUPS ==========

    /**
     * Find all active groups
     * @return list of active groups
     */
    List<Group> findByActiveTrue();

    /**
     * Find all active groups with pagination
     * @param pageable pagination parameters
     * @return page of active groups
     */
    Page<Group> findByActiveTrue(Pageable pageable);

    /**
     * Find active groups ordered by name
     * @return list of active groups ordered by name
     */
    @Query("SELECT g FROM Group g WHERE g.active = true ORDER BY g.name")
    List<Group> findActiveGroupsOrderedByName();

    // ========== PERMISSION LEVEL QUERIES ==========

    /**
     * Find groups by minimum permission level
     * @param minLevel minimum permission level
     * @return list of groups with permission level >= minLevel
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND g.permissionLevel >= :minLevel ORDER BY g.permissionLevel DESC")
    List<Group> findByMinimumPermissionLevel(@Param("minLevel") Integer minLevel);

    /**
     * Find admin groups (permission level >= 8)
     * @return list of admin groups
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND g.permissionLevel >= 8 ORDER BY g.permissionLevel DESC")
    List<Group> findAdminGroups();

    /**
     * Find manager groups (permission level >= 5)
     * @return list of manager groups
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND g.permissionLevel >= 5 ORDER BY g.permissionLevel DESC")
    List<Group> findManagerGroups();

    // ========== EMPLOYEE RELATIONSHIP QUERIES ==========

    /**
     * Find groups by employee ID
     * @param employeeId employee ID
     * @return list of groups where employee is member
     */
    @Query("SELECT g FROM Group g JOIN g.employees e WHERE e.id = :employeeId AND g.active = true ORDER BY g.name")
    List<Group> findByEmployeeId(@Param("employeeId") UUID employeeId);

    /**
     * Find groups not assigned to employee
     * @param employeeId employee ID
     * @return list of groups where employee is not member
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND g.id NOT IN " +
            "(SELECT gr.id FROM Group gr JOIN gr.employees e WHERE e.id = :employeeId) " +
            "ORDER BY g.name")
    List<Group> findGroupsNotAssignedToEmployee(@Param("employeeId") UUID employeeId);

    /**
     * Count employees in group
     * @param groupId group ID
     * @return number of employees in the group
     */
    @Query("SELECT COUNT(e) FROM Group g JOIN g.employees e WHERE g.id = :groupId AND e.active = true")
    Long countActiveEmployeesInGroup(@Param("groupId") UUID groupId);

    // ========== SEARCH QUERIES ==========

    /**
     * Find groups by name containing (case-insensitive)
     * @param name partial name to search
     * @return list of matching groups
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND UPPER(g.name) LIKE UPPER(CONCAT('%', :name, '%')) ORDER BY g.name")
    List<Group> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Find groups by description containing (case-insensitive)
     * @param description partial description to search
     * @return list of matching groups
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND UPPER(g.description) LIKE UPPER(CONCAT('%', :description, '%')) ORDER BY g.name")
    List<Group> findByDescriptionContainingIgnoreCase(@Param("description") String description);

    /**
     * Search groups by name or description (case-insensitive)
     * @param searchTerm search term
     * @return list of matching groups
     */
    @Query("SELECT g FROM Group g WHERE g.active = true AND " +
            "(UPPER(g.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
            "UPPER(g.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY g.name")
    List<Group> searchGroups(@Param("searchTerm") String searchTerm);

    // ========== STATISTICS QUERIES ==========

    /**
     * Get group statistics
     * @return list of group statistics [groupId, groupName, employeeCount, permissionLevel]
     */
    @Query("SELECT g.id, g.name, COUNT(e), g.permissionLevel " +
            "FROM Group g LEFT JOIN g.employees e " +
            "WHERE g.active = true " +
            "GROUP BY g.id, g.name, g.permissionLevel " +
            "ORDER BY g.permissionLevel DESC, g.name")
    List<Object[]> getGroupStatistics();

    /**
     * Find groups with no employees
     * @return list of empty groups
     */
    @Query("SELECT g FROM Group g LEFT JOIN g.employees e " +
            "WHERE g.active = true " +
            "GROUP BY g " +
            "HAVING COUNT(e) = 0 " +
            "ORDER BY g.name")
    List<Group> findEmptyGroups();

    /**
     * Find most populated groups
     * @param limit maximum number of results
     * @return list of groups ordered by employee count (descending)
     */
    @Query(value = "SELECT g.* FROM j_group g " +
            "LEFT JOIN j_employee_group eg ON g.id = eg.group_id " +
            "LEFT JOIN j_employee e ON eg.employee_id = e.id " +
            "WHERE g.active = true " +
            "GROUP BY g.id " +
            "ORDER BY COUNT(e.id) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Group> findMostPopulatedGroups(@Param("limit") int limit);
}