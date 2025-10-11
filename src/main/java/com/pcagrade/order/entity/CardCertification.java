package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;
// @Immutable REMOVED - entity is now writable for sync

import java.time.Instant;
import java.util.UUID;

/**
 * CardCertification entity
 * NOW WRITABLE - synced from Symfony API for planning
 *
 * Represents a certified card within an order.
 * Contains both certification details and planning flags.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "card_certification")
public class CardCertification extends AbstractUlidEntity {

    // ============================================================
    // NEW FIELDS FOR PLANNING (added by Spring Boot sync)
    // ============================================================

    /**
     * Symfony certification ID (ULID hex format)
     * Used to identify certifications from Symfony
     */
    @Column(name = "symfony_certification_id", unique = true)
    private String symfonyCertificationId;

    /**
     * Order ID reference (for faster queries)
     * Denormalized from card_certification_order junction table
     */
    @Column(name = "order_id")
    private UUID orderId;

    /**
     * Card name (denormalized for display)
     * Example: "Pikachu 25/102"
     */
    @Column(name = "card_name")
    private String cardName;

    /**
     * Grading task completed flag
     */
    @Column(name = "grading_completed")
    @ColumnDefault("0")
    private Boolean gradingCompleted = false;

    /**
     * Certification task completed flag
     */
    @Column(name = "certification_completed")
    @ColumnDefault("1")
    private Boolean certificationCompleted = true;

    /**
     * Scanning task completed flag
     */
    @Column(name = "scanning_completed")
    @ColumnDefault("0")
    private Boolean scanningCompleted = false;

    /**
     * Packaging task completed flag
     */
    @Column(name = "packaging_completed")
    @ColumnDefault("0")
    private Boolean packagingCompleted = false;

    // ============================================================
    // EXISTING FIELDS (from original table structure)
    // ============================================================

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @Column(name = "date", nullable = false)
    private Instant date;

    @Size(max = 13)
    @NotNull
    @Column(name = "code_barre", nullable = false, length = 13)
    private String codeBarre;

    @ColumnDefault("0")
    @Column(name = "reverse")
    private Boolean reverse;

    @ColumnDefault("2")
    @Column(name = "edition")
    private Integer edition;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "shadowless", nullable = false)
    private Boolean shadowless = false;

    @Size(max = 255)
    @NotNull
    @ColumnDefault("'FR'")
    @Column(name = "langue", nullable = false)
    private String langue;

    @Column(name = "annotation")
    private Boolean annotation;

    @Column(name = "bug")
    private Boolean bug;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "manuelle", nullable = false)
    private Boolean manuelle = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "descellee", nullable = false)
    private Boolean descellee = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "csn", nullable = false)
    private Boolean csn = false;

    @Size(max = 10)
    @Column(name = "type", length = 10)
    private String type;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "photo", nullable = false)
    private Boolean photo = false;

    @Size(max = 2)
    @NotNull
    @ColumnDefault("'FR'")
    @Column(name = "langue_mention", nullable = false, length = 2)
    private String langueMention;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "vd_cc", nullable = false)
    private Integer vdCc;

    @ColumnDefault("0")
    @Column(name = "foil")
    private Boolean foil;

    @NotNull
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status;

    @Size(max = 255)
    @NotNull
    @Column(name = "custom_qr_code_url", nullable = false)
    private String customQrCodeUrl;

    @Size(max = 255)
    @NotNull
    @Column(name = "custom_label", nullable = false)
    private String customLabel;

    @Column(name = "multi_grade")
    private Boolean multiGrade;

    // ============================================================
    // HELPER METHODS FOR PLANNING
    // ============================================================

    /**
     * Get estimated processing time in minutes for remaining tasks
     * Based on: 3 minutes per task
     */
    public int getRemainingProcessingMinutes() {
        int minutes = 0;

        if (!Boolean.TRUE.equals(gradingCompleted)) {
            minutes += 3; // Grading: 3 minutes
        }
        if (!Boolean.TRUE.equals(certificationCompleted)) {
            minutes += 3; // Certification: 3 minutes
        }
        if (!Boolean.TRUE.equals(scanningCompleted)) {
            minutes += 3; // Scanning: 3 minutes
        }
        if (!Boolean.TRUE.equals(packagingCompleted)) {
            minutes += 3; // Packaging: 3 minutes
        }

        return minutes;
    }

    /**
     * Check if all processing is complete
     */
    public boolean isFullyProcessed() {
        return Boolean.TRUE.equals(gradingCompleted) &&
                Boolean.TRUE.equals(certificationCompleted) &&
                Boolean.TRUE.equals(scanningCompleted) &&
                Boolean.TRUE.equals(packagingCompleted);
    }

    /**
     * Get completion percentage (0-100)
     */
    public int getCompletionPercentage() {
        int completedTasks = 0;
        int totalTasks = 4;

        if (Boolean.TRUE.equals(gradingCompleted)) completedTasks++;
        if (Boolean.TRUE.equals(certificationCompleted)) completedTasks++;
        if (Boolean.TRUE.equals(scanningCompleted)) completedTasks++;
        if (Boolean.TRUE.equals(packagingCompleted)) completedTasks++;

        return (completedTasks * 100) / totalTasks;
    }

    /**
     * Get next required task
     */
    public String getNextTask() {
        if (!Boolean.TRUE.equals(gradingCompleted)) {
            return "GRADING";
        }
        if (!Boolean.TRUE.equals(certificationCompleted)) {
            return "CERTIFICATION";
        }
        if (!Boolean.TRUE.equals(scanningCompleted)) {
            return "SCANNING";
        }
        if (!Boolean.TRUE.equals(packagingCompleted)) {
            return "PACKAGING";
        }
        return "COMPLETED";
    }
}