package com.pcagrade.order.service;

import com.pcagrade.order.entity.CardCertification;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.CardCertificationRepository;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Improved Card Certification Sync Service
 *
 * Key improvements:
 * - Smaller batch size (50 instead of 100)
 * - Better error handling (continues on single card errors)
 * - Detailed logging for debugging
 * - Separate transaction per batch to avoid rollback-only issues
 */
@Service
public class CardCertificationSyncService {

    private static final Logger log = LoggerFactory.getLogger(CardCertificationSyncService.class);
    private static final int BATCH_SIZE = 50; // Reduced from 100

    private final CardCertificationRepository cardCertificationRepository;
    private final OrderRepository orderRepository;

    public CardCertificationSyncService(
            CardCertificationRepository cardCertificationRepository,
            OrderRepository orderRepository) {
        this.cardCertificationRepository = cardCertificationRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Sync cards in batches with improved error handling
     * Each batch gets its own transaction to prevent rollback cascading
     */
    public int syncCardsBatch(List<Map<String, Object>> cardsData, int requestedBatchSize) {
        log.info("🔄 Starting card sync: {} cards to process", cardsData.size());

        int totalSuccess = 0;
        int totalErrors = 0;
        int batchNumber = 0;

        // Use smaller batch size for stability
        int effectiveBatchSize = Math.min(requestedBatchSize, BATCH_SIZE);

        // Process in batches
        for (int i = 0; i < cardsData.size(); i += effectiveBatchSize) {
            batchNumber++;
            int endIndex = Math.min(i + effectiveBatchSize, cardsData.size());
            List<Map<String, Object>> batch = cardsData.subList(i, endIndex);

            try {
                int batchSuccess = syncSingleBatch(batch, batchNumber);
                totalSuccess += batchSuccess;

                if (batchSuccess < batch.size()) {
                    totalErrors += (batch.size() - batchSuccess);
                }

                if (batchNumber % 10 == 0) {
                    log.info("📊 Progress: Processed {} batches, {} cards synced, {} errors",
                            batchNumber, totalSuccess, totalErrors);
                }
            } catch (Exception e) {
                log.error("❌ Batch {} failed completely: {}", batchNumber, e.getMessage());
                totalErrors += batch.size();
            }
        }

        log.info("✅ Card sync completed: {}/{} cards synced ({} errors)",
                totalSuccess, cardsData.size(), totalErrors);

        return totalSuccess;
    }

    /**
     * Sync a single batch with its own transaction
     * REQUIRES_NEW ensures each batch is independent
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    protected int syncSingleBatch(List<Map<String, Object>> batchData, int batchNumber) {
        List<CardCertification> cardsToSave = new ArrayList<>();
        int successCount = 0;

        for (Map<String, Object> cardData : batchData) {
            try {
                CardCertification card = createOrUpdateCardFromData(cardData);
                if (card != null) {
                    cardsToSave.add(card);
                    successCount++;
                }
            } catch (Exception e) {
                String cardId = getString(cardData, "id");
                log.warn("⚠️ Skipping card {} in batch {}: {}", cardId, batchNumber, e.getMessage());
                // Continue with next card - don't let one bad card fail the whole batch
            }
        }

        // Save all valid cards in this batch
        if (!cardsToSave.isEmpty()) {
            try {
                cardCertificationRepository.saveAll(cardsToSave);
                log.debug("✅ Batch {} saved: {}/{} cards", batchNumber, cardsToSave.size(), batchData.size());
            } catch (Exception e) {
                log.error("❌ Failed to save batch {}: {}", batchNumber, e.getMessage());
                throw e; // Let this batch fail but don't affect other batches
            }
        }

        return successCount;
    }

    /**
     * Create or update CardCertification from Symfony API data
     */
    private CardCertification createOrUpdateCardFromData(Map<String, Object> data) {
        String certificationIdHex = getString(data, "id");
        if (certificationIdHex == null || certificationIdHex.isEmpty()) {
            log.warn("⚠️ Card has no id, skipping");
            return null;
        }

        // Try to find existing certification
        CardCertification card = cardCertificationRepository
                .findBySymfonyCertificationId(certificationIdHex)
                .orElse(new CardCertification());

        // Set Symfony ID
        card.setSymfonyCertificationId(certificationIdHex);

        // Map order_id by finding order through symfonyOrderId
        String orderIdHex = getString(data, "order_id");
        if (orderIdHex != null && !orderIdHex.isEmpty()) {
            Optional<Order> orderOpt = orderRepository.findBySymfonyOrderId(orderIdHex);

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                card.setOrderId(order.getId());

                log.trace("Mapped card {} to order {} (Spring Boot ID: {})",
                        certificationIdHex, orderIdHex, order.getId());
            } else {
                log.warn("⚠️ Order {} not found for card {}, skipping",
                        orderIdHex, certificationIdHex);
                return null;
            }
        } else {
            log.warn("⚠️ Card {} has no order_id, skipping", certificationIdHex);
            return null;
        }

        // Map card name
        card.setCardName(getString(data, "card_name"));

        // Map processing status flags
        card.setGradingCompleted(getBoolean(data, "grading_completed", false));
        card.setCertificationCompleted(getBoolean(data, "certification_completed", false));
        card.setScanningCompleted(getBoolean(data, "scanning_completed", false));
        card.setPackagingCompleted(getBoolean(data, "packaging_completed", false));

        return card;
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalCards = cardCertificationRepository.count();
            long needsGrading = cardCertificationRepository.countByGradingCompleted(false);
            long needsCertification = cardCertificationRepository.countByCertificationCompleted(false);
            long needsScanning = cardCertificationRepository.countByScanningCompleted(false);
            long needsPackaging = cardCertificationRepository.countByPackagingCompleted(false);

            long incompleteCards = cardCertificationRepository.countIncompleteCards();

            double completionPercentage = totalCards > 0
                    ? ((totalCards - incompleteCards) * 100.0 / totalCards)
                    : 0.0;

            stats.put("total_cards", totalCards);
            stats.put("incomplete_cards", incompleteCards);
            stats.put("needs_grading", needsGrading);
            stats.put("needs_certification", needsCertification);
            stats.put("needs_scanning", needsScanning);
            stats.put("needs_packaging", needsPackaging);
            stats.put("completion_percentage", Math.round(completionPercentage * 100.0) / 100.0);

        } catch (Exception e) {
            log.error("❌ Error getting sync stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
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
}