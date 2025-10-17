package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Employee;
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
 * Team Repository - Data Access Layer
 * Repository for Team entity (formerly Group - renamed to avoid SQL keyword conflict)
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    // ========== BASIC QUERIES ==========
    List<Team> findByEmployeesContaining(Employee employee);
    /**
     * Find team by exact name
     */
    Optional<Team> findByName(String name);

    /**
     * Find team by name (case-insensitive)
     */
    @Query("SELECT t FROM Team t WHERE UPPER(t.name) = UPPER(:name)")
    Optional<Team> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Check if team exists by name (case-insensitive)
     */
    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE UPPER(t.name) = UPPER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // ========== ACTIVE TEAMS ==========

    /**
     * Find all active teams
     */
    List<Team> findByActiveTrue();

    /**
     * Find all active teams with pagination
     */
    Page<Team> findByActiveTrue(Pageable pageable);

    /**
     * Find active teams ordered by name
     */
    @Query("SELECT t FROM Team t WHERE t.active = true ORDER BY t.name")
    List<Team> findActiveTeamsOrderedByName();

    // ========== PERMISSION LEVEL QUERIES ==========

    /**
     * Find teams by minimum permission level
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND t.permissionLevel >= :minLevel ORDER BY t.permissionLevel DESC")
    List<Team> findByMinimumPermissionLevel(@Param("minLevel") Integer minLevel);

    /**
     * Find admin teams (permission level >= 8)
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND t.permissionLevel >= 8 ORDER BY t.permissionLevel DESC")
    List<Team> findAdminTeams();

    /**
     * Find manager teams (permission level >= 5)
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND t.permissionLevel >= 5 ORDER BY t.permissionLevel DESC")
    List<Team> findManagerTeams();

    // ========== EMPLOYEE RELATIONSHIP QUERIES ==========

    /**
     * Find teams by employee ID
     */
    @Query("SELECT t FROM Team t JOIN t.employees e WHERE e.id = :employeeId AND t.active = true ORDER BY t.name")
    List<Team> findTeamsByEmployeeId(@Param("employeeId") UUID employeeId);

    /**
     * Find teams not assigned to employee
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND t.id NOT IN " +
            "(SELECT tm.id FROM Team tm JOIN tm.employees e WHERE e.id = :employeeId) " +
            "ORDER BY t.name")
    List<Team> findTeamsNotAssignedToEmployee(@Param("employeeId") UUID employeeId);

    /**
     * Count active employees in team
     */
    @Query("SELECT COUNT(e) FROM Team t JOIN t.employees e WHERE t.id = :teamId AND e.active = true")
    Long countActiveEmployeesInTeam(@Param("teamId") UUID teamId);

    // ========== SEARCH QUERIES ==========

    /**
     * Find teams by name containing (case-insensitive)
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND UPPER(t.name) LIKE UPPER(CONCAT('%', :name, '%')) ORDER BY t.name")
    List<Team> findByNameContainingIgnoreCase(@Param("name") String name);

    /**
     * Search teams by name (alias for backward compatibility)
     */
    @Query("SELECT t FROM Team t WHERE UPPER(t.name) LIKE UPPER(CONCAT('%', :searchTerm, '%'))")
    List<Team> searchByName(@Param("searchTerm") String searchTerm);

    /**
     * Find teams by description containing (case-insensitive)
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND UPPER(t.description) LIKE UPPER(CONCAT('%', :description, '%')) ORDER BY t.name")
    List<Team> findByDescriptionContainingIgnoreCase(@Param("description") String description);

    /**
     * Search teams by name or description (case-insensitive)
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND " +
            "(UPPER(t.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
            "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY t.name")
    List<Team> searchByNameOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Search teams by name or description with pagination
     */
    @Query("SELECT t FROM Team t WHERE t.active = true AND " +
            "(UPPER(t.name) LIKE UPPER(CONCAT('%', :searchTerm, '%')) OR " +
            "UPPER(t.description) LIKE UPPER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY t.name")
    Page<Team> searchByNameOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ========== STATISTICS QUERIES ==========

    /**
     * Get team statistics
     * Returns: [teamId, teamName, employeeCount, permissionLevel]
     */
    @Query("SELECT t.id, t.name, COUNT(e), t.permissionLevel " +
            "FROM Team t LEFT JOIN t.employees e " +
            "WHERE t.active = true " +
            "GROUP BY t.id, t.name, t.permissionLevel " +
            "ORDER BY t.permissionLevel DESC, t.name")
    List<Object[]> getTeamStatistics();

    /**
     * Find teams with no employees
     */
    @Query("SELECT t FROM Team t LEFT JOIN t.employees e " +
            "WHERE t.active = true " +
            "GROUP BY t " +
            "HAVING COUNT(e) = 0 " +
            "ORDER BY t.name")
    List<Team> findTeamsWithNoMembers();

    /**
     * Find most populated teams
     */
    @Query(value = "SELECT t.* FROM team t " +
            "LEFT JOIN employee_team et ON t.id = et.team_id " +
            "LEFT JOIN employee e ON et.employee_id = e.id " +
            "WHERE t.active = true " +
            "GROUP BY t.id " +
            "ORDER BY COUNT(e.id) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Team> findMostPopulatedTeams(@Param("limit") int limit);

    // ========== ULID SYNCHRONIZATION SUPPORT ==========

    /**
     * Find teams created after a specific ID (for incremental sync)
     * ULID advantage: IDs are chronologically sortable
     */
    List<Team> findByIdGreaterThan(UUID lastSyncId);

    /**
     * Find the most recently created team (by ULID)
     */
    Optional<Team> findTopByOrderByIdDesc();
}