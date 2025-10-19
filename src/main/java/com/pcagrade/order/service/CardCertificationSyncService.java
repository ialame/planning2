package com.pcagrade.order.service;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.util.UlidConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ULTRA-FAST Card Certification Sync Service using native JDBC batch
 *
 * Uses raw JDBC INSERT instead of JPA for maximum performance
 * Expected: 50,000 cards in ~30-60 seconds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardCertificationSyncService {

    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    /**
     * Sync cards from Symfony API data using native JDBC batch insert
     */
    @Transactional
    public int syncCards(List<Map<String, Object>> cardsData) {
        if (cardsData == null || cardsData.isEmpty()) {
            log.info("No cards to sync");
            return 0;
        }

        log.info("🔄 Starting FAST JDBC card sync: {} cards to process", cardsData.size());
        long startTime = System.currentTimeMillis();

        int totalSuccess = 0;
        int totalErrors = 0;

        // Split into batches
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < cardsData.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, cardsData.size());
            batches.add(cardsData.subList(i, end));
        }

        log.info("📦 Processing {} batches of up to {} cards each", batches.size(), BATCH_SIZE);

        // Process each batch
        int batchNumber = 0;
        for (List<Map<String, Object>> batch : batches) {
            batchNumber++;
            try {
                int batchSuccess = insertBatchJdbc(batch, batchNumber);
                totalSuccess += batchSuccess;

                if (batchSuccess < batch.size()) {
                    totalErrors += (batch.size() - batchSuccess);
                }

                if (batchNumber % 10 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = totalSuccess / (elapsed / 1000.0);
                    log.info("📊 Progress: {} batches, {}/{} cards ({:.0f} cards/sec)",
                            batchNumber, totalSuccess, cardsData.size(), rate);
                }
            } catch (Exception e) {
                log.error("❌ Batch {} failed: {}", batchNumber, e.getMessage());
                totalErrors += batch.size();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double rate = totalSuccess / (duration / 1000.0);

        log.info("✅ Card sync completed: {}/{} cards in {:.1f}s ({:.0f} cards/sec)",
                totalSuccess, cardsData.size(), duration / 1000.0, rate);

        return totalSuccess;
    }

    /**
     * Insert a batch using native JDBC for maximum performance
     */
    private int insertBatchJdbc(List<Map<String, Object>> batchData, int batchNumber) {
        String sql = "INSERT INTO card_certification (" +
                "id, order_id, card_name, code_barre, " +
                "grading_completed, certification_completed, " +
                "scanning_completed, packaging_completed, " +
                "date, status, langue, creation_date, modification_date" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "card_name = VALUES(card_name), " +
                "code_barre = VALUES(code_barre), " +
                "grading_completed = VALUES(grading_completed), " +
                "certification_completed = VALUES(certification_completed), " +
                "scanning_completed = VALUES(scanning_completed), " +
                "packaging_completed = VALUES(packaging_completed), " +
                "modification_date = VALUES(modification_date)";

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        // Prepare all batch arguments
        for (Map<String, Object> cardData : batchData) {
            try {
                String certificationIdHex = getString(cardData, "id");
                if (certificationIdHex == null || certificationIdHex.isEmpty()) {
                    continue;
                }

                UUID certificationId = UlidConverter.hexToUuid(certificationIdHex);

                // Find order
                String orderIdHex = getString(cardData, "order_id");
                if (orderIdHex == null || orderIdHex.isEmpty()) {
                    continue;
                }

                Optional<Order> orderOpt = orderRepository.findBySymfonyOrderId(orderIdHex);
                if (orderOpt.isEmpty()) {
                    continue;
                }

                UUID orderId = orderOpt.get().getId();

                // Get other fields
                String cardName = getString(cardData, "card_name");
                String codeBarre = getString(cardData, "code_barre");
                if (codeBarre == null || codeBarre.isEmpty()) {
                    codeBarre = certificationIdHex;
                }

                Boolean gradingCompleted = getBoolean(cardData, "grading_completed", false);
                Boolean certificationCompleted = getBoolean(cardData, "certification_completed", false);
                Boolean scanningCompleted = getBoolean(cardData, "scanning_completed", false);
                Boolean packagingCompleted = getBoolean(cardData, "packaging_completed", false);

                // Create batch arguments array
                Object[] args = new Object[] {
                        uuidToBytes(certificationId),
                        uuidToBytes(orderId),
                        cardName,
                        codeBarre,
                        gradingCompleted,
                        certificationCompleted,
                        scanningCompleted,
                        packagingCompleted,
                        now,
                        0,
                        "FR",
                        now,
                        now
                };

                batchArgs.add(args);

            } catch (Exception e) {
                log.warn("⚠️ Error preparing card: {}", e.getMessage());
            }
        }

        // Execute batch
        int successCount = 0;
        try {
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);

            for (int result : results) {
                if (result > 0) {
                    successCount++;
                }
            }

            log.debug("✅ Batch {} completed: {}/{} cards", batchNumber, successCount, batchData.size());

        } catch (Exception e) {
            log.error("❌ Batch {} failed: {}", batchNumber, e.getMessage());
        }

        return successCount;
    }

    /**
     * Sync a batch of cards (called by MinimalSyncController)
     */
    public int syncCardsBatch(List<Map<String, Object>> cardsData, int batchSize) {
        if (cardsData == null || cardsData.isEmpty()) {
            return 0;
        }
        return insertBatchJdbc(cardsData, 0);
    }

    /**
     * Get total number of cards in database
     */
    public long getTotalCards() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM card_certification",
                Long.class
        );
        return count != null ? count : 0;
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            String sql = "SELECT " +
                    "COUNT(*) as total, " +
                    "SUM(CASE WHEN grading_completed = 0 THEN 1 ELSE 0 END) as needs_grading, " +
                    "SUM(CASE WHEN certification_completed = 0 THEN 1 ELSE 0 END) as needs_cert, " +
                    "SUM(CASE WHEN scanning_completed = 0 THEN 1 ELSE 0 END) as needs_scan, " +
                    "SUM(CASE WHEN packaging_completed = 0 THEN 1 ELSE 0 END) as needs_pack " +
                    "FROM card_certification";

            Map<String, Object> result = jdbcTemplate.queryForMap(sql);

            long total = ((Number) result.get("total")).longValue();
            long needsGrading = ((Number) result.get("needs_grading")).longValue();
            long needsCert = ((Number) result.get("needs_cert")).longValue();
            long needsScan = ((Number) result.get("needs_scan")).longValue();
            long needsPack = ((Number) result.get("needs_pack")).longValue();

            long incomplete = needsGrading + needsCert + needsScan + needsPack;
            double completion = total > 0 ? ((total - incomplete) * 100.0 / total) : 0.0;

            stats.put("total_cards", total);
            stats.put("incomplete_cards", incomplete);
            stats.put("needs_grading", needsGrading);
            stats.put("needs_certification", needsCert);
            stats.put("needs_scanning", needsScan);
            stats.put("needs_packaging", needsPack);
            stats.put("completion_percentage", Math.round(completion * 100.0) / 100.0);

        } catch (Exception e) {
            log.error("❌ Error getting sync stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // ========== HELPER METHODS ==========

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("yes");
        }

        return defaultValue;
    }

    /**
     * Convert UUID to BINARY(16) bytes
     */
    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - (i - 8))));
        }
        return bytes;
    }
}