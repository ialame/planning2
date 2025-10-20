package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Planning entity for Pokemon card order processing
 * UPDATED: Compatible with Symfony synchronization
 *
 * Key changes:
 * - Uses symfony_order_id (VARCHAR) instead of order_id (UUID)
 * - References card_order table from Symfony
 * - Maintains employee reference as UUID
 */
@Entity
@Table(name = "planning",
        indexes = {
                @Index(name = "idx_planning_employee_date", columnList = "employee_id, planning_date"),
                @Index(name = "idx_planning_symfony_order", columnList = "symfony_order_id"),
                @Index(name = "idx_planning_date", columnList = "planning_date"),
                @Index(name = "idx_planning_status", columnList = "status"),
                @Index(name = "idx_planning_delai", columnList = "delai"),
                @Index(name = "idx_planning_start_time", columnList = "start_time"),
                @Index(name = "idx_planning_employee_time", columnList = "employee_id, planning_date, start_time")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Planning extends AbstractUlidEntity {

    // ========== ORDER REFERENCE - UPDATED ==========

    /**
     * Reference to Symfony order (card_order.id)
     * CHANGED: Now VARCHAR(255) to match Symfony ULID string format
     * Previously: order_id UUID BINARY(16)
     */
    @NotNull(message = "Symfony Order ID is required")
    @Column(name = "symfony_order_id", nullable = false, length = 255)
    private String symfonyOrderId;

    // ========== EMPLOYEE REFERENCE ==========

    /**
     * Reference to Spring Boot employee (employee.id)
     * Remains as UUID BINARY(16)
     */
    @NotNull(message = "Employee ID is required")
    @Column(name = "employee_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID employeeId;

    // ========== TIMING ==========

    @NotNull(message = "Planning date is required")
    @Column(name = "planning_date", nullable = false)
    private LocalDate planningDate;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    // ========== STATUS & PRIORITY ==========

    /**
     * Order status from Symfony (integer codes):
     * 1  = A_RECEPTIONNER (To be received)
     * 2  = A_NOTER (To be graded)
     * 3  = A_CERTIFIER (To be certified/encapsulated)
     * 4  = A_PREPARER (To be prepared for shipping)
     * 5  = ENVOYEE (Sent/Delivered)
     * 10 = A_SCANNER (To be scanned)
     * 41 = EN_COURS_NOTATION (Grading in progress)
     * 42 = A_ENVOYER (Ready to send)
     */
    @Column(name = "status")
    private Integer status;

    /**
     * Priority code from Symfony (delai):
     * X  = Express (highest priority)
     * F+ = Fast Plus
     * F  = Fast
     * C  = Classic (default)
     * E  = Economy (lowest priority)
     */
    @Column(name = "delai", length = 10)
    private String delai;

    // ========== WORKLOAD ==========

    /**
     * Number of cards to process in this planning
     */
    @Min(value = 1, message = "Card count must be at least 1")
    @Column(name = "card_count")
    private Integer cardCount;

    // ========== COMPUTED PROPERTIES ==========

    /**
     * Calculate end time based on start time and duration
     *
     * @return Calculated end time or null if data missing
     */
    public LocalDateTime getEndTime() {
        if (startTime == null || durationMinutes == null) {
            return null;
        }
        return startTime.plusMinutes(durationMinutes);
    }

    /**
     * Get priority score for sorting (lower = higher priority)
     * Used for scheduling optimization
     *
     * @return Priority score (1-5, where 1 is highest)
     */
    public Integer getPriorityScore() {
        if (delai == null || delai.isEmpty()) {
            return 4; // Default: Classic
        }

        return switch (delai) {
            case "X" -> 1;   // Express
            case "F+" -> 2;  // Fast Plus
            case "F" -> 3;   // Fast
            case "C" -> 4;   // Classic
            case "E" -> 5;   // Economy
            default -> 4;    // Unknown -> Classic
        };
    }

    /**
     * Get human-readable priority name
     *
     * @return Priority name in English
     */
    public String getPriorityName() {
        if (delai == null || delai.isEmpty()) {
            return "Classic";
        }

        return switch (delai) {
            case "X" -> "Express";
            case "F+" -> "Fast Plus";
            case "F" -> "Fast";
            case "C" -> "Classic";
            case "E" -> "Economy";
            default -> "Classic";
        };
    }

    /**
     * Get human-readable status name
     *
     * @return Status name in English
     */
    public String getStatusName() {
        if (status == null) {
            return "Unknown";
        }

        return switch (status) {
            case 1 -> "To be received";
            case 2 -> "To be graded";
            case 3 -> "To be certified";
            case 4 -> "To be prepared";
            case 5 -> "Sent/Delivered";
            case 10 -> "To be scanned";
            case 41 -> "Grading in progress";
            case 42 -> "Ready to send";
            default -> "Unknown (" + status + ")";
        };
    }

    /**
     * Check if planning is for grading work
     *
     * @return true if status indicates grading phase
     */
    public boolean isGradingWork() {
        return status != null && (status == 2 || status == 41);
    }

    /**
     * Check if planning is for certification work
     *
     * @return true if status indicates certification phase
     */
    public boolean isCertificationWork() {
        return status != null && status == 3;
    }

    /**
     * Check if planning is for scanning work
     *
     * @return true if status indicates scanning phase
     */
    public boolean isScanningWork() {
        return status != null && status == 10;
    }

    /**
     * Check if planning is for packaging work
     *
     * @return true if status indicates packaging phase
     */
    public boolean isPackagingWork() {
        return status != null && status == 4;
    }

    /**
     * Calculate total work hours
     *
     * @return Hours as decimal (e.g., 1.5 hours)
     */
    public Double getTotalHours() {
        if (durationMinutes == null) {
            return 0.0;
        }
        return durationMinutes / 60.0;
    }

    // ========== VALIDATION ==========

    @PrePersist
    @PreUpdate
    protected void validatePlanning() {
        // Validate symfony_order_id format (basic check)
        if (symfonyOrderId != null && !symfonyOrderId.matches("[0-9A-Za-z]+")) {
            throw new IllegalArgumentException("Invalid Symfony Order ID format");
        }

        // Validate delai code
        if (delai != null && !delai.matches("^(X|F\\+|F|C|E)$")) {
            throw new IllegalArgumentException("Invalid delai code: " + delai);
        }

        // Validate status code
        if (status != null) {
            if (status < 1 || status > 50) {
                throw new IllegalArgumentException("Invalid status code: " + status);
            }
        }

        // Auto-calculate duration if missing but cardCount exists
        if (durationMinutes == null && cardCount != null) {
            durationMinutes = cardCount * 3; // 3 minutes per card
        }
    }
}