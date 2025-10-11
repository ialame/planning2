package com.pcagrade.order.service;

import com.pcagrade.order.entity.Card;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.CardRepository;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for synchronizing card data from Symfony API
 * Handles transaction management properly to avoid EntityManager closed errors
 */
@Service
public class CardSyncService {

    private static final Logger log = LoggerFactory.getLogger(CardSyncService.class);

    private final CardRepository cardRepository;
    private final OrderRepository orderRepository;

    public CardSyncService(CardRepository cardRepository, OrderRepository orderRepository) {
        this.cardRepository = cardRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Sync cards in batches with proper transaction management
     * Uses REQUIRES_NEW to ensure each batch gets its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public int syncCardsBatch(List<Map<String, Object>> cardsData, int batchSize) {
        int successCount = 0;
        List<Card> batch = new ArrayList<>();

        for (Map<String, Object> cardData : cardsData) {
            try {
                Card card = createCardFromData(cardData);
                if (card != null) {
                    batch.add(card);

                    // Save batch when it reaches the batch size
                    if (batch.size() >= batchSize) {
                        cardRepository.saveAll(batch);
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
            cardRepository.saveAll(batch);
            successCount += batch.size();
            log.info("✅ Saved final batch of {} cards", batch.size());
        }

        return successCount;
    }

    /**
     * Sync a single card - used for individual updates
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Card syncSingleCard(Map<String, Object> cardData) {
        try {
            Card card = createCardFromData(cardData);
            if (card != null) {
                return cardRepository.save(card);
            }
            return null;
        } catch (Exception e) {
            log.error("❌ Error syncing card: {}", cardData.get("id"), e);
            throw e;
        }
    }

    /**
     * Check if a card already exists
     */
    public boolean cardExists(String cardId) {
        // Implement based on your Card entity structure
        // Example: return cardRepository.existsBySymfonyCardId(cardId);
        return false;
    }

    /**
     * Delete all cards for an order before re-syncing
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCardsForOrder(Long orderId) {
        try {
            cardRepository.deleteByOrderId(orderId);
            log.info("✅ Deleted existing cards for order {}", orderId);
        } catch (Exception e) {
            log.error("❌ Error deleting cards for order {}", orderId, e);
            throw e;
        }
    }

    /**
     * Create Card entity from Symfony data
     */
    private Card createCardFromData(Map<String, Object> cardData) {
        try {
            Card card = new Card();

            // Get order reference
            Object orderIdObj = cardData.get("order_id");
            if (orderIdObj != null) {
                Long orderId = convertToLong(orderIdObj);
                Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
                card.setOrder(order);
            }

            // Map card properties
            card.setCardName(getString(cardData, "card_name"));
            card.setCardNumber(getString(cardData, "card_number"));
            card.setCardSet(getString(cardData, "card_set"));
            card.setRarity(getString(cardData, "rarity"));
            card.setCondition(getString(cardData, "condition"));

            // Grades
            card.setEstimatedGrade(getInteger(cardData, "estimated_grade"));
            card.setActualGrade(getInteger(cardData, "actual_grade"));

            // Status
            card.setProcessingStatus(getString(cardData, "processing_status", "PENDING"));
            card.setGradingCompleted(getBoolean(cardData, "grading_completed", false));
            card.setCertificationCompleted(getBoolean(cardData, "certification_completed", false));
            card.setScanningCompleted(getBoolean(cardData, "scanning_completed", false));
            card.setPackagingCompleted(getBoolean(cardData, "packaging_completed", false));

            // Notes
            card.setNotes(getString(cardData, "notes"));

            return card;

        } catch (Exception e) {
            log.error("❌ Error creating card from data: {}", cardData, e);
            return null;
        }
    }

    // Helper methods for safe type conversion

    private String getString(Map<String, Object> data, String key) {
        return getString(data, key, null);
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> data, String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private Long convertToLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}