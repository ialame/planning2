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

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 100)
    private String customerEmail;

    /**
     * Delivery deadline - determines order priority
     * Orders with earlier deadlines have higher priority
     */
    @Column(name = "delivery_deadline", nullable = false)
    private LocalDateTime deliveryDeadline;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();

    /**
     * Total estimated processing time in minutes
     */
    @Column(name = "estimated_processing_minutes")
    private Integer estimatedProcessingMinutes;

    @PrePersist
    protected void onOrderCreate() {
        super.onCreate();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onOrderUpdate() {
        super.onUpdate();
    }

    /**
     * Calculate total processing time based on cards and processing stages
     */
    public void calculateEstimatedTime() {
        // 3 minutes per card per processing stage
        final int MINUTES_PER_CARD_PER_STAGE = 3;
        int totalCards = cards.size();

        // Count required stages (grading, certifying, scanning, packaging)
        int stages = 4;

        estimatedProcessingMinutes = totalCards * stages * MINUTES_PER_CARD_PER_STAGE;
    }

    /**
     * Get priority score (lower is higher priority)
     * Based on delivery deadline
     */
    public long getPriorityScore() {
        return deliveryDeadline.toEpochSecond(java.time.ZoneOffset.UTC);
    }

    /**
     * Check if order is overdue
     */
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(deliveryDeadline)
                && status != OrderStatus.COMPLETED
                && status != OrderStatus.DELIVERED;
    }
}



