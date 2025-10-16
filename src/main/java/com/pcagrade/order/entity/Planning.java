package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Planning entity for Pokemon card order processing
 * Uses REAL order status workflow:
 * - Status uses actual order status values (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 41, 42)
 * - Delai uses priority codes (X, F+, F, C, E)
 */
@Entity
@Table(name = "planning",
        indexes = {
                @Index(name = "idx_planning_employee_date", columnList = "employee_id, planning_date"),
                @Index(name = "idx_planning_order", columnList = "order_id"),
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

    // ========== BASIC IDENTIFICATION ==========

    @NotNull(message = "Order ID is required")
    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID orderId;

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

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 720, message = "Duration cannot exceed 12 hours")
    @Column(name = "estimated_duration_minutes", nullable = false)
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    // ========== PRIORITY & STATUS ==========

    /**
     * Priority/Deadline code
     * X = Excelsior, F+ = Fast Plus, F = Fast, C = Classic, E = Economy
     */
    @NotNull(message = "Delai is required")
    @Pattern(regexp = "^(X|F\\+|F|C|E)$", message = "Delai must be: X, F+, F, C, or E")
    @Builder.Default
    @Column(name = "delai", nullable = false, length = 10)
    private String delai = "C";

    /**
     * REAL order status values:
     * 1  = A_RECEPTIONNER (To be received)
     * 2  = A_NOTER (To be graded) - DEFAULT for planning
     * 3  = A_CERTIFIER (To be encapsulated)
     * 4  = A_PREPARER (To be prepared)
     * 5  = ENVOYEE (Sent)
     * 6  = A_VOIR (To be checked)
     * 7  = A_DESCELLER (To be unsealed)
     * 8  = RECU (Received)
     * 9  = COLIS_ACCEPTE (Package accepted)
     * 10 = A_SCANNER (To be scanned)
     * 11 = A_OUVRIR (To be opened)
     * 41 = A_DISTRIBUER (To be delivered)
     * 42 = A_ENVOYER (To be sent)
     */
    @NotNull(message = "Status is required")
    @Builder.Default
    @Column(name = "status", nullable = false)
    private Integer status = 2; // Default: A_NOTER

    @Column(name = "completed")
    private Boolean completed = false;

    // ========== ACTUAL TIMING ==========

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    // ========== CARD COUNTS ==========

    @Min(value = 0, message = "Card count cannot be negative")
    @Column(name = "estimated_card_count")
    @Builder.Default
    private Integer estimatedCardCount = 25;

    @Min(value = 0, message = "Actual card count cannot be negative")
    @Column(name = "actual_card_count")
    private Integer actualCardCount;

    @Min(value = 0, message = "Card count cannot be negative")
    @Column(name = "card_count")
    @Builder.Default
    private Integer cardCount = 0;

    // ========== PROGRESS & NOTES ==========

    @Min(value = 0, message = "Progress cannot be negative")
    @Max(value = 100, message = "Progress cannot exceed 100%")
    @Builder.Default
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    // ========== COSTS ==========

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @DecimalMin(value = "0.0", message = "Actual cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    // ========== TIMESTAMPS ==========

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ========== RELATIONSHIPS ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    private Employee employee;

    // ========== ENUMS FOR ORDER STATUS ==========

    /**
     * Real order status enum matching the actual workflow
     */
    public enum OrderStatus {
        A_RECEPTIONNER(1, "A_RECEPTIONNER", "To be received", "À réceptionner"),
        A_NOTER(2, "A_NOTER", "To be graded", "À noter"),
        A_CERTIFIER(3, "A_CERTIFIER", "To be encapsulated", "À certifier"),
        A_PREPARER(4, "A_PREPARER", "To be prepared", "À préparer"),
        ENVOYEE(5, "ENVOYEE", "Sent", "Envoyée"),
        A_VOIR(6, "A_VOIR", "To be checked", "À voir"),
        A_DESCELLER(7, "A_DESCELLER", "To be unsealed", "À desceller"),
        RECU(8, "RECU", "Received", "Reçu"),
        COLIS_ACCEPTE(9, "COLIS_ACCEPTE", "Package accepted", "Colis accepté"),
        A_SCANNER(10, "A_SCANNER", "To be scanned", "À scanner"),
        A_OUVRIR(11, "A_OUVRIR", "To be opened", "À ouvrir"),
        A_DISTRIBUER(41, "A_DISTRIBUER", "To be delivered", "À distribuer"),
        A_ENVOYER(42, "A_ENVOYER", "To be sent", "À envoyer");

        private final int code;
        private final String statusCode;
        private final String nameEn;
        private final String nameFr;

        OrderStatus(int code, String statusCode, String nameEn, String nameFr) {
            this.code = code;
            this.statusCode = statusCode;
            this.nameEn = nameEn;
            this.nameFr = nameFr;
        }

        public int getCode() { return code; }
        public String getStatusCode() { return statusCode; }
        public String getNameEn() { return nameEn; }
        public String getNameFr() { return nameFr; }

        public static OrderStatus fromCode(int code) {
            for (OrderStatus status : values()) {
                if (status.code == code) return status;
            }
            return A_NOTER; // default
        }

        /**
         * Check if this status is a planning stage (work to be done)
         */
        public boolean isPlanningStage() {
            return this == A_SCANNER || this == A_OUVRIR || this == A_NOTER ||
                    this == A_CERTIFIER || this == A_PREPARER || this == A_DESCELLER ||
                    this == A_VOIR;
        }
    }

    /**
     * Delai priority enum
     */
    public enum DelaiPriority {
        EXCELSIOR("X", "Excelsior", 1),
        FAST_PLUS("F+", "Fast Plus", 2),
        FAST("F", "Fast", 3),
        CLASSIC("C", "Classic", 4),
        ECONOMY("E", "Economy", 5);

        private final String code;
        private final String displayName;
        private final int priorityLevel;

        DelaiPriority(String code, String displayName, int priorityLevel) {
            this.code = code;
            this.displayName = displayName;
            this.priorityLevel = priorityLevel;
        }

        public String getCode() { return code; }
        public String getDisplayName() { return displayName; }
        public int getPriorityLevel() { return priorityLevel; }

        public static DelaiPriority fromCode(String code) {
            if (code == null) return CLASSIC;
            for (DelaiPriority delai : values()) {
                if (delai.code.equals(code)) return delai;
            }
            return CLASSIC;
        }
    }

    // ========== HELPER METHODS ==========

    public OrderStatus getStatusEnum() {
        return OrderStatus.fromCode(this.status);
    }

    public void setStatusEnum(OrderStatus statusEnum) {
        this.status = statusEnum.getCode();
    }

    public DelaiPriority getDelaiEnum() {
        return DelaiPriority.fromCode(this.delai);
    }

    public void setDelaiEnum(DelaiPriority delaiEnum) {
        this.delai = delaiEnum.getCode();
    }

    public String getStatusDisplayName(String lang) {
        OrderStatus statusEnum = getStatusEnum();
        return "fr".equals(lang) ? statusEnum.getNameFr() : statusEnum.getNameEn();
    }

    public String getDelaiDisplayName() {
        return getDelaiEnum().getDisplayName();
    }

    // Status check methods
    public boolean isToBeGraded() { return this.status != null && this.status == 2; }
    public boolean isToBeCertified() { return this.status != null && this.status == 3; }
    public boolean isToBePrepared() { return this.status != null && this.status == 4; }
    public boolean isSent() { return this.status != null && this.status == 5; }
    public boolean isReceived() { return this.status != null && this.status == 8; }
    public boolean isToBeScanned() { return this.status != null && this.status == 10; }
    public boolean isToBeOpened() { return this.status != null && this.status == 11; }

    /**
     * Check if this planning is in a work stage (not finished/sent)
     */
    public boolean isInWorkStage() {
        return getStatusEnum().isPlanningStage();
    }

    // ========== LIFECYCLE CALLBACKS ==========

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.estimatedEndTime == null && this.startTime != null && this.estimatedDurationMinutes != null) {
            this.estimatedEndTime = this.startTime.plusMinutes(this.estimatedDurationMinutes);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.estimatedEndTime == null && this.startTime != null && this.estimatedDurationMinutes != null) {
            this.estimatedEndTime = this.startTime.plusMinutes(this.estimatedDurationMinutes);
        }
    }
}