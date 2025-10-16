package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WorkAssignment entity representing task assignments to employees
 * Tracks which employee is assigned which cards to process
 *
 * Inherits ULID primary key from AbstractUlidEntity for:
 * - Chronological ordering of assignments
 * - Database synchronization compatibility
 * - Natural sorting by creation time
 */
@Entity
@Table(name = "work_assignment", indexes = {
        @Index(name = "idx_employee_status", columnList = "employee_id, status"),
        @Index(name = "idx_order_id", columnList = "order_id"),
        @Index(name = "idx_scheduled_start", columnList = "scheduled_start"),
        @Index(name = "idx_status_priority", columnList = "status, priority_score")
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkAssignment extends AbstractUlidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Processing stage: GRADING, CERTIFYING, SCANNING, PACKAGING
     */
    @Column(name = "processing_stage", nullable = false, length = 50)
    private String processingStage;

    /**
     * Number of cards assigned in this task
     */
    @Column(name = "card_count", nullable = false)
    private Integer cardCount;

    /**
     * Estimated duration in minutes (cardCount * 3)
     */
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    /**
     * Scheduled start time
     */
    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    /**
     * Scheduled end time
     */
    @Column(name = "scheduled_end")
    private LocalDateTime scheduledEnd;

    /**
     * Actual start time
     */
    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    /**
     * Actual end time
     */
    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private AssignmentStatus status = AssignmentStatus.SCHEDULED;

    @Column(name = "priority_score")
    private Long priorityScore;

    @PrePersist
    protected void onAssignmentCreate() {
        super.onCreate();

        // Calculate duration: 3 minutes per card
        if (estimatedDurationMinutes == null && cardCount != null) {
            estimatedDurationMinutes = cardCount * 3;
        }

        // Calculate scheduled end from start + duration
        if (scheduledStart != null && scheduledEnd == null && estimatedDurationMinutes != null) {
            scheduledEnd = scheduledStart.plusMinutes(estimatedDurationMinutes);
        }
    }

    @PreUpdate
    protected void onAssignmentUpdate() {
        super.onUpdate();
    }

    /**
     * Mark assignment as started
     */
    public void start() {
        this.actualStart = LocalDateTime.now();
        this.status = AssignmentStatus.IN_PROGRESS;
    }

    /**
     * Mark assignment as completed
     */
    public void complete() {
        this.actualEnd = LocalDateTime.now();
        this.status = AssignmentStatus.COMPLETED;
    }

    /**
     * Check if assignment is late (started after scheduled time)
     */
    public boolean isLateStart() {
        return actualStart != null
                && scheduledStart != null
                && actualStart.isAfter(scheduledStart);
    }

    /**
     * Check if assignment finished late
     */
    public boolean isLateCompletion() {
        return actualEnd != null
                && scheduledEnd != null
                && actualEnd.isAfter(scheduledEnd);
    }

    /**
     * Get actual duration in minutes
     */
    public Integer getActualDurationMinutes() {
        if (actualStart == null || actualEnd == null) {
            return null;
        }
        return (int) java.time.Duration.between(actualStart, actualEnd).toMinutes();
    }
}

