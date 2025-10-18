package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CardCertification entity - Minimal version for sync
 * Only includes fields needed for planning + required DB fields with defaults
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "card_certification")
public class CardCertification extends AbstractUlidEntity {

    // ============================================================
    // PLANNING SYNC FIELDS
    // ============================================================

    @Column(name = "symfony_certification_id", unique = true)
    private String symfonyCertificationId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "grading_completed")
    @ColumnDefault("0")
    private Boolean gradingCompleted = false;

    @Column(name = "certification_completed")
    @ColumnDefault("1")
    private Boolean certificationCompleted = true;

    @Column(name = "scanning_completed")
    @ColumnDefault("0")
    private Boolean scanningCompleted = false;

    @Column(name = "packaging_completed")
    @ColumnDefault("0")
    private Boolean packagingCompleted = false;

    // ============================================================
    // REQUIRED DB FIELDS (with defaults from @PrePersist)
    // ============================================================

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "code_barre", nullable = false)
    private String codeBarre = "";


    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();


    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "langue", nullable = false)
    private String langue = "FR";



    // ============================================================
    // LIFECYCLE HOOKS - Ensures all required fields have values
    // ============================================================

    @PrePersist
    protected void setDefaults() {
        if (date == null) date = LocalDateTime.now();
        if (codeBarre == null) codeBarre = "";
        if (langue == null) langue = "FR";
        if (status == null) status = 0;

        // Planning flags defaults
        if (gradingCompleted == null) gradingCompleted = false;
        if (certificationCompleted == null) certificationCompleted = true;
        if (scanningCompleted == null) scanningCompleted = false;
        if (packagingCompleted == null) packagingCompleted = false;
    }
}