package com.pcagrade.order.entity.ulid;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract base entity with ULID primary key
 *
 * All entities inherit from this class to ensure:
 * - Consistent ID generation using ULID
 * - Chronological ordering capability
 * - Database synchronization compatibility
 * - Audit timestamps (creation and modification)
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractUlidEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key using ULID
     * - Stored as BINARY(16) for optimal storage
     * - Lexicographically sortable (chronological)
     * - Compatible with UUID type in Java
     */
    @Id
    @GeneratedValue(generator = "ulid-generator")
    @GenericGenerator(
            name = "ulid-generator",
            strategy = "com.pcagrade.order.util.UlidGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    /**
     * Creation timestamp
     * Automatically set on entity creation
     */
    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    /**
     * Last modification timestamp
     * Automatically updated on entity modification
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    /**
     * JPA callback - executed before persist
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        creationDate = now;
        modificationDate = now;
    }

    /**
     * JPA callback - executed before update
     */
    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();
    }

    /**
     * Extract timestamp from ULID
     * ULID contains timestamp in first 48 bits
     *
     * @return Creation timestamp extracted from ULID
     */
    public LocalDateTime getUlidTimestamp() {
        if (id == null) {
            return null;
        }

        // Extract timestamp from ULID (first 48 bits)
        long timestamp = (id.getMostSignificantBits() >>> 16);
        return LocalDateTime.ofEpochSecond(
                timestamp / 1000,
                (int) (timestamp % 1000) * 1_000_000,
                java.time.ZoneOffset.UTC
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractUlidEntity)) return false;
        AbstractUlidEntity that = (AbstractUlidEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}