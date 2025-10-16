package com.pcagrade.order.entity;


import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Card entity representing individual Pokemon cards in an order
 *
 * Inherits ULID primary key from AbstractUlidEntity for:
 * - Chronological tracking of card processing
 * - Database synchronization compatibility
 */
@Entity
@Table(name = "pokemon_card")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Card extends AbstractUlidEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "card_name", length = 200)
    private String cardName;

    @Column(name = "card_set", length = 100)
    private String cardSet;

    @Column(name = "card_number", length = 50)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private CardStatus status = CardStatus.PENDING;

    /**
     * Assigned grade after grading process
     */
    @Column(name = "assigned_grade")
    private Double assignedGrade;
}

