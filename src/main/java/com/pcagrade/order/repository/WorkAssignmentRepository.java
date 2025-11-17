package com.pcagrade.order.repository;


import com.pcagrade.order.entity.AssignmentStatus;
import com.pcagrade.order.entity.WorkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WorkAssignment Repository
 */
@Repository
public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, UUID> {

    List<WorkAssignment> findByEmployeeIdOrderByScheduledStartAsc(UUID employeeId);

    List<WorkAssignment> findByOrderIdOrderByScheduledStartAsc(UUID orderId);

    List<WorkAssignment> findByStatusOrderByPriorityScoreAsc(AssignmentStatus status);

    @Query("SELECT wa FROM WorkAssignment wa WHERE wa.employee.id = :employeeId AND wa.status = :status ORDER BY wa.scheduledStart ASC")
    List<WorkAssignment> findByEmployeeIdAndStatus(UUID employeeId, AssignmentStatus status);

    @Query("SELECT wa FROM WorkAssignment wa WHERE wa.processingStage = :stage AND wa.status = 'SCHEDULED' ORDER BY wa.priorityScore ASC")
    List<WorkAssignment> findScheduledByStage(String stage);

    @Query("SELECT COUNT(wa) FROM WorkAssignment wa WHERE wa.employee.id = :employeeId AND wa.status IN ('SCHEDULED', 'IN_PROGRESS')")
    Long countActiveAssignmentsByEmployee(UUID employeeId);

    // For synchronization - ULID advantage!
    List<WorkAssignment> findByIdGreaterThan(UUID lastSyncId);

    Optional<WorkAssignment> findTopByOrderByIdDesc();

    // Advanced queries leveraging ULID ordering
    @Query("SELECT wa FROM WorkAssignment wa WHERE wa.id >= :fromId AND wa.id <= :toId")
    List<WorkAssignment> findByIdBetween(UUID fromId, UUID toId);

    // Get assignments created in last N hours (using ULID timestamp)
    @Query(value = "SELECT * FROM work_assignment WHERE id > :sinceId", nativeQuery = true)
    List<WorkAssignment> findCreatedSince(UUID sinceId);

    /**
     * Find work assignments by employee and date range, ordered by scheduled start
     */
    List<WorkAssignment> findByEmployeeIdAndScheduledStartBetweenOrderByScheduledStartAsc(
            UUID employeeId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

}