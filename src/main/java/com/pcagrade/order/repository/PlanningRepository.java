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
 * FIXED: Removed references to non-existent enums (Planning.OrderStatus, Planning.DelaiPriority)
 * Uses INT for status and VARCHAR for delai directly
 */
@Repository
public interface PlanningRepository extends JpaRepository<Planning, UUID> {

    // ========== FIND BY SYMFONY ORDER ID (UPDATED) ==========

    /**
     * Find all plannings for a specific order using Symfony order ID
     */
    List<Planning> findBySymfonyOrderId(String symfonyOrderId);

    /**
     * Check if planning exists for a Symfony order
     */
    boolean existsBySymfonyOrderId(String symfonyOrderId);

    /**
     * Find first planning for a Symfony order
     */
    Optional<Planning> findFirstBySymfonyOrderIdOrderByStartTimeAsc(String symfonyOrderId);

    // ========== FIND BY EMPLOYEE ==========

    /**
     * Find all plannings for a specific employee
     */
    List<Planning> findByEmployeeId(UUID employeeId);

    /**
     * Find plannings for employee ordered by start time
     */
    List<Planning> findByEmployeeIdOrderByStartTimeAsc(UUID employeeId);

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

    // ========== FIND BY DATE ==========

    /**
     * Find plannings by date
     */
    List<Planning> findByPlanningDate(LocalDate planningDate);

    /**
     * Find plannings in date range
     */
    List<Planning> findByPlanningDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find plannings starting after a specific time
     */
    List<Planning> findByStartTimeAfter(LocalDateTime startTime);

    /**
     * Find plannings starting before a specific time
     */
    List<Planning> findByStartTimeBefore(LocalDateTime startTime);

    // ========== FIND BY STATUS (INT) ==========

    /**
     * Find plannings by status (using INT)
     * Status codes:
     * - 1 = A_RECEPTIONNER (To be received)
     * - 2 = A_NOTER (To be graded)
     * - 3 = A_CERTIFIER (To be certified)
     * - 4 = A_PREPARER (To be prepared)
     * - 5 = ENVOYEE (Sent)
     * - 10 = A_SCANNER (To be scanned)
     * - 41 = EN_COURS_NOTATION (Grading in progress)
     * - 42 = A_ENVOYER (Ready to send)
     */
    List<Planning> findByStatus(Integer status);

    /**
     * Find plannings by status and date
     */
    List<Planning> findByStatusAndPlanningDate(Integer status, LocalDate planningDate);

    /**
     * Find plannings by multiple status values
     */
    @Query("SELECT p FROM Planning p WHERE p.status IN :statuses ORDER BY p.startTime")
    List<Planning> findByStatusIn(@Param("statuses") List<Integer> statuses);

    /**
     * Find active plannings (work in progress)
     * Status values: 2 (A_NOTER), 3 (A_CERTIFIER), 4 (A_PREPARER), 10 (A_SCANNER), 41 (EN_COURS)
     */
    @Query("SELECT p FROM Planning p WHERE p.status IN (2, 3, 4, 10, 41) ORDER BY p.startTime")
    List<Planning> findActivePlannings();

    /**
     * Find grading plannings (status = 2 or 41)
     */
    @Query("SELECT p FROM Planning p WHERE p.status IN (2, 41) ORDER BY p.startTime")
    List<Planning> findGradingPlannings();

    /**
     * Find certification plannings (status = 3)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 3 ORDER BY p.startTime")
    List<Planning> findCertificationPlannings();

    /**
     * Find scanning plannings (status = 10)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 10 ORDER BY p.startTime")
    List<Planning> findScanningPlannings();

    /**
     * Find packaging plannings (status = 4)
     */
    @Query("SELECT p FROM Planning p WHERE p.status = 4 ORDER BY p.startTime")
    List<Planning> findPackagingPlannings();

    // ========== FIND BY DELAI (VARCHAR) ==========

    /**
     * Find plannings by delai priority
     * Delai codes:
     * - X = Express (highest priority)
     * - F+ = Fast Plus
     * - F = Fast
     * - C = Classic
     * - E = Economy (lowest priority)
     */
    List<Planning> findByDelai(String delai);

    /**
     * Find plannings by delai and status
     */
    List<Planning> findByDelaiAndStatus(String delai, Integer status);

    /**
     * Find plannings by delai ordered by start time
     */
    List<Planning> findByDelaiOrderByStartTimeAsc(String delai);

    /**
     * Find urgent plannings (Express priority)
     */
    @Query("SELECT p FROM Planning p WHERE p.delai = 'X' ORDER BY p.startTime")
    List<Planning> findExpressPlannings();

    /**
     * Find high priority plannings (Express + Fast Plus)
     */
    @Query("SELECT p FROM Planning p WHERE p.delai IN ('X', 'F+') ORDER BY p.startTime")
    List<Planning> findHighPriorityPlannings();

    /**
     * Find plannings ordered by priority
     */
    @Query("SELECT p FROM Planning p ORDER BY " +
            "CASE p.delai " +
            "  WHEN 'X' THEN 1 " +
            "  WHEN 'F+' THEN 2 " +
            "  WHEN 'F' THEN 3 " +
            "  WHEN 'C' THEN 4 " +
            "  WHEN 'E' THEN 5 " +
            "  ELSE 6 " +
            "END, p.startTime ASC")
    List<Planning> findAllOrderedByPriority();

