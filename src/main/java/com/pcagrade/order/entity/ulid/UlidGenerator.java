package com.pcagrade.order.entity.ulid;

import com.github.f4b6a3.ulid.UlidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * Custom Hibernate ID Generator using ULID
 *
 * ULID (Universally Unique Lexicographically Sortable Identifier)
 * - 128 bits: 48 bits timestamp + 80 bits randomness
 * - Lexicographically sortable (chronological order)
 * - Compatible with UUID storage
 * - Better database index performance than random UUIDs
 */
public class UlidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(
            SharedSessionContractImplementor session,
            Object object
    ) {
        // Generate ULID and convert to UUID
        return UlidCreator.getMonotonicUlid().toUuid();
    }
}
