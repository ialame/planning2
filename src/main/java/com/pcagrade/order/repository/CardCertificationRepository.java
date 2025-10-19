package com.pcagrade.order.repository;

import com.pcagrade.order.entity.CardCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for CardCertification entity
 * Handles database operations for card certifications with planning flags
 *
 * UPDATED: Removed findBySymfonyCertificationId and existsBySymfonyCertificationId
 * because we now use Symfony ID directly as primary key (findById does the job)
 */
@Repository
public interface CardCertificationRepository extends JpaRepository<CardCertification, UUID> {

    /**
     * Find all certifications for a specific order
     */
    List<CardCertification> findByOrderId(UUID orderId);

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

    /**
     * Count certifications by order
     */
    long countByOrderId(UUID orderId);

    /**
     * Find certifications that need grading
     */
    List<CardCertification> findByGradingCompletedFalse();

    /**
     * Find certifications that need certification
     */
    List<CardCertification> findByCertificationCompletedFalse();

    /**
     * Find certifications that need scanning
     */
    List<CardCertification> findByScanningCompletedFalse();

    /**
     * Find certifications that need packaging
     */
    List<CardCertification> findByPackagingCompletedFalse();
}