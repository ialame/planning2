package com.pcagrade.order.service;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        // Set all fields using direct setters
        order.setSymfonyOrderId(symfonyOrderId);
        order.setOrderNumber(getString(orderData, "order_number"));
        order.setCustomerName(getString(orderData, "customer_name"));
        order.setDeliveryDate(getString(orderData, "delivery_date")); // This is the delai code
        order.setOrderDate(parseDate(getString(orderData, "order_date")));
        order.setTotalCards(getInteger(orderData, "total_cards"));
        order.setStatus(getInteger(orderData, "status"));
        order.setPrice(getFloat(orderData, "price"));
        order.setDelai(getString(orderData, "delai"));

        // Save to database
        Order saved = orderRepository.save(order);

        log.debug("✅ Saved order: {} - {} cards, delai: {}, price: {}",
                saved.getOrderNumber(),
                saved.getTotalCards(),
                saved.getDelai(),
                saved.getPrice());
    }

    // ============================================================
    // Helper Methods for Data Extraction
    // ============================================================

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    private Integer getInteger(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Cannot parse integer from '{}': {}", key, value);
            return null;
        }
    }

    private Float getFloat(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof Float) {
                return (Float) value;
            }
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            log.warn("⚠️ Cannot parse float from '{}': {}", key, value);
            return null;
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ Cannot parse date: {}", dateString);
            return null;
        }
    }
}