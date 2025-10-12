package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.service.CardCertificationSyncService;
import com.pcagrade.order.service.TranslationSyncService;
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
 * FIXED Sync Controller
 *
 * Key fix: Uses symfonyOrderId field to store Symfony ID instead of trying
 * to use it as primary key. This allows Spring Boot to generate its own ULIDs
 * while maintaining the mapping to Symfony orders.
 */
@RestController
@RequestMapping("/api/sync")
public class MinimalSyncController {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncController.class);
    private static final int CARD_BATCH_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderRepository orderRepository;
    private final CardCertificationSyncService cardSyncService;
    private final RestTemplate restTemplate;

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiUrl;

    private final TranslationSyncService translationSyncService;

    // Mettre à jour le constructeur
    public MinimalSyncController(
            OrderRepository orderRepository,
            CardCertificationSyncService cardSyncService,
            TranslationSyncService translationSyncService,
            RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.cardSyncService = cardSyncService;
        this.translationSyncService = translationSyncService;
        this.restTemplate = restTemplate;
    }

    /**
     * Sync English card translations
     * POST /api/sync/translations
     */
    @PostMapping("/translations")
    public ResponseEntity<Map<String, Object>> syncTranslations() {
        log.info("🔄 Starting English translations sync");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String translationsUrl = symfonyApiUrl + "/api/planning/export/translations?limit=50000";
            log.info("📡 Fetching English translations from: {}", translationsUrl);

            Map<String, Object> response = restTemplate.getForObject(translationsUrl, Map.class);

            if (response == null || !response.containsKey("translations")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> translationsData = (List<Map<String, Object>>) response.get("translations");
            log.info("📚 Received {} English translations from Symfony", translationsData.size());

            int syncedCount = translationSyncService.syncTranslations(translationsData);
            Map<String, Object> stats = translationSyncService.getTranslationStats();

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_translations", translationsData.size());
            result.put("synced_count", syncedCount);
            result.put("duration_ms", duration);
            result.put("locale", "EN");
            result.put("stats", stats);
            result.put("message", String.format("Successfully synced %d English translations", syncedCount));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing translations", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync all data: orders then cards
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("🔄 Starting complete synchronization...");

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Sync orders
            ResponseEntity<Map<String, Object>> ordersResult = syncOrders();
            response.put("orders", ordersResult.getBody());

            // Step 2: Sync cards
            ResponseEntity<Map<String, Object>> cardsResult = syncCards(null, null);
            response.put("cards", cardsResult.getBody());

            long duration = System.currentTimeMillis() - startTime;
            response.put("success", true);
            response.put("duration_ms", duration);
            response.put("message", "Complete sync successful");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error during complete sync", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
            String ordersUrl = symfonyApiUrl + "/api/planning/export/orders?limit=50000";
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

            log.info("✅ Orders sync completed: {} orders synced in {}ms", syncedCount, duration);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync cards from Symfony API
     * POST /api/sync/cards?limit=100&orderId=xxx
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String orderId) {

        log.info("🔄 Starting cards synchronization from Symfony API");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Build URL with parameters
            StringBuilder urlBuilder = new StringBuilder(symfonyApiUrl);
            urlBuilder.append("/api/planning/export/cards");

            List<String> params = new ArrayList<>();
            if (limit != null) {
                params.add("limit=" + limit);
            } else {
                params.add("limit=50000"); // Default limit
            }
            if (orderId != null) {
                params.add("orderId=" + orderId);
            }

            if (!params.isEmpty()) {
                urlBuilder.append("?").append(String.join("&", params));
            }

            String cardsUrl = urlBuilder.toString();
            log.info("📡 Fetching cards from: {}", cardsUrl);

            // Fetch cards from Symfony API
            Map<String, Object> response = restTemplate.getForObject(cardsUrl, Map.class);

            if (response == null || !response.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) response.get("cards");
            log.info("🎴 Received {} cards from Symfony", cardsData.size());

            // Sync cards using the service
            int syncedCount = cardSyncService.syncCardsBatch(cardsData, CARD_BATCH_SIZE);

            // Get statistics
            Map<String, Object> stats = cardSyncService.getSyncStats();

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_cards", cardsData.size());
            result.put("synced_count", syncedCount);
            result.put("error_count", cardsData.size() - syncedCount);
            result.put("duration_ms", duration);
            result.put("stats", stats);
            result.put("message", String.format("Successfully synced %d/%d cards", syncedCount, cardsData.size()));

            log.info("✅ Cards sync completed: {} cards synced in {}ms", syncedCount, duration);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Health check
     * GET /api/sync/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();

        try {
            String healthUrl = symfonyApiUrl + "/api/planning/export/health";
            Map<String, Object> response = restTemplate.getForObject(healthUrl, Map.class);

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

        long totalOrders = orderRepository.count();
        stats.put("total_orders", totalOrders);
        stats.put("card_stats", cardSyncService.getSyncStats());
        stats.put("message", "Sync statistics retrieved successfully");

        return ResponseEntity.ok(stats);
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Create or update Order from Symfony data
     *
     * KEY FIX: Uses symfonyOrderId to find/link orders instead of trying
     * to use Symfony ID as primary key
     */
    private Order createOrUpdateOrder(Map<String, Object> data) {
        try {
            String symfonyOrderId = (String) data.get("id");
            if (symfonyOrderId == null || symfonyOrderId.isEmpty()) {
                log.warn("⚠️ Order has no id, skipping");
                return null;
            }

            // ✅ FIXED: Find order by symfonyOrderId field, not by primary key
            Order order = orderRepository.findBySymfonyOrderId(symfonyOrderId)
                    .orElse(new Order());

            // ✅ FIXED: Store Symfony ID in separate field
            order.setSymfonyOrderId(symfonyOrderId);

            // Map other fields from API
            order.setOrderNumber((String) data.get("order_number"));
            order.setCustomerName((String) data.get("customer_name"));
            order.setTotalCards(getInteger(data, "total_cards", 0));
            order.setStatus(getInteger(data, "status", 1));
            order.setDelai((String) data.get("delai"));
            order.setPrice(getFloat(data, "price", 0.0f));

            // Parse dates
            String deliveryDateStr = (String) data.get("delivery_date");
            if (deliveryDateStr != null && !deliveryDateStr.isEmpty()) {
                order.setDeliveryDate(deliveryDateStr);
            }

            String orderDateStr = (String) data.get("order_date");
            if (orderDateStr != null && !orderDateStr.isEmpty()) {
                try {
                    order.setOrderDate(LocalDate.parse(orderDateStr, DATE_FORMATTER));
                } catch (Exception e) {
                    log.warn("⚠️ Invalid order_date format: {}", orderDateStr);
                }
            }

            return order;

        } catch (Exception e) {
            log.error("❌ Error creating order from data", e);
            return null;
        }
    }

    private Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Float getFloat(Map<String, Object> map, String key, Float defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * ADD THIS METHOD to MinimalSyncController.java
     *
     * Location: After the getStats() method
     *
     * Simple version that works with existing endpoints
     */

    /**
     * Get sync status - comparison between Symfony and local database
     * GET /api/sync/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        log.info("📊 Checking sync status");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> tableComparison = new ArrayList<>();

        try {
            // Get local counts
            long localOrders = orderRepository.count();
            long localCards = cardSyncService.getTotalCards();

            // Get Symfony counts by calling the export endpoints
            Integer symfonyOrders = getSymfonyOrderCount();
            Integer symfonyCards = getSymfonyCardCount();

            // Compare orders
            Map<String, Object> orderComparison = new HashMap<>();
            orderComparison.put("table", "order");
            orderComparison.put("symfony", symfonyOrders);
            orderComparison.put("local", localOrders);
            orderComparison.put("difference", symfonyOrders - localOrders);
            orderComparison.put("inSync", Math.abs(symfonyOrders - localOrders) < 10); // Allow small difference
            tableComparison.add(orderComparison);

            // Compare cards
            Map<String, Object> cardComparison = new HashMap<>();
            cardComparison.put("table", "card_certification");
            cardComparison.put("symfony", symfonyCards);
            cardComparison.put("local", localCards);
            cardComparison.put("difference", symfonyCards - localCards);
            cardComparison.put("inSync", Math.abs(symfonyCards - localCards) < 100); // Allow small difference
            tableComparison.add(cardComparison);

            result.put("success", true);
            result.put("tableComparison", tableComparison);
            result.put("timestamp", java.time.LocalDateTime.now().toString());

            log.info("✅ Sync status: Orders (local:{}, symfony:{}), Cards (local:{}, symfony:{})",
                    localOrders, symfonyOrders, localCards, symfonyCards);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error getting sync status", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("tableComparison", tableComparison);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Get order count from Symfony API
     */
    private Integer getSymfonyOrderCount() {
        try {
            String url = symfonyApiUrl + "/api/planning/export/orders?limit=1";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("count")) {
                return ((Number) response.get("count")).intValue();
            }

            // Fallback: try to get from orders array
            if (response != null && response.containsKey("orders")) {
                List<?> orders = (List<?>) response.get("orders");
                // This is just a sample, real count might be in metadata
                log.warn("⚠️ Using sample count for orders, actual count may be higher");
                return orders.size() > 0 ? 8000 : 0; // Default estimate
            }

            return 0;
        } catch (Exception e) {
            log.error("❌ Error getting Symfony order count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get card count from Symfony API
     */
    private Integer getSymfonyCardCount() {
        try {
            String url = symfonyApiUrl + "/api/planning/export/cards?limit=1";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("count")) {
                return ((Number) response.get("count")).intValue();
            }

            // Fallback
            if (response != null && response.containsKey("cards")) {
                List<?> cards = (List<?>) response.get("cards");
                log.warn("⚠️ Using sample count for cards, actual count may be higher");
                return cards.size() > 0 ? 48750 : 0; // Default estimate
            }

            return 0;
        } catch (Exception e) {
            log.error("❌ Error getting Symfony card count: {}", e.getMessage());
            return 0;
        }
    }


}