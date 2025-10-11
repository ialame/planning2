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
import java.util.List;
import java.util.Map;

/**
 * Minimal Sync Service - Syncs 9 essential fields for planning
 *
 * Synced fields from Symfony API:
 * 1. id (ULID hex)
 * 2. order_number
 * 3. customer_name
 * 4. delivery_date (delai code)
 * 5. order_date
 * 6. total_cards
 * 7. status
 * 8. price (NEW - from invoice.total_ttc)
 * 9. delai (NEW - replaces priority)
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
    // ORDER SYNC - 9 Essential Fields
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
        String symfonyIdHex = getString(data, "id");
        if (symfonyIdHex == null) {
            throw new IllegalArgumentException("Order must have an id");
        }

        // Find or create order by symfony ID
        Order order = findOrderBySymfonyId(symfonyIdHex)
                .orElse(createNewOrder());

        // Set the 9 essential fields using reflection
        trySet(order, "symfonyOrderId", symfonyIdHex);
        trySet(order, "orderNumber", getString(data, "order_number"));
        trySet(order, "customerName", getString(data, "customer_name"));

        // delivery_date is actually the delai code (not a date)
        String deliveryDateCode = getString(data, "delivery_date");
        trySet(order, "deliveryDate", deliveryDateCode);

        trySet(order, "orderDate", parseDate(getString(data, "order_date")));
        trySet(order, "totalCards", getInteger(data, "total_cards"));
        trySet(order, "status", getInteger(data, "status"));

        // NEW: Price from invoice
        trySet(order, "price", getFloat(data, "price"));

        // NEW: Delai code (replaces priority)
        trySet(order, "delai", getString(data, "delai"));

        Order saved = orderRepository.save(order);
        log.debug("✅ Synced order: {} (Symfony ID: {}, Delai: {}, Price: {})",
                saved.getId(), symfonyIdHex, saved.getDelai(), saved.getPrice());

        return saved;
    }

    // ============================================================
    // CARD SYNC - 8 Essential Fields
    // ============================================================

    @Transactional
    public int syncCards(List<Map<String, Object>> cardsData) {
        log.info("🔄 Starting cards sync: {} cards", cardsData.size());
        int successCount = 0;

        for (Map<String, Object> cardData : cardsData) {
            try {
                syncSingleCard(cardData);
                successCount++;
            } catch (Exception e) {
                log.error("❌ Error syncing card {}: {}", cardData.get("id"), e.getMessage());
            }
        }

        log.info("✅ Cards sync completed: {}/{} cards synced", successCount, cardsData.size());
        return successCount;
    }

    private Card syncSingleCard(Map<String, Object> data) {
        String cardIdHex = getString(data, "id");
        if (cardIdHex == null) {
            throw new IllegalArgumentException("Card must have an id");
        }

        String orderIdHex = getString(data, "order_id");
        if (orderIdHex == null) {
            log.warn("⚠️ Card {} has no order_id, skipping", cardIdHex);
            return null;
        }

        // Find the order
        Order order = findOrderBySymfonyId(orderIdHex).orElse(null);
        if (order == null) {
            log.warn("⚠️ Order {} not found for card {}, skipping", orderIdHex, cardIdHex);
            return null;
        }

        // Find or create card
        Card card = findCardBySymfonyId(cardIdHex)
                .orElse(createNewCard());

        // Set the 8 essential fields
        trySet(card, "symfonyCardId", cardIdHex);
        trySet(card, "order", order);
        trySet(card, "cardName", getString(data, "card_name"));
        trySet(card, "processingStatus", getInteger(data, "processing_status"));
        trySet(card, "gradingCompleted", getBoolean(data, "grading_completed"));
        trySet(card, "certificationCompleted", getBoolean(data, "certification_completed"));
        trySet(card, "scanningCompleted", getBoolean(data, "scanning_completed"));
        trySet(card, "packagingCompleted", getBoolean(data, "packaging_completed"));

        Card saved = cardRepository.save(card);
        log.debug("✅ Synced card: {} for order {}", saved.getId(), order.getOrderNumber());

        return saved;
    }

    // ============================================================
    // Helper Methods - Reflection-based Setters
    // ============================================================

    private void trySet(Object obj, String fieldName, Object value) {
        if (value == null) {
            return; // Don't set null values
        }

        // Try standard setter
        if (tryStandardSetter(obj, fieldName, value)) {
            return;
        }

        // Try French alternatives
        if (tryFrenchAlternatives(obj, fieldName, value)) {
            return;
        }

        // Log warning if field couldn't be set
        log.warn("⚠️ Could not set field '{}' on {}", fieldName, obj.getClass().getSimpleName());
    }

    private boolean tryStandardSetter(Object obj, String fieldName, Object value) {
        try {
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
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

    private java.util.Optional<Order> findOrderBySymfonyId(String symfonyIdHex) {
        try {
            Method method = OrderRepository.class.getMethod("findBySymfonyOrderId", String.class);
            return (java.util.Optional<Order>) method.invoke(orderRepository, symfonyIdHex);
        } catch (Exception e) {
            // Method doesn't exist, try alternative
            return orderRepository.findAll().stream()
                    .filter(o -> symfonyIdHex.equals(getOrderSymfonyId(o)))
                    .findFirst();
        }
    }

    private String getOrderSymfonyId(Order order) {
        try {
            Method method = Order.class.getMethod("getSymfonyOrderId");
            return (String) method.invoke(order);
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.Optional<Card> findCardBySymfonyId(String symfonyIdHex) {
        try {
            Method method = CardRepository.class.getMethod("findBySymfonyCardId", String.class);
            return (java.util.Optional<Card>) method.invoke(cardRepository, symfonyIdHex);
        } catch (Exception e) {
            return cardRepository.findAll().stream()
                    .filter(c -> symfonyIdHex.equals(getCardSymfonyId(c)))
                    .findFirst();
        }
    }

    private String getCardSymfonyId(Card card) {
        try {
            Method method = Card.class.getMethod("getSymfonyCardId");
            return (String) method.invoke(card);
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
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Could not parse integer from key '{}': {}", key, value);
            return null;
        }
    }

    private Float getFloat(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Could not parse float from key '{}': {}", key, value);
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // Handle both "2025-07-04" and "2025-07-04 12:30:00" formats
            if (dateStr.length() > 10) {
                dateStr = dateStr.substring(0, 10);
            }
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ Could not parse date: {}", dateStr);
            return null;
        }
    }
}