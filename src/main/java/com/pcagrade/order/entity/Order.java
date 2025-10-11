package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Order entity representing Pokemon card orders
 * Writable entity - synced from Symfony API and stored locally for planning
 *
 * IMPORTANT: This entity is now WRITABLE to allow syncing from Symfony
 * Data flows: Symfony API → Spring Boot → dev-planning database
 */
@Entity
@Table(name = "`order`")
// @Immutable  ← REMOVED to allow writes
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AbstractUlidEntity {

    /**
     * Symfony order ID (ULID hex format)
     * This is the unique identifier from the Symfony system
     */
    @Column(name = "symfony_order_id", unique = true)
    private String symfonyOrderId;

    /**
     * Unique order number for tracking
     */
    @Column(name = "order_number")
    private String orderNumber;

    /**
     * Customer name (from customer table join in Symfony)
     */
    @Column(name = "customer_name")
    private String customerName;

    /**
     * Delivery date code (actually the delai code: X, F+, F, C, E)
     * X = Express (1 day)
     * F+ = Fast+ (1 week)
     * F = Fast (2 weeks)
     * C = Classic (1 month)
     * E = Economy (3 months)
     */
    @Column(name = "delivery_date", length = 10)
    private String deliveryDate;

    /**
     * Order creation date
     */
    @Column(name = "order_date")
    private LocalDate orderDate;

    /**
     * Total number of cards in this order
     */
    @Column(name = "total_cards")
    private Integer totalCards;

    /**
     * Order status (from Symfony workflow)
     * Status constants for planning reference:
     */
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
     * Price from invoice (total_ttc)
     * Synced from Symfony API: invoice.total_ttc
     */
    @Column(name = "price")
    private Float price;

    /**
     * Delivery priority code
     * Values: X=1day, F+=1week, F=2weeks, C=1month, E=3months
     * Synced from Symfony API: order.delai
     */
    @Column(name = "delai", length = 3)
    private String delai;

    // ============================================================
    // HELPER METHODS FOR PLANNING
    // ============================================================

    /**
     * Get estimated processing time in minutes
     * Based on: 3 minutes per card
     */
    public int getEstimatedProcessingMinutes() {
        if (totalCards == null || totalCards <= 0) {
            return 0;
        }
        return totalCards * 3;
    }

    /**
     * Get priority score for scheduling (higher = more urgent)
     * Based on delai code
     */
    public int getPriorityScore() {
        if (delai == null) return 50;

        return switch (delai) {
            case "X" -> 100;   // Express - Highest priority
            case "F+" -> 80;   // Fast+
            case "F" -> 60;    // Fast
            case "C" -> 40;    // Classic
            case "E" -> 20;    // Economy - Lowest priority
            default -> 50;     // Unknown
        };
    }

    /**
     * Check if order is ready for planning
     * (has been accepted but not yet completed)
     */
    public boolean isReadyForPlanning() {
        return status != null &&
                status >= STATUS_A_NOTER &&
                status < STATUS_ENVOYEE;
    }

    /**
     * Get human-readable delai description
     */
    public String getDelaiDescription() {
        if (delai == null) return "Unknown";

        return switch (delai) {
            case "X" -> "Express (1 day)";
            case "F+" -> "Fast+ (1 week)";
            case "F" -> "Fast (2 weeks)";
            case "C" -> "Classic (1 month)";
            case "E" -> "Economy (3 months)";
            default -> "Unknown (" + delai + ")";
        };
    }
}