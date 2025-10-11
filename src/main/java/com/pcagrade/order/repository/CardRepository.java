package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Card entity
 *
 * IMPORTANT: Card is a READ-ONLY catalog table shared with other projects.
 * It does NOT have a direct relationship with Order entity.
 *
 * Card-to-Order relationships are managed through:
 * - card_certification (CardCertification entity)
 * - card_certification_order (junction table)
 *
 * This repository provides ONLY basic catalog lookup operations.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Find card by discriminator (card type/series identifier)
     */
    Optional<Card> findByDiscriminator(String discriminator);

    /**
     * Find card by number within a discriminator
     */
    @Query("SELECT c FROM Card c WHERE c.discriminator = :discriminator AND c.num = :num")
    Optional<Card> findByDiscriminatorAndNum(
            @Param("discriminator") String discriminator,
            @Param("num") String num
    );

    /**
     * Check if a card exists by discriminator
     */
    boolean existsByDiscriminator(String discriminator);

    /**
     * Count all cards in catalog
     */
    @Query("SELECT COUNT(c) FROM Card c")
    long countAllCards();
}