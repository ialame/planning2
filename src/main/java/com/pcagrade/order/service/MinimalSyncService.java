package com.pcagrade.order.service;

import com.pcagrade.order.entity.Card;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.CardRepository;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal Sync Service - Only syncs essential fields for planning
 * Works with any Order/Card entity structure using reflection
 */
@Service
public class MinimalSyncService {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncService.class);

    private final OrderRepository orderRepository;
    private final CardRepository cardRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int BATCH_SIZE = 50;

    public MinimalSyncService(OrderRepository orderRepository, CardRepository cardRepository) {
        this.orderRepository = orderRepository;
        this.cardRepository = cardRepository;
    }

    // ============================================================
    // ORDER SYNC - 8 Essential Fields Only
    // ============================================================

    @Transactional
    public int syncOrders(List<Map<String, Object>> ordersData) {
        log.info("🔄 Starting orders sync: {} orders", ordersData.size());
        int successCount = 0;

        for (Map<String, Object> orderData : ordersData) {
            try {
                syncSingleOrder(orderData);
                successCount++;
            } catch (Exception e) {
                log.error("❌ Error syncing order {}: {}", orderData.get("id"), e.getMessage());
            }
        }

        log.info("✅ Orders sync completed: {}/{} orders synced", successCount, ordersData.size());
        return successCount;
    }

    private Order syncSingleOrder(Map<String, Object> data) {
        Long symfonyId = getLong(data, "id");
        if (symfonyId == null) {
            throw new IllegalArgumentException("Order must have an id");
        }

        // Find or create order
        Order order = findOrderBySymfonyId(symfonyId)
                .orElse(createNewOrder());

        // Set the 8 essential fields using reflection
        trySet(order, "symfonyOrderId", symfonyId);
        trySet(order, "orderNumber", getString(data, "order_number"));
        trySet(order, "customerName", getString(data, "customer_name"));
        trySet(order, "deliveryDate", parseDate(getString(data, "delivery_date")));
        trySet(order, "orderDate", parseDate(getString(data, "order_date")));
        trySet(order, "totalCards", getInteger(data, "total_cards"));
        trySet(order, "status", getString(data, "status"));
        trySet(order, "priority", getInteger(data, "priority"));

        Order saved = orderRepository.save(order);
        log.debug("✅ Synced order: {} (Symfony ID: {})", saved.getId(), symfonyId);

        return saved;
    }

    // ============================================================
    // CARD SYNC - 8 Essential Fields Only
    // ============================================================

    @Transactional
    public int syncCards(List<Map<String, Object>> cardsData) {
        log.info("🔄 Starting cards sync: {} cards", cardsData.size());
        int successCount = 0;
        List<Card> batch = new ArrayList<>();

        for (Map<String, Object> cardData : cardsData) {
            try {
                Card card = createCardFromData(cardData);
                if (card != null) {
                    batch.add(card);

                    if (batch.size() >= BATCH_SIZE) {
                        cardRepository.saveAll(batch);
                        successCount += batch.size();
                        log.info("✅ Saved batch: {} cards", batch.size());
                        batch.clear();
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error syncing card {}: {}", cardData.get("id"), e.getMessage());
            }
        }

        // Save remaining cards
        if (!batch.isEmpty()) {
            cardRepository.saveAll(batch);
            successCount += batch.size();
            log.info("✅ Saved final batch: {} cards", batch.size());
        }

        log.info("✅ Cards sync completed: {}/{} cards synced", successCount, cardsData.size());
        return successCount;
    }

    private Card createCardFromData(Map<String, Object> data) {
        Long symfonyOrderId = getLong(data, "order_id");
        if (symfonyOrderId == null) {
            log.warn("⚠️ Card {} has no order_id, skipping", data.get("id"));
            return null;
        }

        // Find the order (must exist!)
        Order order = findOrderBySymfonyId(symfonyOrderId).orElse(null);
        if (order == null) {
            log.warn("⚠️ Order {} not found for card {}, skipping", symfonyOrderId, data.get("id"));
            return null;
        }

        Card card = createNewCard();

        // Set the 8 essential fields
        trySet(card, "symfonyCardId", getString(data, "id"));
        trySet(card, "order", order);
        trySet(card, "cardName", getString(data, "card_name", "Pokemon Card"));

        // Processing status flags - THE KEY DATA FOR PLANNING!
        trySet(card, "gradingCompleted", getBoolean(data, "grading_completed"));
        trySet(card, "certificationCompleted", getBoolean(data, "certification_completed"));
        trySet(card, "scanningCompleted", getBoolean(data, "scanning_completed"));
        trySet(card, "packagingCompleted", getBoolean(data, "packaging_completed"));

        trySet(card, "processingStatus", getString(data, "processing_status", "PENDING"));

        return card;
    }

    // ============================================================
    // Reflection-based Setters - Works with ANY entity structure
    // ============================================================

    /**
     * Try to set a field value using reflection
     * Tries multiple approaches to work with any entity structure
     */
    private void trySet(Object obj, String fieldName, Object value) {
        if (value == null) return;

        // Try standard setter first
        if (tryStandardSetter(obj, fieldName, value)) return;

        // Try alternative French field names
        if (tryFrenchAlternatives(obj, fieldName, value)) return;

        // Log if we couldn't set the field (not critical, might not exist)
        log.trace("⚠️ Could not set field '{}' on {}", fieldName, obj.getClass().getSimpleName());
    }

    private boolean tryStandardSetter(Object obj, String fieldName, Object value) {
        try {
            String setterName = "set" + capitalize(fieldName);
            Method[] methods = obj.getClass().getMethods();

            for (Method method : methods) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    method.invoke(obj, value);
                    return true;
                }
            }
        } catch (Exception e) {
            // Try next approach
        }
        return false;
    }

    private boolean tryFrenchAlternatives(Object obj, String fieldName, Object value) {
        Map<String, String> frenchNames = Map.of(
                "orderNumber", "numeroCommande",
                "customerName", "nomClient",
                "deliveryDate", "delai",
                "orderDate", "dateCommande",
                "totalCards", "nbCartes",
                "cardName", "nom",
                "gradingCompleted", "notationTerminee",
                "certificationCompleted", "certificationTerminee",
                "scanningCompleted", "scanTermine",
                "packagingCompleted", "preparationTerminee"
        );

        String frenchName = frenchNames.get(fieldName);
        if (frenchName != null) {
            return tryStandardSetter(obj, frenchName, value);
        }

        return false;
    }

    // ============================================================
    // Repository Helpers
    // ============================================================

    private java.util.Optional<Order> findOrderBySymfonyId(Long symfonyId) {
        try {
            Method method = OrderRepository.class.getMethod("findBySymfonyOrderId", Long.class);
            return (java.util.Optional<Order>) method.invoke(orderRepository, symfonyId);
        } catch (Exception e) {
            // Method doesn't exist, try alternative
            return orderRepository.findAll().stream()
                    .filter(o -> symfonyId.equals(getOrderSymfonyId(o)))
                    .findFirst();
        }
    }

    private Long getOrderSymfonyId(Order order) {
        try {
            Method method = Order.class.getMethod("getSymfonyOrderId");
            return (Long) method.invoke(order);
        } catch (Exception e) {
            return null;
        }
    }

    private Order createNewOrder() {
        try {
            return Order.class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create Order instance", e);
        }
    }

    private Card createNewCard() {
        try {
            return Card.class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create Card instance", e);
        }
    }

    // ============================================================
    // Type Conversion Helpers
    // ============================================================

    private String getString(Map<String, Object> data, String key) {
        return getString(data, key, null);
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ Could not parse date: {}", dateString);
            return null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}