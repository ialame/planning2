package com.pcagrade.order.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.pcagrade.order.util.AbstractUlidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Order entity representing Pokemon card orders
 * Translated from Commande to Order with enhanced functionality
 */
@Entity
@Table(name = "`order`")
@Immutable  // ✅ Indique que l'entité ne doit jamais être modifiée
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AbstractUlidEntity {

    /**
     * Unique order number for tracking
     */
    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number must not exceed 50 characters")
    @Column(name = "order_number", insertable = false, updatable = false)
    private String orderNumber;

    /**
     * Number of cards in this order
     */
    @NotNull(message = "Card count is required")
    @Positive(message = "Card count must be positive")
    @Min(value = 1, message = "Minimum 1 card per order")
    @Max(value = 10000, message = "Maximum 10000 cards per order")
    @Column(name = "card_count", insertable = false, updatable = false)
    private Integer cardCount;

    /**
     * Order priority level
     */
    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private OrderPriority priority = OrderPriority.FAST;


    /**
     * Deadline/Priority code from database (X, F, F+, C, E)
     * This maps to the OrderPriority enum
     */
    @Column(name = "delai", length = 10)
    private String delai;


    /**
     * Total price of the order (removed precision and scale for Double)
     */
    @DecimalMin(value = "0.0", message = "Total price cannot be negative")
    @Column(name = "total_price")
    private Double totalPrice;

    /**
     * Estimated processing time in minutes
     */
    @Min(value = 1, message = "Estimated time must be at least 1 minute")
    @Column(name = "estimated_time_minutes")
    private Integer estimatedTimeMinutes;

    /**
     * Customer name or identifier
     */
    @Size(max = 255, message = "Customer name must not exceed 255 characters")
    @Column(name = "customer_name", length = 255)
    private String customerName;

    /**
     * Order status
     */
    // Constantes de statut
    public static final int STATUS_A_RECEPTIONNER = 1;
    public static final int STATUS_COLIS_ACCEPTE = 9;
    public static final int STATUS_A_SCANNER = 10;
    public static final int STATUS_A_OUVRIR = 11;
    public static final int STATUS_A_NOTER = 2;
    public static final int STATUS_A_CERTIFIER = 3;
    public static final int STATUS_A_PREPARER = 4;
    public static final int STATUS_A_DESCELLER = 7;
    public static final int STATUS_A_VOIR = 6;
    public static final int STATUS_A_DISTRIBUER = 41;
    public static final int STATUS_A_ENVOYER = 42;
    public static final int STATUS_ENVOYEE = 5;
    public static final int STATUS_RECU = 8;

    @Column(name = "status")
    private Integer status;

    /**
     * Date when the order was placed
     */
    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();

    /**
     * Deadline for completing this order
     */
    @Column(name = "deadline_date")
    private LocalDateTime deadlineDate;

    /**
     * When processing started
     */
    @Column(name = "processing_start_date")
    private LocalDateTime processingStartDate;

    /**
     * When processing ended
     */
    @Column(name = "processing_end_date")
    private LocalDateTime processingEndDate;

    /**
     * Additional notes or special instructions
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Record creation timestamp
     */
    @Column(name = "creation_date", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now();

    /**
     * Record last modification timestamp
     */
    @Column(name = "modification_date", nullable = false)
    @Builder.Default
    private LocalDateTime modificationDate = LocalDateTime.now();

    // ========== ENUMS ==========

    /**
     * Order priority levels
     */
    public enum OrderPriority {
        EXCELSIOR,     // 1 week - price >= 1000€
        FAST_PLUS,   // 2 weeks - price >= 500€
        FAST,      // 4 weeks - price < 500€
        CLASSIC    // 8 weeks - price < 100€
    }


    // ========== LIFECYCLE HOOKS ==========

    /**
     * Set creation date before persist
     */
    @PrePersist
    protected void onCreate() {

    }

    /**
     * Update modification date before update
     */
    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = LocalDateTime.now();
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Calculate estimated processing time based on card count
     * Default: 3 minutes per card
     */
    public void calculateEstimatedTime() {
        if (this.cardCount != null) {
            this.estimatedTimeMinutes = this.cardCount * 3; // 3 minutes per card
        }
    }


    /**
     * Check if order is high priority
     */
    public boolean isHighPriority() {
        return priority == OrderPriority.FAST_PLUS;
    }


    /**
     * Get formatted priority display
     */
    public String getPriorityDisplay() {
        return switch (priority) {
            case EXCELSIOR -> "Priorité Excelsior";
            case FAST_PLUS -> "Priorité Fast+";
            case FAST -> "Priorité Fast";
            case CLASSIC -> "Priorité Classique";
        };
    }

    /**
     * Calculate estimated end date based on start date and processing time
     */
    public LocalDateTime getEstimatedEndDate() {
        if (processingStartDate != null && estimatedTimeMinutes != null) {
            return processingStartDate.plusMinutes(estimatedTimeMinutes);
        }
        return null;
    }

    /**
     * Get actual processing duration in minutes
     */
    public Integer getActualProcessingTimeMinutes() {
        if (processingStartDate != null && processingEndDate != null) {
            return (int) java.time.Duration.between(processingStartDate, processingEndDate).toMinutes();
        }
        return null;
    }


    // Helper methods pour lisibilité
    public String getStatusText() {
        return switch (this.status) {
            case STATUS_A_RECEPTIONNER -> "To be received";
            case STATUS_COLIS_ACCEPTE -> "Package accepted";
            case STATUS_A_SCANNER -> "To be scanned";
            case STATUS_A_OUVRIR -> "To be opened";
            case STATUS_A_NOTER -> "To be evaluated";
            case STATUS_A_CERTIFIER -> "To be encapsulated";
            case STATUS_A_PREPARER -> "To be prepared";
            case STATUS_A_DESCELLER -> "To be unsealed";
            case STATUS_A_VOIR -> "To be seen";
            case STATUS_A_DISTRIBUER -> "To be delivered";
            case STATUS_A_ENVOYER -> "To be sent";
            case STATUS_ENVOYEE -> "Sent";
            case STATUS_RECU -> "Received";
            default -> "Unknown";
        };
    }

    public boolean canBeAssigned() {
        return this.status != null &&
                this.status != STATUS_ENVOYEE &&
                this.status != STATUS_RECU;
    }

    public void markAsScheduled() {
        this.status = STATUS_A_PREPARER; // Ou le statut approprié
    }

    public void markAsCompleted() {
        this.status = STATUS_ENVOYEE;
    }

    // Add this enum inside the Order class after the OrderPriority enum

    /**
     * Order status enumeration corresponding to database integer values
     */
    public enum OrderStatus {
        PENDING(STATUS_A_RECEPTIONNER, "To be received"),
        PACKAGE_ACCEPTED(STATUS_COLIS_ACCEPTE, "Package accepted"),
        TO_SCAN(STATUS_A_SCANNER, "To be scanned"),
        TO_OPEN(STATUS_A_OUVRIR, "To be opened"),
        TO_EVALUATE(STATUS_A_NOTER, "To be evaluated"),
        TO_CERTIFY(STATUS_A_CERTIFIER, "To be encapsulated"),
        TO_PREPARE(STATUS_A_PREPARER, "To be prepared"),
        TO_UNSEAL(STATUS_A_DESCELLER, "To be unsealed"),
        TO_SEE(STATUS_A_VOIR, "To be seen"),
        TO_DISTRIBUTE(STATUS_A_DISTRIBUER, "To be delivered"),
        TO_SEND(STATUS_A_ENVOYER, "To be sent"),
        SENT(STATUS_ENVOYEE, "Sent"),
        RECEIVED(STATUS_RECU, "Received");

        private final int code;
        private final String displayName;

        OrderStatus(int code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public int getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Get OrderStatus from integer code
         */
        public static OrderStatus fromCode(int code) {
            for (OrderStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status code: " + code);
        }

        /**
         * Check if status indicates the order can be assigned to planning
         */
        public boolean canBeAssigned() {
            return this != SENT && this != RECEIVED;
        }

        /**
         * Check if status indicates the order is completed
         */
        public boolean isCompleted() {
            return this == SENT || this == RECEIVED;
        }
    }

// Add these helper methods to work with both enum and integer status

    /**
     * Get status as enum (for business logic)
     */
    public OrderStatus getStatusEnum() {
        if (status == null) {
            return OrderStatus.PENDING;
        }
        return OrderStatus.fromCode(status);
    }

    /**
     * Set status from enum (for business logic)
     */
    public void setStatusEnum(OrderStatus orderStatus) {
        this.status = orderStatus.getCode();
    }

    /**
     * Get formatted status display text
     */
    public String getStatusDisplayText() {
        return getStatusEnum().getDisplayName();
    }

    /**
     * Map delai column value to OrderPriority enum
     * @param delaiValue value from delai column (X, F, F+, C, E)
     * @return corresponding OrderPriority enum
     */
    public static OrderPriority mapDelaiToPriority(String delaiValue) {
        if (delaiValue == null || delaiValue.trim().isEmpty()) {
            return OrderPriority.FAST; // Default
        }

        String delai = delaiValue.trim().toUpperCase();

        switch (delai) {
            case "X":
                return OrderPriority.EXCELSIOR;
            case "F+":
            case "F PLUS":
            case "FPLUS":
                return OrderPriority.FAST_PLUS;
            case "F":
            case "FAST":
                return OrderPriority.FAST;
            case "C":
            case "E":
            case "CLASSIC":
            case "ECONOMY":
                return OrderPriority.CLASSIC;
            default:
                return OrderPriority.FAST; // Default fallback
        }
    }

    /**
     * Map OrderPriority enum to delai column value
     * @param priority OrderPriority enum
     * @return corresponding delai value for database
     */
    public static String mapPriorityToDelai(OrderPriority priority) {
        if (priority == null) {
            return "F"; // Default
        }

        switch (priority) {
            case EXCELSIOR:
                return "X";
            case FAST_PLUS:
                return "F+";
            case FAST:
                return "F";
            case CLASSIC:
                return "C";
            default:
                return "F"; // Default fallback
        }
    }



    /**
     * Get the priority enum based on delai value
     */
    public OrderPriority getPriorityFromDelai() {
        return mapDelaiToPriority(this.delai);
    }

    /**
     * Set delai based on priority enum
     */
    public void setDelaiFromPriority(OrderPriority priority) {
        this.delai = mapPriorityToDelai(priority);
    }


}