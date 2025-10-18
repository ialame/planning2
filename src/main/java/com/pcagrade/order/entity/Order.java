package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity representing a Pokemon card grading order
 *
 * Simplified version with only fields that exist in Symfony database
 *
 * Inherits ULID primary key from AbstractUlidEntity for:
 * - Chronological ordering of orders (important for priority)
 * - Database synchronization compatibility
 * - Natural sorting by creation time
 */
@Entity
@Table(name = "card_order")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Order extends AbstractUlidEntity {

    // ============================================================
    // SYMFONY SYNCHRONIZATION FIELDS (Read from Symfony API)
    // ============================================================

    /**
     * Original Symfony order ID for synchronization
     */
    @Column(name = "symfony_order_id", unique = true, length = 50)
    private String symfonyOrderId;

    /**
     * Order number (unique identifier)
     */
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * Customer name
     */
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    /**
     * Total number of cards in this order (from Symfony)
     */
    @Column(name = "total_cards")
    private Integer totalCards;

    /**
     * Order price/total (from Symfony)
     */
    @Column(name = "price")
    private Float price;

    /**
     * Delivery priority code from Symfony (X, F+, F, C, E)
     * X = Express (highest priority)
     * F+ = Fast Plus
     * F = Fast
     * C = Classic
     * E = Economy (lowest priority)
     */
    @Column(name = "delai", length = 10)
    private String delai;

    /**
     * Order creation date (from Symfony)
     */
    @Column(name = "date")
    private LocalDateTime date;

    /**
     * Current processing status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status = OrderStatus.PENDING;

    // ============================================================
    // SPRING BOOT PLANNING FIELDS (Managed locally)
    // ============================================================

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();

    @PrePersist
    protected void onOrderCreate() {
        super.onCreate();
        if (date == null) {
            date = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onOrderUpdate() {
        super.onUpdate();
    }

    // ============================================================
    // BUSINESS LOGIC METHODS
    // ============================================================

    /**
     * Get priority score (lower is higher priority)
     * Based on delai code
     */
    public int getPriorityScore() {
        if (delai != null) {
            switch (delai.toUpperCase()) {
                case "X":  return 1; // Highest priority
                case "F+": return 2;
                case "F":  return 3;
                case "C":  return 4;
                case "E":  return 5; // Lowest priority
            }
        }
        return 6; // Unknown priority
    }

    /**
     * Get priority label for display
     */
    public String getPriorityLabel() {
        if (delai != null) {
            switch (delai.toUpperCase()) {
                case "X":  return "🔴 Express";
                case "F+": return "🟠 Fast Plus";
                case "F":  return "🟡 Fast";
                case "C":  return "🟢 Classic";
                case "E":  return "🔵 Economy";
            }
        }
        return "⚪ Unknown";
    }

    /**
     * Get estimated days until delivery based on delai code
     */
    public int getEstimatedDeliveryDays() {
        if (delai != null) {
            switch (delai.toUpperCase()) {
                case "X":  return 2;   // Express - 2 days
                case "F+": return 5;   // Fast Plus - 5 days
                case "F":  return 10;  // Fast - 10 days
                case "C":  return 20;  // Classic - 20 days
                case "E":  return 30;  // Economy - 30 days
            }
        }
        return 20; // Default to Classic
    }

    /**
     * Get card count (prefers totalCards from sync, falls back to cards list)
     */
    public int getCardCount() {
        return totalCards != null ? totalCards : cards.size();
    }

    /**
     * Calculate total estimated processing time in minutes
     * Formula: totalCards * 4 stages * 3 minutes per stage
     */
    public int getEstimatedProcessingMinutes() {
        final int MINUTES_PER_CARD_PER_STAGE = 3;
        final int NUMBER_OF_STAGES = 4; // grading, certifying, scanning, packaging

        int cardCount = getCardCount();
        return cardCount * NUMBER_OF_STAGES * MINUTES_PER_CARD_PER_STAGE;
    }

    /**
     * Calculate estimated delivery date based on order date and delai
     */
    public LocalDateTime getEstimatedDeliveryDate() {
        LocalDateTime baseDate = date != null ? date : LocalDateTime.now();
        return baseDate.plusDays(getEstimatedDeliveryDays());
    }

    /**
     * Check if order is likely overdue (past estimated delivery date)
     */
    public boolean isLikelyOverdue() {
        LocalDateTime estimatedDelivery = getEstimatedDeliveryDate();
        return LocalDateTime.now().isAfter(estimatedDelivery)
                && status != OrderStatus.COMPLETED
                && status != OrderStatus.DELIVERED;
    }
}