    // ========== WORKLOAD QUERIES ==========

    /**
     * Get today's workload for an employee (in minutes)
     */
    @Query("SELECT COALESCE(SUM(p.durationMinutes), 0) " +
            "FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate = :date " +
            "AND p.status IN (2, 3, 4, 10, 41)")
    Integer getTodayWorkloadMinutes(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );

    /**
     * Get employee workload for date range
     */
    @Query("SELECT COALESCE(SUM(p.durationMinutes), 0) " +
            "FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate BETWEEN :startDate AND :endDate " +
            "AND p.status IN (2, 3, 4, 10, 41)")
    Integer getWorkloadMinutesInRange(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get total card count for employee on date
     */
    @Query("SELECT COALESCE(SUM(p.cardCount), 0) " +
            "FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate = :date")
    Integer getTodayCardCount(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
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
            "AND p.status IN (2, 3, 4, 10, 41)")
    long countActivePlanningsByEmployee(@Param("employeeId") UUID employeeId);

    /**
     * Count plannings for employee on date
     */
    long countByEmployeeIdAndPlanningDate(UUID employeeId, LocalDate planningDate);

    // ========== ADVANCED QUERIES ==========

    /**
     * Find overlapping plannings for employee
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.planningDate = :date " +
            "AND p.startTime < :endTime " +
            "AND FUNCTION('TIMESTAMPADD', MINUTE, p.durationMinutes, p.startTime) > :startTime")
    List<Planning> findOverlappingPlannings(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find overdue plannings (past date but not completed status)
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.planningDate < :currentDate " +
            "AND p.status NOT IN (5, 42)")  // Not ENVOYEE or A_ENVOYER
    List<Planning> findOverduePlannings(@Param("currentDate") LocalDate currentDate);

    /**
     * Find plannings starting soon (next N hours)
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.startTime BETWEEN :now AND :futureTime " +
            "AND p.status IN (2, 3, 4, 10) " +
            "ORDER BY p.startTime")
    List<Planning> findStartingSoon(
            @Param("now") LocalDateTime now,
            @Param("futureTime") LocalDateTime futureTime
    );

    /**
     * Find plannings by employee and status in date range
     */
    @Query("SELECT p FROM Planning p " +
            "WHERE p.employeeId = :employeeId " +
            "AND p.status = :status " +
            "AND p.planningDate BETWEEN :startDate AND :endDate " +
            "ORDER BY p.startTime")
    List<Planning> findByEmployeeStatusAndDateRange(
            @Param("employeeId") UUID employeeId,
            @Param("status") Integer status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ========== STATISTICS QUERIES ==========

    /**
     * Get total cards planned for a date
     */
    @Query("SELECT COALESCE(SUM(p.cardCount), 0) FROM Planning p WHERE p.planningDate = :date")
    long getTotalCardsForDate(@Param("date") LocalDate date);

    /**
     * Get total duration for a date (in minutes)
     */
    @Query("SELECT COALESCE(SUM(p.durationMinutes), 0) FROM Planning p WHERE p.planningDate = :date")
    long getTotalDurationForDate(@Param("date") LocalDate date);

    /**
     * Count plannings by delai
     */
    long countByDelai(String delai);

    /**
     * Get average duration by status
     */
    @Query("SELECT AVG(p.durationMinutes) FROM Planning p WHERE p.status = :status")
    Double getAverageDurationByStatus(@Param("status") Integer status);

    // ========== EXISTENCE CHECKS ==========

    /**
     * Check if planning exists for order
     */

    /**
     * Check if employee has plannings on date
     */
    boolean existsByEmployeeIdAndPlanningDate(UUID employeeId, LocalDate planningDate);

    /**
     * Check if planning exists for order and employee
     */
    boolean existsBySymfonyOrderIdAndEmployeeId(String symfonyOrderId, UUID employeeId);

    // ========== DELETE OPERATIONS ==========

    /**
     * Delete plannings by date
     */
    long deleteByPlanningDate(LocalDate planningDate);

    /**
     * Delete plannings in date range
     */
    long deleteByPlanningDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Delete plannings for specific order
     */
    long deleteBySymfonyOrderId(String symfonyOrderId);

    /**
     * Delete plannings for employee
     */
    long deleteByEmployeeId(UUID employeeId);

    // ========== ULID SYNCHRONIZATION ==========

    /**
     * Find plannings created after a specific ID (for synchronization)
     * Takes advantage of ULID chronological ordering
     */
    List<Planning> findByIdGreaterThan(UUID lastSyncId);

    /**
     * Get the most recent planning (by ULID)
     */
    Optional<Planning> findTopByOrderByIdDesc();

    /**
     * Find plannings in ID range (for batch synchronization)
     */
    @Query("SELECT p FROM Planning p WHERE p.id >= :fromId AND p.id <= :toId")
    List<Planning> findByIdBetween(@Param("fromId") UUID fromId, @Param("toId") UUID toId);
}
