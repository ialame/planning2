package com.pcagrade.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CardCertification entity - Using Symfony ID as Primary Key
 *
 * IMPORTANT: This entity does NOT inherit from AbstractUlidEntity
 * because we use the Symfony certification ID directly as our primary key.
 *
 * This approach:
 * - Eliminates the need for symfony_certification_id as a separate field
 * - Simplifies synchronization (no ID mapping needed)
 * - Reduces database storage (one less column and index)
 * - Ensures perfect consistency between databases
 */
@Data
@Entity
@Table(name = "card_certification")
public class CardCertification implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // PRIMARY KEY - Using Symfony Certification ID directly
    // ============================================================

    /**
     * Primary key: Uses the Symfony certification ID directly
     * We manually set this ID from the Symfony API data during sync
     *
     * CRITICAL: @JdbcTypeCode ensures UUID is stored as BINARY(16) not VARCHAR
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    // ============================================================
    // PLANNING SYNC FIELDS
    // ============================================================

    @Column(name = "order_id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
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

    @Column(name = "card_id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID cardId;

    @Column(name = "code_barre", nullable = false)
    private String codeBarre = "";

    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "langue", nullable = false)
    private String langue = "FR";

    /**
     * Creation timestamp
     */
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    /**
     * Last modification timestamp
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ============================================================
    // LIFECYCLE HOOKS - Ensures all required fields have values
    // ============================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (creationDate == null) creationDate = now;
        if (modificationDate == null) modificationDate = now;
        if (date == null) date = now;
        if (codeBarre == null) codeBarre = "";
        if (langue == null) langue = "FR";
        if (status == null) status = 0;

        // Planning flags defaults
        if (gradingCompleted == null) gradingCompleted = false;
        if (certificationCompleted == null) certificationCompleted = true;
        if (scanningCompleted == null) scanningCompleted = false;
        if (packagingCompleted == null) packagingCompleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CardCertification)) return false;
        CardCertification that = (CardCertification) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}