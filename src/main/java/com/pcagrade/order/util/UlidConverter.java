package com.pcagrade.order.util;

import com.github.f4b6a3.ulid.Ulid;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Utility class for ULID and UUID conversions
 * Handles conversion between hex strings, ULIDs, and UUIDs
 */
public class UlidConverter {

    /**
     * Convert hex string (32 chars) to UUID
     * Used for Symfony API ID conversion
     *
     * @param hexString 32-character hex string (no dashes)
     * @return UUID object
     */
    public static UUID hexToUuid(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            throw new IllegalArgumentException("Hex string cannot be null or empty");
        }

        // Remove any dashes or spaces
        String cleanHex = hexString.replaceAll("[-\\s]", "");

        // Validate length
        if (cleanHex.length() != 32) {
            throw new IllegalArgumentException(
                    "Hex string must be 32 characters long (got " + cleanHex.length() + "): " + hexString
            );
        }

        // Validate hex format
        if (!cleanHex.matches("[0-9A-Fa-f]+")) {
            throw new IllegalArgumentException("Invalid hex string: " + hexString);
        }

        try {
            // Convert hex string to UUID format
            String formatted = cleanHex.toLowerCase()
                    .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");

            return UUID.fromString(formatted);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert hex to UUID: " + hexString, e);
        }
    }

    /**
     * Convert UUID to hex string (no dashes)
     *
     * @param uuid UUID object
     * @return 32-character hex string
     */
    public static String uuidToHex(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uuid.toString().replace("-", "").toUpperCase();
    }

    /**
     * Convert UUID to BINARY(16) bytes for database storage
     *
     * @param uuid UUID object
     * @return 16-byte array
     */
    public static byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * Convert BINARY(16) bytes to UUID
     *
     * @param bytes 16-byte array from database
     * @return UUID object
     */
    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * Convert ULID string to UUID
     *
     * @param ulidString ULID string (26 chars)
     * @return UUID object
     */
    public static UUID ulidToUuid(String ulidString) {
        if (ulidString == null || ulidString.isEmpty()) {
            throw new IllegalArgumentException("ULID string cannot be null or empty");
        }

        try {
            Ulid ulid = Ulid.from(ulidString.trim());
            return ulid.toUuid();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ULID string: " + ulidString, e);
        }
    }

    /**
     * Convert UUID to ULID string
     *
     * @param uuid UUID object
     * @return ULID string (26 chars)
     */
    public static String uuidToUlid(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        try {
            Ulid ulid = Ulid.from(uuid);
            return ulid.toString();
        } catch (Exception e) {
            // If UUID cannot be converted to ULID, return UUID string
            return uuid.toString();
        }
    }

    /**
     * Smart ID converter - handles hex, UUID, or ULID format
     *
     * @param idString String in any supported format
     * @return UUID object
     */
    public static UUID smartConvert(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            return null;
        }

        String cleanId = idString.trim();

        try {
            // UUID with dashes (36 chars)
            if (cleanId.length() == 36 && cleanId.contains("-")) {
                return UUID.fromString(cleanId);
            }

            // Hex without dashes (32 chars)
            if (cleanId.length() == 32 && cleanId.matches("[0-9A-Fa-f]+")) {
                return hexToUuid(cleanId);
            }

            // ULID format (26 chars)
            if (cleanId.length() == 26 && cleanId.matches("[0-9A-Z]+")) {
                return ulidToUuid(cleanId);
            }

            throw new IllegalArgumentException("Unknown ID format: " + idString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert ID: " + idString, e);
        }
    }
}