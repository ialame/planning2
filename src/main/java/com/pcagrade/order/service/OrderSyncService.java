package com.pcagrade.order.service;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service for synchronizing order data from Symfony API
 */
@Service
public class OrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    private final OrderRepository orderRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public OrderSyncService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Sync multiple orders from Symfony
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int syncOrders(List<Map<String, Object>> ordersData) {
        int successCount = 0;

        for (Map<String, Object> orderData : ordersData) {
            try {
                Order order = syncSingleOrder(orderData);
                if (order != null) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("❌ Error syncing order: {}", orderData.get("id"), e);
                // Continue with next order instead of failing all
            }
        }

        log.info("✅ Successfully synced {} out of {} orders", successCount, ordersData.size());
        return successCount;
    }

    /**
     * Sync a single order
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Order syncSingleOrder(Map<String, Object> orderData) {
        try {
            String orderNumber = getString(orderData, "order_number");

            // Check if order already exists
            Order order = orderRepository.findByOrderNumber(orderNumber)
                    .orElse(new Order());

            // Update order fields
            order.setOrderNumber(orderNumber);
            order.setCustomerName(getString(orderData, "customer_name"));
            order.setCustomerEmail(getString(orderData, "customer_email"));

            // Parse dates
            order.setOrderDate(parseDate(getString(orderData, "order_date")));
            order.setDeliveryDate(parseDate(getString(orderData, "delivery_date")));

            // Status and priority
            order.setStatus(getString(orderData, "status", "PENDING"));
            order.setPriority(getInteger(orderData, "priority", 0));

            // Card counts
            order.setTotalCards(getInteger(orderData, "total_cards", 0));
            order.setCompletedCards(getInteger(orderData, "completed_cards", 0));

            // Processing time (3 minutes per card * number of roles)
            int estimatedTime = order.getTotalCards() * 3 * 3; // 3 roles: grader, certifier, scanner
            order.setEstimatedProcessingTime(estimatedTime);

            // Notes and Symfony reference
            order.setNotes(getString(orderData, "notes"));

            Object symfonyIdObj = orderData.get("id");
            if (symfonyIdObj != null) {
                order.setSymfonyOrderId(convertToLong(symfonyIdObj));
            }

            // Sync timestamp
            order.setSyncedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);
            log.info("✅ Synced order: {} (ID: {})", orderNumber, savedOrder.getId());

            return savedOrder;

        } catch (Exception e) {
            log.error("❌ Error creating order from data: {}", orderData, e);
            throw e;
        }
    }

    /**
     * Calculate priority based on delivery date
     * Earlier delivery dates get lower priority numbers (higher priority)
     */
    public int calculatePriority(LocalDate deliveryDate) {
        LocalDate today = LocalDate.now();
        long daysUntilDelivery = java.time.temporal.ChronoUnit.DAYS.between(today, deliveryDate);

        // Priority: negative for overdue, 0 for today, positive for future
        return (int) daysUntilDelivery;
    }

    // Helper methods for safe type conversion

    private String getString(Map<String, Object> data, String key) {
        return getString(data, key, null);
    }

    private String getString(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Integer getInteger(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("⚠️ Could not parse date: {}, using today", dateString);
            return LocalDate.now();
        }
    }

    private Long convertToLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}