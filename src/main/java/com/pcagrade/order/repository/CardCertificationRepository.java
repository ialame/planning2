package com.pcagrade.order.repository;

import com.pcagrade.order.entity.CardCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CardCertification entity
 * Handles database operations for card certifications with planning flags
 */
@Repository
public interface CardCertificationRepository extends JpaRepository<CardCertification, UUID> {

    /**
     * Find certification by Symfony certification ID
     */
    Optional<CardCertification> findBySymfonyCertificationId(String symfonyCertificationId);

    /**
     * Find all certifications for a specific order
     */
    List<CardCertification> findByOrderId(UUID orderId);

    /**
     * Check if certification exists by Symfony ID
     */
    boolean existsBySymfonyCertificationId(String symfonyCertificationId);

    // ============================================================
    // COUNTING METHODS FOR STATISTICS
    // ============================================================

    /**
     * Count cards by grading completion status
     */
    long countByGradingCompleted(boolean completed);

    /**
     * Count cards by certification completion status
     */
    long countByCertificationCompleted(boolean completed);

    /**
     * Count cards by scanning completion status
     */
    long countByScanningCompleted(boolean completed);

    /**
     * Count cards by packaging completion status
     */
    long countByPackagingCompleted(boolean completed);

    /**
     * Count incomplete cards (at least one task not completed)
     */
    @Query("SELECT COUNT(c) FROM CardCertification c WHERE " +
            "c.gradingCompleted = false OR " +
            "c.certificationCompleted = false OR " +
            "c.scanningCompleted = false OR " +
            "c.packagingCompleted = false")
    long countIncompleteCards();

    /**
     * Find incomplete certifications for a specific order
     */
    @Query("SELECT c FROM CardCertification c WHERE c.orderId = :orderId AND (" +
            "c.gradingCompleted = false OR " +
            "c.certificationCompleted = false OR " +
            "c.scanningCompleted = false OR " +
            "c.packagingCompleted = false)")
    List<CardCertification> findIncompleteByOrderId(UUID orderId);

    // ============================================================
    // TASK-SPECIFIC QUERIES
    // ============================================================

    /**
     * Find all cards needing grading
     */
    List<CardCertification> findByGradingCompletedFalse();

    /**
     * Find all cards needing certification
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.gradingCompleted = true AND c.certificationCompleted = false")
    List<CardCertification> findNeedingCertification();

    /**
     * Find all cards needing scanning
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.certificationCompleted = true AND c.scanningCompleted = false")
    List<CardCertification> findNeedingScanning();

    /**
     * Find all cards needing packaging
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.scanningCompleted = true AND c.packagingCompleted = false")
    List<CardCertification> findNeedingPackaging();
}