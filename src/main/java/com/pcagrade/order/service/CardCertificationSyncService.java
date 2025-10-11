package com.pcagrade.order.service;

import com.pcagrade.order.entity.CardCertification;
import com.pcagrade.order.repository.CardCertificationRepository;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.util.UlidConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for synchronizing card certification data from Symfony API
 * Maps API data to CardCertification entities with processing status flags
 */
@Service
public class CardCertificationSyncService {

    private static final Logger log = LoggerFactory.getLogger(CardCertificationSyncService.class);

    private final CardCertificationRepository cardCertificationRepository;
    private final OrderRepository orderRepository;

    public CardCertificationSyncService(
            CardCertificationRepository cardCertificationRepository,
            OrderRepository orderRepository) {
        this.cardCertificationRepository = cardCertificationRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Sync cards in batches with proper transaction management
     * Uses REQUIRES_NEW to ensure each batch gets its own transaction
     *
     * @param cardsData List of card data from Symfony API
     * @param batchSize Number of cards to process per batch
     * @return Number of successfully synced cards
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int syncCardsBatch(List<Map<String, Object>> cardsData, int batchSize) {
        int successCount = 0;
        List<CardCertification> batch = new ArrayList<>();

        for (Map<String, Object> cardData : cardsData) {
            try {
                CardCertification card = createOrUpdateCardFromData(cardData);
                if (card != null) {
                    batch.add(card);

                    // Save batch when it reaches the batch size
                    if (batch.size() >= batchSize) {
                        cardCertificationRepository.saveAll(batch);
                        successCount += batch.size();
                        log.info("✅ Saved batch of {} cards", batch.size());
                        batch.clear();
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error processing card: {}", cardData.get("id"), e);
                // Continue with next card instead of breaking entire batch
            }
        }

        // Save remaining cards
        if (!batch.isEmpty()) {
            cardCertificationRepository.saveAll(batch);
            successCount += batch.size();
            log.info("✅ Saved final batch of {} cards", batch.size());
        }

        return successCount;
    }

    /**
     * Create or update CardCertification from Symfony API data
     * Maps processing status flags from API to entity
     */
    private CardCertification createOrUpdateCardFromData(Map<String, Object> data) {
        try {
            String certificationIdHex = getString(data, "id");
            if (certificationIdHex == null || certificationIdHex.isEmpty()) {
                log.warn("⚠️ Card has no id, skipping");
                return null;
            }

            // Try to find existing certification
            CardCertification card = cardCertificationRepository
                    .findBySymfonyCertificationId(certificationIdHex)
                    .orElse(new CardCertification());

            // Set Symfony ID for future lookups
            card.setSymfonyCertificationId(certificationIdHex);

            // Map order_id (hex string to UUID)
            String orderIdHex = getString(data, "order_id");
            if (orderIdHex != null && !orderIdHex.isEmpty()) {
                try {
                    UUID orderId = UlidConverter.hexToUuid(orderIdHex);
                    card.setOrderId(orderId);

                    // Verify order exists
                    if (!orderRepository.existsById(orderId)) {
                        log.warn("⚠️ Order {} not found for card {}, skipping", orderIdHex, certificationIdHex);
                        return null;
                    }
                } catch (Exception e) {
                    log.error("❌ Invalid order_id format: {}", orderIdHex, e);
                    return null;
                }
            } else {
                log.warn("⚠️ Card {} has no order_id, skipping", certificationIdHex);
                return null;
            }

            // Map card name
            card.setCardName(getString(data, "card_name"));

            // Map processing status flags (from Symfony API)
            card.setGradingCompleted(getBoolean(data, "grading_completed", false));
            card.setCertificationCompleted(getBoolean(data, "certification_completed", false));
            card.setScanningCompleted(getBoolean(data, "scanning_completed", false));
            card.setPackagingCompleted(getBoolean(data, "packaging_completed", false));

            // Note: Most required fields should be set by the database defaults
            // We only set the planning-specific fields from the API data

            return card;

        } catch (Exception e) {
            log.error("❌ Error creating card from data", e);
            throw new RuntimeException("Failed to create card from data", e);
        }
    }

    /**
     * Get string value from map, handling null and different types
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Get boolean value from map, with default fallback
     */
    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        // Handle numeric values (0/1)
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        // Handle string values
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get sync statistics for monitoring
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSyncStats() {
        long totalCards = cardCertificationRepository.count();
        long incompleteCards = cardCertificationRepository.findIncompleteCertifications().size();
        long needGrading = cardCertificationRepository.findCertificationsNeedingGrading().size();
        long needCertification = cardCertificationRepository.findCertificationsNeedingCertification().size();
        long needScanning = cardCertificationRepository.findCertificationsNeedingScanning().size();
        long needPackaging = cardCertificationRepository.findCertificationsNeedingPackaging().size();

        return Map.of(
                "total_cards", totalCards,
                "incomplete_cards", incompleteCards,
                "needs_grading", needGrading,
                "needs_certification", needCertification,
                "needs_scanning", needScanning,
                "needs_packaging", needPackaging,
                "completion_percentage", totalCards > 0 ?
                        (double)(totalCards - incompleteCards) / totalCards * 100 : 0
        );
    }
}