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
 * Repository for Team (Role) management
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find a team by its unique name
     * @param name Team name (e.g., "ROLE_GRADER")
     * @return Optional containing the team if found
     */
    Optional<Team> findByName(String name);

    /**
     * Find all active teams
     * @return List of active teams
     */
    List<Team> findByActiveTrue();

    /**
     * Find all active teams with pagination
     * @param pageable Pagination information
     * @return Page of active teams
     */
    Page<Team> findByActiveTrue(Pageable pageable);

    /**
     * Find teams by name or description (search)
     * @param searchTerm Search term
     * @return List of matching teams
     */
    @Query("SELECT t FROM Team t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Team> searchByNameOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Find teams by name or description with pagination
     * @param searchTerm Search term
     * @param pageable Pagination
     * @return Page of matching teams
     */
    @Query("SELECT t FROM Team t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Team> searchByNameOrDescription(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find teams with at least one active employee
     * @return List of teams with active employees
     */
    @Query("SELECT DISTINCT t FROM Team t " +
            "JOIN t.employees e " +
            "WHERE e.active = true AND t.active = true")
    List<Team> findTeamsWithActiveEmployees();

    /**
     * Find teams with no members
     * @return List of empty teams
     */
    @Query("SELECT t FROM Team t WHERE t.employees IS EMPTY")
    List<Team> findTeamsWithNoMembers();

    /**
     * Find teams containing a specific employee
     * @param employee The employee
     * @return List of teams
     */
    @Query("SELECT t FROM Team t JOIN t.employees e WHERE e = :employee")
    List<Team> findByEmployeesContaining(@Param("employee") Employee employee);

    /**
     * Check if a team name exists
     * @param name Team name
     * @return true if exists
     */
    boolean existsByName(String name);



    /**
     * Get team statistics (count of employees per team)
     * @return List of Object arrays [teamId, teamName, employeeCount]
     */
    @Query("SELECT t.id, t.name, COUNT(e.id) " +
            "FROM Team t " +
            "LEFT JOIN t.employees e " +
            "GROUP BY t.id, t.name")
    List<Object[]> getTeamStatistics();

    /**
     * Get total capacity for a specific team
     * @param teamName Team name
     * @return Total daily capacity in minutes
     *
     * FIXED: Use workHoursPerDay instead of dailyCapacityMinutes
     * Formula: workHoursPerDay * 60 * efficiencyRating
     */
    @Query("SELECT COALESCE(SUM(e.workHoursPerDay * 60 * e.efficiencyRating), 0) " +
            "FROM Team t " +
            "JOIN t.employees e " +
            "WHERE t.name = :teamName AND e.active = true AND t.active = true")
    Integer getTotalTeamCapacity(@Param("teamName") String teamName);
}