package com.pcagrade.order.service;

import com.pcagrade.order.util.UlidConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for syncing card translations using native SQL
 *
 * Uses native SQL instead of JPA because CardTranslation is @Immutable
 * This is a catalog table that should be read-only after initial sync
 */
@Service
public class TranslationSyncService {

    private static final Logger log = LoggerFactory.getLogger(TranslationSyncService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbcTemplate;

    public TranslationSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Sync translations from Symfony using native SQL INSERT
     * Batch size of 100 for performance
     */
    @Transactional
    public int syncTranslations(List<Map<String, Object>> translationsData) {
        log.info("🔄 Starting translations sync: {} translations", translationsData.size());

        int successCount = 0;
        int batchSize = 100;

        // Prepare SQL statement
        String sql = "INSERT INTO card_translation " +
                "(id, translatable_id, name, label_name, locale, discriminator, available, release_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), " +
                "label_name = VALUES(label_name), " +
                "available = VALUES(available), " +
                "release_date = VALUES(release_date)";

        // Process in batches
        for (int i = 0; i < translationsData.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, translationsData.size());
            List<Map<String, Object>> batch = translationsData.subList(i, endIndex);

            int batchSuccess = insertBatch(sql, batch);
            successCount += batchSuccess;

            if ((i / batchSize + 1) % 10 == 0) {
                log.info("📊 Progress: {} translations synced", successCount);
            }
        }

        log.info("✅ Translations sync completed: {}/{} translations synced",
                successCount, translationsData.size());

        return successCount;
    }

    /**
     * Insert a batch of translations using JDBC batch update
     */
    private int insertBatch(String sql, List<Map<String, Object>> batch) {
        try {
            jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, translation) -> {
                // id
                String idHex = getString(translation, "id");
                UUID id = UlidConverter.hexToUuid(idHex);
                ps.setBytes(1, uuidToBytes(id));

                // translatable_id (card_id)
                String cardIdHex = getString(translation, "card_id");
                UUID cardId = UlidConverter.hexToUuid(cardIdHex);
                ps.setBytes(2, uuidToBytes(cardId));

                // name
                ps.setString(3, getString(translation, "name"));

                // label_name
                ps.setString(4, getString(translation, "label_name"));

                // locale
                ps.setString(5, getString(translation, "locale"));

                // discriminator
                ps.setString(6, getString(translation, "discriminator"));

                // available
                ps.setBoolean(7, getBoolean(translation, "available", true));

                // release_date
                String dateStr = getString(translation, "release_date");
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
                        ps.setObject(8, date.atStartOfDay());
                    } catch (Exception e) {
                        ps.setObject(8, null);
                    }
                } else {
                    ps.setObject(8, null);
                }
            });

            return batch.size();

        } catch (Exception e) {
            log.error("❌ Error inserting batch: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get translation statistics
     */
    public Map<String, Object> getTranslationStats() {
        String sql = "SELECT " +
                "COUNT(*) as total, " +
                "COUNT(DISTINCT locale) as locales, " +
                "COUNT(DISTINCT translatable_id) as cards " +
                "FROM card_translation";

        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Clear all translations (for re-sync)
     */
    @Transactional
    public int clearTranslations() {
        log.warn("⚠️ Clearing all translations");
        String sql = "DELETE FROM card_translation";
        return jdbcTemplate.update(sql);
    }

    // Helper methods
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;

        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> 8 * (7 - i + 8));
        }

        return buffer;
    }
}