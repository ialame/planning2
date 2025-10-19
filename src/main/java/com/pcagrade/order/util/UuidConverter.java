// ============= UUIDCONVERTER - CONVERSION AUTOMATIQUE =============

// ✅ CRÉEZ ce nouveau fichier : UuidConverter.java

package com.pcagrade.order.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * ✅ Convertisseur automatique UUID ↔ BINARY(16)
 *
 * Gère la conversion transparente entre UUID Java et BINARY(16) MariaDB
 */
@Converter(autoApply = true)
public class UuidConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        try {
            // Convertir UUID en bytes pour BINARY(16)
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());

            byte[] bytes = buffer.array();

            return bytes;

        } catch (Exception e) {
            System.err.println("❌ Erreur conversion UUID → bytes: " + e.getMessage());
            return null;
        }
    }

    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        try {
            // Convertir bytes BINARY(16) en UUID
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long mostSigBits = buffer.getLong();
            long leastSigBits = buffer.getLong();

            UUID uuid = new UUID(mostSigBits, leastSigBits);

            return uuid;

        } catch (Exception e) {
            System.err.println("❌ Erreur conversion bytes → UUID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Utilitaire : Convertir bytes en hex pour debug
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";

        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }
}