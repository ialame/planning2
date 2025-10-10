// ========== ENGLISH VERSION: EmployeeRepository.java ==========
// src/main/java/com/pcagrade/order/repository/EmployeeRepository.java

package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Employee Repository - English Version
 * Handles database operations for Employee entities
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    // ========== BASIC QUERIES ==========

    /**
     * Find all active employees
     * @return list of active employees
     */
    @Query("SELECT e FROM Employee e WHERE e.active = true ORDER BY e.lastName, e.firstName")
    List<Employee> findAllActive();

    /**
     * Find all inactive employees
     * @return list of inactive employees
     */
    @Query("SELECT e FROM Employee e WHERE e.active = false ORDER BY e.lastName, e.firstName")
    List<Employee> findAllInactive();

    /**
     * Count employees by active status
     * @param active the active status
     * @return number of employees with the specified status
     */
    long countByActive(Boolean active);

    // ========== SEARCH QUERIES ==========

    /**
     * Find employees by last name
     * @param lastName the last name to search for
     * @return list of employees with matching last name
     */
    List<Employee> findByLastName(String lastName);

    /**
     * Find employees by first name
     * @param firstName the first name to search for
     * @return list of employees with matching first name
     */
    List<Employee> findByFirstName(String firstName);

    /**
     * Find employee by email
     * @param email the email to search for
     * @return optional employee with matching email
     */
    Optional<Employee> findByEmail(String email);

    /**
     * Find employees by name (case insensitive)
     * @param firstName the first name to search for
     * @param lastName the last name to search for
     * @return list of employees with matching names
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) " +
            "AND LOWER(e.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')) ORDER BY e.lastName, e.firstName")
    List<Employee> findByNameContainingIgnoreCase(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Find employees by partial name (full text search)
     * @param searchTerm the search term
     * @return list of employees matching the search term
     */
    @Query("SELECT e FROM Employee e WHERE LOWER(CONCAT(e.firstName, ' ', e.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "ORDER BY e.lastName, e.firstName")
    List<Employee> findByFullNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    // ========== WORK HOURS QUERIES ==========

    /**
     * Find employees by work hours per day
     * @param workHours the work hours to search for
     * @return list of employees with specified work hours
     */
    List<Employee> findByWorkHoursPerDay(Integer workHours);

    /**
     * Find employees with work hours in range
     * @param minHours minimum work hours
     * @param maxHours maximum work hours
     * @return list of employees with work hours in the specified range
     */
    @Query("SELECT e FROM Employee e WHERE e.workHoursPerDay BETWEEN :minHours AND :maxHours " +
            "ORDER BY e.workHoursPerDay DESC, e.lastName")
    List<Employee> findByWorkHoursRange(@Param("minHours") Integer minHours, @Param("maxHours") Integer maxHours);

    /**
     * Get average work hours per day for active employees
     * @return average work hours
     */
    @Query("SELECT AVG(e.workHoursPerDay) FROM Employee e WHERE e.active = true")
    Double getAverageWorkHours();

    // ========== DATE-BASED QUERIES ==========

    /**
     * Find employees created after specific date
     * @param date the date after which to search
     * @return list of employees created after the specified date
     */
    @Query("SELECT e FROM Employee e WHERE e.creationDate >= :date ORDER BY e.creationDate DESC")
    List<Employee> findCreatedAfter(@Param("date") LocalDateTime date);

    /**
     * Find employees created between dates
     * @param startDate start date
     * @param endDate end date
     * @return list of employees created between specified dates
     */
    @Query("SELECT e FROM Employee e WHERE e.creationDate BETWEEN :startDate AND :endDate ORDER BY e.creationDate DESC")
    List<Employee> findCreatedBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find recently created employees (last N days)
     * @param days number of days to look back
     * @return list of recently created employees
     */
    @Query("SELECT e FROM Employee e WHERE e.creationDate >= :sinceDate ORDER BY e.creationDate DESC")
    List<Employee> findRecentlyCreated(@Param("sinceDate") LocalDateTime sinceDate);

    // ========== PLANNING-RELATED QUERIES ==========

    /**
     * Find employees available for planning (active employees)
     * @return list of available employees
     */
    @Query("SELECT e FROM Employee e WHERE e.active = true ORDER BY e.workHoursPerDay DESC, e.lastName")
    List<Employee> findAvailableForPlanning();

    /**
     * Find employees with planning assignments
     * @return list of employees who have planning assignments
     */
    @Query("SELECT DISTINCT e FROM Employee e JOIN e.plannings p WHERE e.active = true ORDER BY e.lastName")
    List<Employee> findWithPlanningAssignments();

    /**
     * Find employees without planning assignments
     * @return list of employees who don't have planning assignments
     */
    @Query("SELECT e FROM Employee e WHERE e.active = true AND e.plannings IS EMPTY ORDER BY e.lastName")
    List<Employee> findWithoutPlanningAssignments();

    // ========== STATISTICAL QUERIES ==========

    /**
     * Get employee count by work hours
     * @return list of object arrays containing [workHours, count]
     */
    @Query("SELECT e.workHoursPerDay, COUNT(e) FROM Employee e WHERE e.active = true GROUP BY e.workHoursPerDay ORDER BY e.workHoursPerDay")
    List<Object[]> getEmployeeCountByWorkHours();

    /**
     * Get workload statistics for employees
     * @return list of object arrays containing employee workload data
     */
    /**
     * Get workload statistics for employees
     * @return list of object arrays containing employee workload data
     */
    @Query(value = """
    SELECT 
        HEX(e.id) as employee_id,
        CONCAT(e.first_name, ' ', e.last_name) as full_name,
        e.work_hours_per_day,
        COUNT(p.id) as planning_count,
        COALESCE(SUM(p.duration_minutes), 0) as total_minutes
    FROM employee e 
    LEFT JOIN planning p ON e.id = p.employee_id
    WHERE e.active = 1
    GROUP BY e.id, e.first_name, e.last_name, e.work_hours_per_day
    ORDER BY e.last_name, e.first_name
    """, nativeQuery = true)
    List<Object[]> getEmployeeWorkloadStatistics();

    // ========== CUSTOM NATIVE QUERIES ==========

    /**
     * Find employees with current planning load
     * This is a native query for complex calculations
     */
    @Query(value = """
        SELECT 
            HEX(e.id) as employee_id,
            CONCAT(e.first_name, ' ', e.last_name) as full_name,
            e.work_hours_per_day * 60 as max_minutes_per_day,
            COALESCE(daily_load.total_minutes, 0) as current_load_minutes,
            ROUND(COALESCE(daily_load.total_minutes, 0) / (e.work_hours_per_day * 60) * 100, 2) as load_percentage
        FROM employee e
        LEFT JOIN (
            SELECT 
                p.employee_id,
                SUM(p.duration_minutes) as total_minutes
            FROM planning p 
            WHERE DATE(p.planned_date) = CURDATE()
            GROUP BY p.employee_id
        ) daily_load ON e.id = daily_load.employee_id
        WHERE e.active = 1
        ORDER BY load_percentage DESC, e.last_name
        """, nativeQuery = true)
    List<Object[]> findEmployeesWithCurrentLoad();

    /**
     * Find top performers by completed tasks
     */
    @Query(value = """
        SELECT 
            HEX(e.id) as employee_id,
            CONCAT(e.first_name, ' ', e.last_name) as full_name,
            COUNT(p.id) as completed_tasks,
            COALESCE(SUM(p.duration_minutes), 0) as total_minutes_worked
        FROM employee e
        LEFT JOIN planning p ON e.id = p.employee_id AND p.completed = 1
        WHERE e.active = 1
        GROUP BY e.id, e.first_name, e.last_name
        ORDER BY completed_tasks DESC, total_minutes_worked DESC
        """, nativeQuery = true)
    List<Object[]> findTopPerformers();
}