// ===================================================================
// CardRepository.java
// ===================================================================
package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Card entity
 * Handles database operations for Pokemon cards
 */
@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Find all cards for a specific order
     */
    List<Card> findByOrderId(Long orderId);

    /**
     * Find cards by processing status
     */
    List<Card> findByProcessingStatus(String status);

    /**
     * Delete all cards for a specific order
     * Used when re-syncing order cards
     */
    @Modifying
    @Query("DELETE FROM Card c WHERE c.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    /**
     * Check if a card exists by Symfony card ID
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
            "FROM Card c WHERE c.symfonyCardId = :cardId")
    boolean existsBySymfonyCardId(@Param("cardId") String cardId);

    /**
     * Find card by Symfony card ID
     */
    Optional<Card> findBySymfonyCardId(String cardId);

    /**
     * Count cards by order
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.order.id = :orderId")
    long countByOrderId(@Param("orderId") Long orderId);

    /**
     * Count completed cards for an order
     */
    @Query("SELECT COUNT(c) FROM Card c WHERE c.order.id = :orderId " +
            "AND c.gradingCompleted = true " +
            "AND c.certificationCompleted = true " +
            "AND c.scanningCompleted = true " +
            "AND c.packagingCompleted = true")
    long countCompletedCardsByOrderId(@Param("orderId") Long orderId);

    /**
     * Find cards pending specific processing step
     */
    @Query("SELECT c FROM Card c WHERE c.order.id = :orderId AND c.gradingCompleted = false")
    List<Card> findPendingGradingByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT c FROM Card c WHERE c.order.id = :orderId AND c.certificationCompleted = false")
    List<Card> findPendingCertificationByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT c FROM Card c WHERE c.order.id = :orderId AND c.scanningCompleted = false")
    List<Card> findPendingScanningByOrderId(@Param("orderId") Long orderId);
}

