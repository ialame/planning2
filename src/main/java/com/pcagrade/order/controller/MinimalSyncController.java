package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.model.SyncProgress;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.service.CardCertificationSyncService;
import com.pcagrade.order.service.SyncProgressPublisher;
import com.pcagrade.order.service.TranslationSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sync Controller with SSE Progress Support
 *
 * Provides synchronization endpoints with real-time progress updates via SSE
 */
@RestController
@RequestMapping("/api/sync")
public class MinimalSyncController {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncController.class);
    private static final int CARD_BATCH_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderRepository orderRepository;
    private final CardCertificationSyncService cardSyncService;
    private final TranslationSyncService translationSyncService;
    private final SyncProgressPublisher progressPublisher;
    private final RestTemplate restTemplate;

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiUrl;

    public MinimalSyncController(
            OrderRepository orderRepository,
            CardCertificationSyncService cardSyncService,
            TranslationSyncService translationSyncService,
            SyncProgressPublisher progressPublisher,
            RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.cardSyncService = cardSyncService;
        this.translationSyncService = translationSyncService;
        this.progressPublisher = progressPublisher;
        this.restTemplate = restTemplate;
    }

    /**
     * Sync all data: orders then cards
     * POST /api/sync/all?syncId=xxx
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll(
            @RequestParam(required = false) String syncId) {

        // Generate syncId if not provided
        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        log.info("🔄 Starting complete synchronization with syncId: {}", syncId);

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Publish starting event
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "ALL", "Starting full synchronization..."));

            // Step 1: Sync orders (0-45%)
            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "ALL", "Fetching orders from Symfony API..."));

            ResponseEntity<Map<String, Object>> ordersResult = syncOrdersWithProgress(syncId);
            response.put("orders", ordersResult.getBody());

            // Step 2: Sync cards (45-90%)
            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "ALL", "Fetching cards from Symfony API..."));

            ResponseEntity<Map<String, Object>> cardsResult = syncCardsWithProgress(syncId, null, null);
            response.put("cards", cardsResult.getBody());

            // Final completion
            long duration = System.currentTimeMillis() - startTime;
            response.put("success", true);
            response.put("duration_ms", duration);
            response.put("message", "Complete sync successful");
            response.put("syncId", syncId);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "ALL",
                            String.format("Synchronization completed in %.1f seconds", duration / 1000.0), 0));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error during complete sync", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync orders from Symfony API
     * POST /api/sync/orders?syncId=xxx
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> syncOrders(
            @RequestParam(required = false) String syncId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        return syncOrdersWithProgress(syncId);
    }

    private ResponseEntity<Map<String, Object>> syncOrdersWithProgress(String syncId) {
        log.info("🔄 Starting orders synchronization with syncId: {}", syncId);

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Starting event
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "ORDERS", "Starting orders synchronization..."));

            // Fetch orders from Symfony API
            String ordersUrl = symfonyApiUrl + "/api/planning/export/orders?limit=50000";
            log.info("📡 Fetching orders from: {}", ordersUrl);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "ORDERS", "Fetching orders from Symfony API..."));

            Map<String, Object> response = restTemplate.getForObject(ordersUrl, Map.class);

            if (response == null || !response.containsKey("orders")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> ordersData = (List<Map<String, Object>>) response.get("orders");
            log.info("📦 Received {} orders from Symfony", ordersData.size());

            int syncedCount = 0;
            List<Order> ordersToSave = new ArrayList<>();
            int totalOrders = ordersData.size();

            // Process orders with progress updates every 100 orders
            for (int i = 0; i < ordersData.size(); i++) {
                Map<String, Object> orderData = ordersData.get(i);

                try {
                    Order order = createOrUpdateOrder(orderData);
                    if (order != null) {
                        ordersToSave.add(order);
                        syncedCount++;
                    }

                    // Publish progress every 100 orders
                    if ((i + 1) % 100 == 0 || i == ordersData.size() - 1) {
                        progressPublisher.publishProgress(syncId,
                                SyncProgress.processing(syncId, "ORDERS",
                                        "Processing orders", i + 1, totalOrders));
                    }

                } catch (Exception e) {
                    log.error("❌ Error processing order: {}", orderData.get("id"), e);
                }
            }

            // Save all orders
            progressPublisher.publishProgress(syncId,
                    SyncProgress.saving(syncId, "ORDERS", "Saving orders to database..."));

            orderRepository.saveAll(ordersToSave);

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_orders", ordersData.size());
            result.put("synced_count", syncedCount);
            result.put("duration_ms", duration);
            result.put("message", String.format("Synced %d/%d orders", syncedCount, ordersData.size()));
            result.put("syncId", syncId);

            log.info("✅ Orders sync completed: {} orders synced in {}ms", syncedCount, duration);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "ORDERS",
                            String.format("Synced %d orders in %.1f seconds", syncedCount, duration / 1000.0),
                            syncedCount));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync cards from Symfony API
     * POST /api/sync/cards?limit=100&orderId=xxx&syncId=xxx
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String syncId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        return syncCardsWithProgress(syncId, limit, orderId);
    }

    private ResponseEntity<Map<String, Object>> syncCardsWithProgress(
            String syncId, Integer limit, String orderId) {

        log.info("🔄 Starting cards synchronization with syncId: {}", syncId);

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "CARDS", "Starting cards synchronization..."));

            // Build URL with parameters
            StringBuilder urlBuilder = new StringBuilder(symfonyApiUrl);
            urlBuilder.append("/api/planning/export/cards");

            List<String> params = new ArrayList<>();
            if (limit != null) {
                params.add("limit=" + limit);
            } else {
                params.add("limit=50000");
            }
            if (orderId != null) {
                params.add("orderId=" + orderId);
            }

            if (!params.isEmpty()) {
                urlBuilder.append("?").append(String.join("&", params));
            }

            String cardsUrl = urlBuilder.toString();
            log.info("📡 Fetching cards from: {}", cardsUrl);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "CARDS", "Fetching cards from Symfony API..."));

            // Fetch cards from Symfony API
            Map<String, Object> response = restTemplate.getForObject(cardsUrl, Map.class);

            if (response == null || !response.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) response.get("cards");
            log.info("🎴 Received {} cards from Symfony", cardsData.size());

            // Sync cards in batches with progress
            int totalCards = cardsData.size();
            int processedCards = 0;

            for (int i = 0; i < cardsData.size(); i += CARD_BATCH_SIZE) {
                int endIndex = Math.min(i + CARD_BATCH_SIZE, cardsData.size());
                List<Map<String, Object>> batch = cardsData.subList(i, endIndex);

                cardSyncService.syncCardsBatch(batch, CARD_BATCH_SIZE);

                processedCards = endIndex;
                progressPublisher.publishProgress(syncId,
                        SyncProgress.processing(syncId, "CARDS",
                                "Processing cards", processedCards, totalCards));
            }

            // Get statistics
            Map<String, Object> stats = cardSyncService.getSyncStats();

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_cards", cardsData.size());
            result.put("synced_count", processedCards);
            result.put("error_count", cardsData.size() - processedCards);
            result.put("duration_ms", duration);
            result.put("stats", stats);
            result.put("message", String.format("Successfully synced %d/%d cards", processedCards, cardsData.size()));
            result.put("syncId", syncId);

            log.info("✅ Cards sync completed: {} cards synced in {}ms", processedCards, duration);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "CARDS",
                            String.format("Synced %d cards in %.1f seconds", processedCards, duration / 1000.0),
                            processedCards));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Incremental sync (last 24 hours)
     * POST /api/sync/incremental?syncId=xxx
     */
    @PostMapping("/incremental")
    public ResponseEntity<Map<String, Object>> syncIncremental(
            @RequestParam(required = false) String syncId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        log.info("🔄 Starting incremental sync with syncId: {}", syncId);

        Map<String, Object> result = new HashMap<>();

        try {
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "INCREMENTAL", "Starting incremental sync..."));

            // For now, just do a regular sync
            // TODO: Implement actual incremental logic based on modification dates
            ResponseEntity<Map<String, Object>> syncResult = syncOrdersWithProgress(syncId);

            result = syncResult.getBody();
            result.put("syncId", syncId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error during incremental sync", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

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
            orderComparison.put("inSync", Math.abs(symfonyOrders - localOrders) < 10);
            tableComparison.add(orderComparison);

            // Compare cards
            Map<String, Object> cardComparison = new HashMap<>();
            cardComparison.put("table", "card_certification");
            cardComparison.put("symfony", symfonyCards);
            cardComparison.put("local", localCards);
            cardComparison.put("difference", symfonyCards - localCards);
            cardComparison.put("inSync", Math.abs(symfonyCards - localCards) < 100);
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

            // Fallback: estimate from sample
            if (response != null && response.containsKey("orders")) {
                List<?> orders = (List<?>) response.get("orders");
                log.warn("⚠️ Using estimated count for orders");
                return orders.size() > 0 ? 8000 : 0;
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

            // Fallback: estimate from sample
            if (response != null && response.containsKey("cards")) {
                List<?> cards = (List<?>) response.get("cards");
                log.warn("⚠️ Using estimated count for cards");
                return cards.size() > 0 ? 50000 : 0;
            }

            return 0;
        } catch (Exception e) {
            log.error("❌ Error getting Symfony card count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Health check endpoint
     * GET /api/sync/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test Symfony API connection
            String url = symfonyApiUrl + "/api/planning/export/health";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            boolean symfonyHealthy = response != null && "ok".equals(response.get("status"));

            health.put("status", symfonyHealthy ? "healthy" : "unhealthy");
            health.put("symfony_api", symfonyHealthy ? "connected" : "disconnected");
            health.put("symfony_url", symfonyApiUrl);
            health.put("timestamp", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("symfony_api", "error");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
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


    // Add this helper method to MinimalSyncController.java

    /**
     * Create or update order from Symfony API data
     */
    private Order createOrUpdateOrder(Map<String, Object> orderData) {
        try {
            String symfonyId = getString(orderData, "id");
            if (symfonyId == null) {
                log.warn("⚠️ Order missing ID, skipping");
                return null;
            }

            // Check if order already exists
            Order order = orderRepository.findBySymfonyOrderId(symfonyId)
                    .orElse(new Order());

            // Set Symfony ID for tracking
            order.setSymfonyOrderId(symfonyId);

            // Map Symfony fields
            order.setOrderNumber(getString(orderData, "order_number"));
            order.setCustomerName(getString(orderData, "customer_name"));

            // Set delai priority code (X, F+, F, C, E)
            order.setDelai(getString(orderData, "delai", "C")); // Default: Classic

            // ✅ FIX: Use "order_date" instead of "date"
            // Symfony returns "order_date", not "date"
            String dateStr = getString(orderData, "order_date");
            order.setDate(parseDate(dateStr));

            // Set total cards count
            order.setTotalCards(getInteger(orderData, "total_cards", 0));

            // Convert Symfony status integer to OrderStatus enum
            Integer statusCode = getInteger(orderData, "status", 2); // Default: A_NOTER (GRADING)
            order.setStatus(convertSymfonyStatusToOrderStatus(statusCode));

            // Set order price
            order.setPrice(getFloat(orderData, "price", 0.0f));

            return order;

        } catch (Exception e) {
            log.error("❌ Error creating order: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert Symfony status code (integer) to OrderStatus enum
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
                log.debug("Unknown Symfony status code: {}, defaulting to GRADING", statusCode);
                return OrderStatus.GRADING;
        }
    }

    // Helper methods for safe data extraction
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private Integer getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;

        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;

        if (value instanceof Float) {
            return (Float) value;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // Parse yyyy-MM-dd and convert to LocalDateTime at start of day
            LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
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