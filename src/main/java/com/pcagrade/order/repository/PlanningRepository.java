package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Planning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Planning entity
 * UPDATED: Uses new status (INT) and delai (VARCHAR) instead of ENUMs
 */
@Repository
public interface PlanningRepository extends JpaRepository<Planning, UUID> {

    // ========== FIND BY BASIC FIELDS ==========

    /**
     * Find all plannings for a specific order
     */
    List<Planning> findByOrderId(UUID orderId);

    /**
     * Find all plannings for a specific employee
     */
    List<Planning> findByEmployeeId(UUID employeeId);

    /**
     * Find plannings by date
     */
    List<Planning> findByPlanningDate(LocalDate planningDate);

    /**
     * Find plannings for employee on specific date
     */
    List<Planning> findByEmployeeIdAndPlanningDate(UUID employeeId, LocalDate planningDate);

    /**
     * Find plannings for employee in date range
     */
    List<Planning> findByEmployeeIdAndPlanningDateBetween(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    // ========== FIND BY STATUS (INT) ==========

    /**
     * Find plannings by status (using INT)
     * Examples:
     * - status = 2 (A_NOTER)
     * - status = 3 (A_CERTIFIER)
     */
    List<Planning> findByStatus(Integer status);

    /**
     * Find plannings by status and date
     */
    List<Planning> findByStatusAndPlanningDate(Integer status, LocalDate planningDate);

    /**
     * Find plannings by multiple status values
     */
    @Query("SELECT p FROM Planning p WHERE p.status IN :statuses")
    List<Planning> findByStatusIn(@Param("statuses") List<Integer> statuses);

    /**
     * Find active plannings (work stages: A_NOTER, A_CERTIFIER, etc.)
     * Status values: 2, 3, 4, 6, 7, 10, 11
     */
    @Query("SELECT p FROM Planning p WHERE p.status IN (2, 3, 4, 6, 7, 10, 11) AND p.completed = false")
    List<Planning> findActivePlannings();

    // ========== FIND BY DELAI (VARCHAR) ==========

    /**
     * Find plannings by delai priority
     * Examples:
     * - delai = 'X' (EXCELSIOR)
     * - delai = 'F+' (FAST_PLUS)
     */
    List<Planning> findByDelai(String delai);

    /**
     * Find plannings by delai and status
     */
    List<Planning> findByDelaiAndStatus(String delai, Integer status);

    /**
     * Find urgent plannings (Excelsior)
     */
    @Query("SELECT p FROM Planning p WHERE p.delai = 'X' AND p.completed = false")
    List<Planning> findUrgentPlannings();

    /**
     * Find high priority plannings (Excelsior + Fast Plus)
     */
    @Query("SELECT p FROM Planning p WHERE p.delai IN ('X', 'F+') AND p.completed = false ORDER BY p.startTime")
    List<Planning> findHighPriorityPlannings();

    // ========== FIND BY COMPLETION STATUS ==========

    /**
     * Find completed plannings
     */
    List<Planning> findByCompleted(Boolean completed);

    /**
     * Find completed plannings for employee
     */
    List<Planning> findByEmployeeIdAndCompleted(UUID employeeId, Boolean completed);

    /**
     * Find incomplete plannings for employee
     */
    @Query("SELECT p FROM Planning p WHERE p.employeeId = :employeeId AND p.completed = false")
    List<Planning> findIncompleteByEmployee(@Param("employeeId") UUID employeeId);

    // ========== FIND BY ENUM HELPER (for backward compatibility) ==========

    /**
     * Find by OrderStatus enum
     * Uses the helper method to convert enum to INT
     */
    default List<Planning> findByStatusEnum(Planning.OrderStatus statusEnum) {
        return findByStatus(statusEnum.getCode());
    }

    /**
     * Find by DelaiPriority enum
     * Uses the helper method to convert enum to VARCHAR
     */
    default List<Planning> findByDelaiEnum(Planning.DelaiPriority delaiEnum) {
        return findByDelai(delaiEnum.getCode());
    }

    // ========== WORKLOAD QUERIES ==========

    /**
     * Get today's workload for an employee (in minutes)
     */
    @Query("SELECT COALESCE(SUM(p.estimatedDurationMinutes), 0) " +
            "FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate = :date " +
            "AND p.status IN (2, 3, 4, 6, 7, 10, 11)")
    Integer getTodayWorkloadMinutes(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date);

    /**
     * Get employee workload for date range
     */
    @Query("SELECT COALESCE(SUM(p.estimatedDurationMinutes), 0) " +
            "FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate BETWEEN :startDate AND :endDate " +
            "AND p.status IN (2, 3, 4, 6, 7, 10, 11)")
    Integer getWorkloadMinutesInRange(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count plannings by status
     */
    @Query("SELECT COUNT(p) FROM Planning p WHERE p.status = :status")
    long countByStatus(@Param("status") Integer status);

    /**
     * Count active plannings for employee
     */
    @Query("SELECT COUNT(p) FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.status IN (2, 3, 4, 6, 7, 10, 11) " +
            "AND p.completed = false")
    long countActiveByEmployee(@Param("employeeId") UUID employeeId);

    // ========== DELETE QUERIES ==========

    /**
     * Delete plannings by order ID
     */
    void deleteByOrderId(UUID orderId);

    /**
     * Delete plannings by employee ID
     */
    void deleteByEmployeeId(UUID employeeId);

    /**
     * Delete plannings for a specific date
     */
    void deleteByPlanningDate(LocalDate planningDate);

    /**
     * Delete plannings in date range
     */
    @Query("DELETE FROM Planning p WHERE p.planningDate BETWEEN :startDate AND :endDate")
    void deleteByPlanningDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ========== SPECIFIC WORK TYPE QUERIES ==========

    /**
     * Find grading work (A_NOTER = 2)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 2 AND p.completed = false ORDER BY p.startTime")
    List<Planning> findGradingWork();

    /**
     * Find certification work (A_CERTIFIER = 3)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 3 AND p.completed = false ORDER BY p.startTime")
    List<Planning> findCertificationWork();

    /**
     * Find scanning work (A_SCANNER = 10)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 10 AND p.completed = false ORDER BY p.startTime")
    List<Planning> findScanningWork();

    /**
     * Find preparation work (A_PREPARER = 4)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 4 AND p.completed = false ORDER BY p.startTime")
    List<Planning> findPreparationWork();

    // ========== ADVANCED QUERIES ==========

    /**
     * Find overlapping plannings for employee
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate = :date " +
            "AND ((p.startTime <= :endTime AND p.estimatedEndTime >= :startTime))")
    List<Planning> findOverlappingPlannings(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find plannings with progress > threshold
     */
    @Query("SELECT p FROM Planning p WHERE p.progressPercentage > :threshold")
    List<Planning> findByProgressGreaterThan(@Param("threshold") Integer threshold);

    /**
     * Find overdue plannings (past date but not completed)
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.planningDate < :currentDate " +
            "AND p.completed = false " +
            "AND p.status IN (2, 3, 4, 6, 7, 10, 11)")
    List<Planning> findOverduePlannings(@Param("currentDate") LocalDate currentDate);

    /**
     * Find plannings starting soon (next N hours)
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.startTime BETWEEN :now AND :futureTime " +
            "AND p.completed = false " +
            "ORDER BY p.startTime")
    List<Planning> findStartingSoon(
            @Param("now") LocalDateTime now,
            @Param("futureTime") LocalDateTime futureTime
    );

    // ========== EXISTS QUERIES ==========

    /**
     * Check if planning exists for order
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Check if employee has plannings on date
     */
    boolean existsByEmployeeIdAndPlanningDate(UUID employeeId, LocalDate planningDate);

    /**
     * Check if planning exists for order and employee
     */
    boolean existsByOrderIdAndEmployeeId(UUID orderId, UUID employeeId);
}