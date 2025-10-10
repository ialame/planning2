package com.pcagrade.order.controller;

import com.pcagrade.order.service.CardSyncService;
import com.pcagrade.order.service.OrderSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for synchronizing data from Symfony API
 * Handles orders and cards synchronization with proper error handling
 */
@RestController
@RequestMapping("/api/sync")
public class DataSyncController {

    private static final Logger log = LoggerFactory.getLogger(DataSyncController.class);

    private final RestTemplate restTemplate;
    private final OrderSyncService orderSyncService;
    private final CardSyncService cardSyncService;

    @Value("${symfony.api.base-url}")
    private String symfonyApiUrl;

    private static final int CARD_BATCH_SIZE = 50; // Process cards in batches of 50

    public DataSyncController(
            RestTemplate restTemplate,
            OrderSyncService orderSyncService,
            CardSyncService cardSyncService) {
        this.restTemplate = restTemplate;
        this.orderSyncService = orderSyncService;
        this.cardSyncService = cardSyncService;
    }

    /**
     * Sync all data: orders and their cards
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("🔄 Starting complete data synchronization...");

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Sync orders
            log.info("📦 Step 1: Syncing orders...");
            ResponseEntity<Map<String, Object>> ordersResult = syncOrders();
            response.put("orders", ordersResult.getBody());

            // Step 2: Sync cards
            log.info("🎴 Step 2: Syncing cards...");
            ResponseEntity<Map<String, Object>> cardsResult = syncCards();
            response.put("cards", cardsResult.getBody());

            long duration = System.currentTimeMillis() - startTime;
            response.put("success", true);
            response.put("duration_ms", duration);
            response.put("message", "Complete synchronization successful");

            log.info("✅ Complete synchronization finished in {}ms", duration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error during complete synchronization", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync only orders from Symfony API
     * POST /api/sync/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> syncOrders() {
        log.info("🔄 Starting orders synchronization...");

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Fetch orders from Symfony API
            String ordersUrl = symfonyApiUrl + "/api/export/orders";
            log.info("📡 Fetching orders from: {}", ordersUrl);

            Map<String, Object> symfonyResponse = restTemplate.getForObject(ordersUrl, Map.class);

            if (symfonyResponse == null || !symfonyResponse.containsKey("orders")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> ordersData = (List<Map<String, Object>>) symfonyResponse.get("orders");
            log.info("📦 Received {} orders from Symfony", ordersData.size());

            // Sync orders using service
            int syncedCount = orderSyncService.syncOrders(ordersData);

            long duration = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("total_orders", ordersData.size());
            response.put("synced_count", syncedCount);
            response.put("duration_ms", duration);
            response.put("message", String.format("Successfully synced %d orders", syncedCount));

            log.info("✅ Orders synchronization completed: {} orders in {}ms", syncedCount, duration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync only cards from Symfony API
     * POST /api/sync/cards
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards() {
        log.info("🔄 Starting cards synchronization...");

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();
        int totalSynced = 0;
        int totalErrors = 0;

        try {
            // Fetch cards from Symfony API
            String cardsUrl = symfonyApiUrl + "/api/export/cards";
            log.info("📡 Fetching cards from: {}", cardsUrl);

            Map<String, Object> symfonyResponse = restTemplate.getForObject(cardsUrl, Map.class);

            if (symfonyResponse == null || !symfonyResponse.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) symfonyResponse.get("cards");
            log.info("🎴 Received {} cards from Symfony", cardsData.size());

            // Process cards in batches to avoid memory issues and transaction timeouts
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
                    // Continue with next batch instead of failing completely
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            response.put("success", true);
            response.put("total_cards", totalCards);
            response.put("synced_count", totalSynced);
            response.put("error_count", totalErrors);
            response.put("duration_ms", duration);
            response.put("message", String.format("Successfully synced %d/%d cards", totalSynced, totalCards));

            log.info("✅ Cards synchronization completed: {}/{} cards in {}ms",
                    totalSynced, totalCards, duration);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            response.put("success", false);
            response.put("synced_count", totalSynced);
            response.put("error_count", totalErrors);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync cards for a specific order
     * POST /api/sync/cards/order/{orderId}
     */
    @PostMapping("/cards/order/{orderId}")
    public ResponseEntity<Map<String, Object>> syncCardsForOrder(@PathVariable Long orderId) {
        log.info("🔄 Starting card synchronization for order {}...", orderId);

        Map<String, Object> response = new HashMap<>();

        try {
            // Fetch cards for specific order
            String cardsUrl = symfonyApiUrl + "/api/export/cards?order_id=" + orderId;
            log.info("📡 Fetching cards from: {}", cardsUrl);

            Map<String, Object> symfonyResponse = restTemplate.getForObject(cardsUrl, Map.class);

            if (symfonyResponse == null || !symfonyResponse.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) symfonyResponse.get("cards");
            log.info("🎴 Received {} cards for order {}", cardsData.size(), orderId);

            // Delete existing cards for this order first
            cardSyncService.deleteCardsForOrder(orderId);

            // Sync new cards
            int syncedCount = cardSyncService.syncCardsBatch(cardsData, CARD_BATCH_SIZE);

            response.put("success", true);
            response.put("order_id", orderId);
            response.put("synced_count", syncedCount);
            response.put("message", String.format("Successfully synced %d cards for order %d",
                    syncedCount, orderId));

            log.info("✅ Cards synced for order {}: {} cards", orderId, syncedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error syncing cards for order {}", orderId, e);
            response.put("success", false);
            response.put("order_id", orderId);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Health check for Symfony API connection
     * GET /api/sync/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();

        try {
            String healthUrl = symfonyApiUrl + "/api/health";
            restTemplate.getForObject(healthUrl, String.class);

            response.put("symfony_api", "connected");
            response.put("symfony_url", symfonyApiUrl);
            response.put("status", "healthy");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("symfony_api", "disconnected");
            response.put("symfony_url", symfonyApiUrl);
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}