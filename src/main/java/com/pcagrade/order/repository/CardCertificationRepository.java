package com.pcagrade.order.repository;

import com.pcagrade.order.entity.CardCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CardCertification entity
 * Handles database operations for card certifications
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
     * Count certifications for an order
     */
    long countByOrderId(UUID orderId);

    /**
     * Find incomplete certifications (not fully processed)
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.gradingCompleted = false OR " +
            "c.certificationCompleted = false OR " +
            "c.scanningCompleted = false OR " +
            "c.packagingCompleted = false")
    List<CardCertification> findIncompleteCertifications();

    /**
     * Find certifications needing grading
     */
    @Query("SELECT c FROM CardCertification c WHERE c.gradingCompleted = false")
    List<CardCertification> findCertificationsNeedingGrading();

    /**
     * Find certifications needing certification
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.gradingCompleted = true AND c.certificationCompleted = false")
    List<CardCertification> findCertificationsNeedingCertification();

    /**
     * Find certifications needing scanning
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.certificationCompleted = true AND c.scanningCompleted = false")
    List<CardCertification> findCertificationsNeedingScanning();

    /**
     * Find certifications needing packaging
     */
    @Query("SELECT c FROM CardCertification c WHERE " +
            "c.scanningCompleted = true AND c.packagingCompleted = false")
    List<CardCertification> findCertificationsNeedingPackaging();

    /**
     * Count completed certifications for an order
     */
    @Query("SELECT COUNT(c) FROM CardCertification c WHERE c.orderId = :orderId AND " +
            "c.gradingCompleted = true AND " +
            "c.certificationCompleted = true AND " +
            "c.scanningCompleted = true AND " +
            "c.packagingCompleted = true")
    long countCompletedByOrderId(@Param("orderId") UUID orderId);
}