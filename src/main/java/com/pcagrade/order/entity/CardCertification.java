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

    @Column(name = "custom_label", nullable = false)
    private String customLabel = "";

    @Column(name = "custom_qr_code_url", nullable = false)
    private String customQrCodeUrl = "";

    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "langue", nullable = false)
    private String langue = "FR";

    @Column(name = "langue_mention", nullable = false)
    private String langueMention = "FR";

    @Column(name = "shadowless", nullable = false)
    private Boolean shadowless = false;

    @Column(name = "manuelle", nullable = false)
    private Boolean manuelle = false;

    @Column(name = "descellee", nullable = false)
    private Boolean descellee = false;

    @Column(name = "csn", nullable = false)
    private Boolean csn = false;

    @Column(name = "photo", nullable = false)
    private Boolean photo = false;

    @Column(name = "vd_cc", nullable = false)
    private Integer vdCc = 1;

    // ============================================================
    // OPTIONAL FIELDS
    // ============================================================

    @Column(name = "annotation")
    private Boolean annotation;

    @Column(name = "bug")
    private Boolean bug;

    @Column(name = "edition")
    private Integer edition = 2;

    @Column(name = "foil")
    private Boolean foil = false;

    @Column(name = "multi_grade")
    private Boolean multiGrade;

    @Column(name = "reverse")
    private Boolean reverse = false;

    @Column(name = "type")
    private String type;

    // ============================================================
    // LIFECYCLE HOOKS - Ensures all required fields have values
    // ============================================================

    @PrePersist
    protected void setDefaults() {
        if (date == null) date = LocalDateTime.now();
        if (codeBarre == null) codeBarre = "";
        if (customLabel == null) customLabel = "";
        if (customQrCodeUrl == null) customQrCodeUrl = "";
        if (langue == null) langue = "FR";
        if (langueMention == null) langueMention = "FR";
        if (status == null) status = 0;
        if (deleted == null) deleted = false;
        if (shadowless == null) shadowless = false;
        if (manuelle == null) manuelle = false;
        if (descellee == null) descellee = false;
        if (csn == null) csn = false;
        if (photo == null) photo = false;
        if (vdCc == null) vdCc = 1;

        // Planning flags defaults
        if (gradingCompleted == null) gradingCompleted = false;
        if (certificationCompleted == null) certificationCompleted = true;
        if (scanningCompleted == null) scanningCompleted = false;
        if (packagingCompleted == null) packagingCompleted = false;
    }
}