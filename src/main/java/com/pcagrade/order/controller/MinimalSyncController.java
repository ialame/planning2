package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.service.CardCertificationSyncService;
import com.pcagrade.order.util.UlidConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Minimal controller for synchronizing orders and cards from Symfony API
 * Focuses on essential fields needed for planning
 */
@RestController
@RequestMapping("/api/sync")
public class MinimalSyncController {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncController.class);
    private static final int CARD_BATCH_SIZE = 100; // Process cards in batches

    private final OrderRepository orderRepository;
    private final CardCertificationSyncService cardSyncService;
    private final RestTemplate restTemplate;

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiUrl;

    public MinimalSyncController(
            OrderRepository orderRepository,
            CardCertificationSyncService cardSyncService,
            RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.cardSyncService = cardSyncService;
        this.restTemplate = restTemplate;
    }

    /**
     * Sync orders from Symfony API
     * POST /api/sync/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> syncOrders() {
        log.info("🔄 Starting orders synchronization from Symfony API");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Fetch orders from Symfony API
            String ordersUrl = symfonyApiUrl + "/api/planning/export/orders?limit=10000&exclude_completed=true";
            log.info("📡 Fetching orders from: {}", ordersUrl);

            Map<String, Object> response = restTemplate.getForObject(ordersUrl, Map.class);

            if (response == null || !response.containsKey("orders")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> ordersData = (List<Map<String, Object>>) response.get("orders");
            log.info("📦 Received {} orders from Symfony", ordersData.size());

            int syncedCount = 0;
            List<Order> ordersToSave = new ArrayList<>();

            for (Map<String, Object> orderData : ordersData) {
                try {
                    Order order = createOrUpdateOrder(orderData);
                    if (order != null) {
                        ordersToSave.add(order);
                        syncedCount++;
                    }
                } catch (Exception e) {
                    log.error("❌ Error processing order: {}", orderData.get("id"), e);
                }
            }

            // Save all orders in batch
            orderRepository.saveAll(ordersToSave);

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_orders", ordersData.size());
            result.put("synced_count", syncedCount);
            result.put("duration_ms", duration);
            result.put("message", String.format("Synced %d/%d orders", syncedCount, ordersData.size()));

            log.info("✅ Orders sync completed: {} orders in {}ms", syncedCount, duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("symfony_url", symfonyApiUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync cards from Symfony API
     * POST /api/sync/cards
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String orderId) {

        log.info("🔄 Starting cards synchronization from Symfony API");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int totalSynced = 0;
        int totalErrors = 0;

        try {
            // Build API URL with parameters
            StringBuilder urlBuilder = new StringBuilder(symfonyApiUrl)
                    .append("/api/planning/export/cards");

            List<String> params = new ArrayList<>();
            if (limit != null) {
                params.add("limit=" + limit);
            } else {
                params.add("limit=50000"); // Default high limit
            }
            if (orderId != null) {
                params.add("order_id=" + orderId);
            }

            if (!params.isEmpty()) {
                urlBuilder.append("?").append(String.join("&", params));
            }

            String cardsUrl = urlBuilder.toString();
            log.info("📡 Fetching cards from: {}", cardsUrl);

            Map<String, Object> response = restTemplate.getForObject(cardsUrl, Map.class);

            if (response == null || !response.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) response.get("cards");
            log.info("🎴 Received {} cards from Symfony", cardsData.size());

            // Process cards in batches
            int totalCards = cardsData.size();
            for (int i = 0; i < totalCards; i += CARD_BATCH_SIZE) {
                int endIndex = Math.min(i + CARD_BATCH_SIZE, totalCards);
                List<Map<String, Object>> batch = cardsData.subList(i, endIndex);

                log.info("📦 Processing batch {}-{} of {}", i + 1, endIndex, totalCards);

                try {
                    int batchSynced = cardSyncService.syncCardsBatch(batch, CARD_BATCH_SIZE);
                    totalSynced += batchSynced;
                    log.info("✅ Batch processed: {} cards synced", batchSynced);
                } catch (Exception e) {
                    totalErrors += batch.size();
                    log.error("❌ Error processing batch {}-{}", i + 1, endIndex, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_cards", totalCards);
            result.put("synced_count", totalSynced);
            result.put("error_count", totalErrors);
            result.put("duration_ms", duration);
            result.put("message", String.format("Successfully synced %d/%d cards", totalSynced, totalCards));

            // Add sync statistics
            result.put("stats", cardSyncService.getSyncStats());

            log.info("✅ Cards sync completed: {}/{} cards in {}ms", totalSynced, totalCards, duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            result.put("success", false);
            result.put("synced_count", totalSynced);
            result.put("error_count", totalErrors);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync all data (orders + cards)
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("🔄 Starting complete sync...");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Sync orders
            log.info("📦 Step 1/2: Syncing orders...");
            ResponseEntity<Map<String, Object>> ordersResponse = syncOrders();
            result.put("orders", ordersResponse.getBody());

            // Step 2: Sync cards
            log.info("🎴 Step 2/2: Syncing cards...");
            ResponseEntity<Map<String, Object>> cardsResponse = syncCards(null, null);
            result.put("cards", cardsResponse.getBody());

            long duration = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("duration_ms", duration);
            result.put("message", "Complete sync successful");

            log.info("✅ Complete sync finished in {}ms", duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error during complete sync", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Check Symfony API connection
     * GET /api/sync/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> result = new HashMap<>();

        try {
            String url = symfonyApiUrl + "/api/planning/export/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            result.put("symfony_api", "connected");
            result.put("symfony_url", symfonyApiUrl);
            result.put("symfony_status", response);
            result.put("status", "healthy");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("symfony_api", "disconnected");
            result.put("symfony_url", symfonyApiUrl);
            result.put("status", "unhealthy");
            result.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
    }

    /**
     * Get sync statistics
     * GET /api/sync/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // Order statistics
        long totalOrders = orderRepository.count();
        stats.put("total_orders", totalOrders);

        // Card statistics from sync service
        stats.put("card_stats", cardSyncService.getSyncStats());

        stats.put("message", "Sync statistics retrieved successfully");

        return ResponseEntity.ok(stats);
    }

    /**
     * Create or update Order from Symfony data
     */
    private Order createOrUpdateOrder(Map<String, Object> data) {
        try {
            String orderIdHex = (String) data.get("id");
            if (orderIdHex == null || orderIdHex.isEmpty()) {
                log.warn("⚠️ Order has no id, skipping");
                return null;
            }

// Use Symfony ID directly as the primary key
            UUID orderId = UlidConverter.hexToUuid(orderIdHex);

// Try to find existing order by this ID
            Order order = orderRepository.findById(orderId)
                    .orElse(new Order());

// Set the Symfony ID as primary key
            order.setId(orderId);

            // Map fields from API
            order.setOrderNumber((String) data.get("order_number"));
            order.setTotalCards(getInteger(data, "total_cards", 0));
            order.setStatus(getInteger(data, "status", 1));
            order.setDelai((String) data.get("delai"));
            order.setPrice(getFloat(data, "price", 0.0f));

            // Parse delivery_date (stored as String in database)
            String deliveryDateStr = (String) data.get("delivery_date");
            if (deliveryDateStr != null) {
                order.setDeliveryDate(deliveryDateStr);
            }

            // Parse order_date
            String orderDateStr = (String) data.get("date");
            if (orderDateStr != null) {
                try {
                    order.setOrderDate(LocalDate.parse(orderDateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    log.warn("⚠️ Invalid order date format: {}", orderDateStr);
                }
            }

            return order;

        } catch (Exception e) {
            log.error("❌ Error creating order from data", e);
            return null;
        }
    }

    /**
     * Helper: Get integer value with default
     */
    private Integer getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Helper: Get float value with default
     */
    private Float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Helper: Get double value with default
     */
    private Double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}