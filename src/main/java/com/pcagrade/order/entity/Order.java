package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

/**
 * Order entity representing Pokemon card orders
 * Read-only entity - synced from Symfony database
 */
@Entity
@Table(name = "`order`")
@Immutable  // ✅ Read-only entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends AbstractUlidEntity {

    /**
     * Symfony order ID (ULID hex format)
     */
    @Column(name = "symfony_order_id", unique = true, insertable = false, updatable = false)
    private String symfonyOrderId;

    /**
     * Unique order number for tracking
     */
    @Column(name = "order_number", insertable = false, updatable = false)
    private String orderNumber;

    /**
     * Customer name (from customer table join)
     */
    @Column(name = "customer_name", insertable = false, updatable = false)
    private String customerName;

    /**
     * Delivery date code (actually the delai code: X, F+, F, C, E)
     */
    @Column(name = "delivery_date", insertable = false, updatable = false)
    private String deliveryDate;

    /**
     * Order creation date
     */
    @Column(name = "order_date", insertable = false, updatable = false)
    private LocalDate orderDate;

    /**
     * Total number of cards in this order
     */
    @Column(name = "total_cards", insertable = false, updatable = false)
    private Integer totalCards;

    /**
     * Order status
     * 1=new, 2=processing, 3=ready, 4=shipped, 5=completed, 6=cancelled, 8=archived, 41=pending
     */

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


    @Column(name = "status", insertable = false, updatable = false)
    private Integer status;

    /**
     * Price from invoice (total_ttc)
     * NEW FIELD - Synced from Symfony API
     */
    @Column(name = "price", insertable = false, updatable = false)
    private Float price;

    /**
     * Delivery priority code
     * NEW FIELD - Replaces priority
     * Values: X=1day, F+=1week, F=2weeks, C=1month, E=3months
     */
    @Column(name = "delai", length = 3, insertable = false, updatable = false)
    private String delai;

    // ============================================================
    // DEPRECATED FIELDS - Remove after migration
    // ============================================================

    // @Column(name = "priority", insertable = false, updatable = false)
    // private Integer priority;  // ← DEPRECATED, use delai instead

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Get numeric priority from delai code
     * X=1, F+=2, F=3, C=4, E=5
     */
    public int getDelaiNumber() {
        if (delai == null) return 99;
        return switch (delai) {
            case "X" -> 1;
            case "F+" -> 2;
            case "F" -> 3;
            case "C" -> 4;
            case "E" -> 5;
            default -> 99;
        };
    }

    /**
     * Get human-readable priority description
     */
    public String getDelaiDescription() {
        if (delai == null) return "Unknown";
        return switch (delai) {
            case "X" -> "Express (1 day)";
            case "F+" -> "Fast+ (1 week)";
            case "F" -> "Fast (2 weeks)";
            case "C" -> "Classic (1 month)";
            case "E" -> "Economy (3 months)";
            default -> "Unknown";
        };
    }
    /**
     * Order priority levels
     */
    public enum OrderDelai {
        EXCELSIOR,     // 1 week - price >= 1000€
        FAST_PLUS,   // 2 weeks - price >= 500€
        FAST,      // 4 weeks - price < 500€
        CLASSIC    // 8 weeks - price < 100€
    }
    /**
     * Check if order is completed
     */
    public boolean isCompleted() {
        return status != null && (status == 5 || status == 6 || status == 8);
    }

    /**
     * Check if order is active (not completed or cancelled)
     */
    public boolean isActive() {
        return !isCompleted();
    }

    /**
     * Get formatted price with currency
     */
    public String getFormattedPrice() {
        if (price == null) return "0.00 €";
        return String.format("%.2f €", price);
    }
}
