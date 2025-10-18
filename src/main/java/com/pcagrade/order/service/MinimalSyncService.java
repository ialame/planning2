package com.pcagrade.order.service;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal Sync Service - Simple version without reflection
 * Syncs orders from Symfony API to local database
 */
@Service
public class MinimalSyncService {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderRepository orderRepository;

    public MinimalSyncService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Sync all orders from Symfony API
     */
    @Transactional
    public int syncOrders(List<Map<String, Object>> ordersData) {
        log.info("🔄 Starting orders sync: {} orders", ordersData.size());
        int successCount = 0;
        int errorCount = 0;

        for (Map<String, Object> orderData : ordersData) {
            try {
                syncSingleOrder(orderData);
                successCount++;

                if (successCount % 100 == 0) {
                    log.info("📦 Progress: {}/{} orders synced", successCount, ordersData.size());
                }
            } catch (Exception e) {
                errorCount++;
                log.error("❌ Error syncing order {}: {}", orderData.get("id"), e.getMessage());

                // Log first 5 errors in detail
                if (errorCount <= 5) {
                    log.error("Full error details:", e);
                }
            }
        }

        log.info("✅ Orders sync completed: {}/{} orders synced ({} errors)",
                successCount, ordersData.size(), errorCount);
        return successCount;
    }

    /**
     * Sync a single order using direct setters (no reflection)
     */
    private void syncSingleOrder(Map<String, Object> orderData) {
        // Get Symfony ID
        String symfonyOrderId = getString(orderData, "id");
        if (symfonyOrderId == null || symfonyOrderId.isEmpty()) {
            log.warn("⚠️ Skipping order with no ID");
            return;
        }

        // Find existing order by Symfony ID or create new one
        Optional<Order> existingOrder = orderRepository.findBySymfonyOrderId(symfonyOrderId);
        Order order;

        if (existingOrder.isPresent()) {
            order = existingOrder.get();
            log.debug("📝 Updating existing order: {}", symfonyOrderId);
        } else {
            order = new Order();
            log.debug("✨ Creating new order: {}", symfonyOrderId);
        }

        // Set all fields using direct setters (Symfony fields only)
        order.setSymfonyOrderId(symfonyOrderId);
        order.setOrderNumber(getString(orderData, "order_number"));
        order.setCustomerName(getString(orderData, "customer_name"));

        // Set delai (priority code: X, F+, F, C, E)
        order.setDelai(getString(orderData, "delai"));

        // Set order creation date
        order.setDate(parseDate(getString(orderData, "order_date")));

        // Set total cards count
        order.setTotalCards(getInteger(orderData, "total_cards"));

        // Convert Symfony status integer to OrderStatus enum
        Integer statusCode = getInteger(orderData, "status");
        order.setStatus(convertSymfonyStatusToOrderStatus(statusCode));

        // Set order price/total
        order.setPrice(getFloat(orderData, "price"));

        // Save to database
        Order saved = orderRepository.save(order);

        log.debug("✅ Saved order: {} - {} cards, delai: {}, price: {}",
                saved.getOrderNumber(),
                saved.getTotalCards(),
                saved.getDelai(),
                saved.getPrice());
    }

    /**
     * Convert Symfony status code (integer) to OrderStatus enum
     *
     * Symfony status codes:
     * 1 = A_RECEPTIONNER (To be received)
     * 2 = A_NOTER (To be graded) -> GRADING
     * 3 = A_CERTIFIER (To be encapsulated) -> CERTIFYING
     * 4 = A_PREPARER (To be prepared) -> PACKAGING
     * 5 = ENVOYEE (Sent) -> COMPLETED
     * 10 = A_SCANNER (To be scanned) -> SCANNING
     */
    private OrderStatus convertSymfonyStatusToOrderStatus(Integer statusCode) {
        if (statusCode == null) {
            return OrderStatus.PENDING;
        }

        switch (statusCode) {
            case 1:  // A_RECEPTIONNER
                return OrderStatus.PENDING;
            case 2:  // A_NOTER (to be graded)
                return OrderStatus.GRADING;
            case 3:  // A_CERTIFIER (to be certified)
                return OrderStatus.CERTIFYING;
            case 4:  // A_PREPARER (to be prepared/packaged)
                return OrderStatus.PACKAGING;
            case 5:  // ENVOYEE (sent)
            case 42: // A_ENVOYER (to be sent)
                return OrderStatus.DELIVERED;
            case 10: // A_SCANNER (to be scanned)
                return OrderStatus.SCANNING;
            default:
                log.debug("Unknown Symfony status code: {}, defaulting to PENDING", statusCode);
                return OrderStatus.PENDING;
        }
    }

    // ==================== HELPER METHODS ====================

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert '{}' value '{}' to Integer", key, value);
            return null;
        }
    }

    private Float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Cannot convert '{}' value '{}' to Float", key, value);
            return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // Parse as LocalDate then convert to LocalDateTime
            LocalDate localDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            return localDate.atStartOfDay();
        } catch (Exception e) {
            try {
                LocalDate localDate = LocalDate.parse(dateStr);
                return localDate.atStartOfDay();
            } catch (Exception ex) {
                log.warn("Cannot parse date: {}", dateStr);
                return null;
            }
        }
    }
}