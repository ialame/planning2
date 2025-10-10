package com.pcagrade.order.controller;

import com.pcagrade.order.service.MinimalSyncService;
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
 * Minimal Sync Controller - Syncs only essential fields from Symfony
 * Simple, fast, and reliable!
 */
@RestController
@RequestMapping("/api/sync")
public class MinimalSyncController {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncController.class);

    private final MinimalSyncService syncService;
    private final RestTemplate restTemplate;

    @Value("${symfony.api.base-url}")
    private String symfonyApiUrl;

    public MinimalSyncController(MinimalSyncService syncService, RestTemplate restTemplate) {
        this.syncService = syncService;
        this.restTemplate = restTemplate;
    }

    /**
     * Sync all data (orders + cards)
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("🔄 Starting complete sync (orders + cards)...");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Sync orders
            log.info("📦 Step 1/2: Syncing orders...");
            ResponseEntity<Map<String, Object>> ordersResponse = syncOrders();
            result.put("orders", ordersResponse.getBody());

            // Step 2: Sync cards
            log.info("🎴 Step 2/2: Syncing cards...");
            ResponseEntity<Map<String, Object>> cardsResponse = syncCards();
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
     * Sync only orders from Symfony
     * POST /api/sync/orders
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> syncOrders() {
        log.info("🔄 Starting orders sync...");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Call Symfony minimal export API
            String url = symfonyApiUrl + "/api/planning/export/orders";
            log.info("📡 Fetching from: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("orders")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> ordersData = (List<Map<String, Object>>) response.get("orders");
            log.info("📦 Received {} orders", ordersData.size());

            // Sync using minimal service
            int syncedCount = syncService.syncOrders(ordersData);

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
     * Sync only cards from Symfony
     * POST /api/sync/cards
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards() {
        log.info("🔄 Starting cards sync...");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Call Symfony minimal export API
            String url = symfonyApiUrl + "/api/planning/export/cards";
            log.info("📡 Fetching from: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) response.get("cards");
            log.info("🎴 Received {} cards", cardsData.size());

            // Sync using minimal service
            int syncedCount = syncService.syncCards(cardsData);

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("total_cards", cardsData.size());
            result.put("synced_count", syncedCount);
            result.put("duration_ms", duration);
            result.put("message", String.format("Synced %d/%d cards", syncedCount, cardsData.size()));

            log.info("✅ Cards sync completed: {} cards in {}ms", syncedCount, duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("symfony_url", symfonyApiUrl);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Sync cards for a specific order
     * POST /api/sync/order/{orderId}/cards
     */
    @PostMapping("/order/{orderId}/cards")
    public ResponseEntity<Map<String, Object>> syncOrderCards(@PathVariable Long orderId) {
        log.info("🔄 Starting cards sync for order {}...", orderId);

        Map<String, Object> result = new HashMap<>();

        try {
            String url = symfonyApiUrl + "/api/planning/export/order/" + orderId + "/cards";
            log.info("📡 Fetching from: {}", url);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("cards")) {
                throw new RuntimeException("Invalid response from Symfony API");
            }

            List<Map<String, Object>> cardsData = (List<Map<String, Object>>) response.get("cards");
            log.info("🎴 Received {} cards for order {}", cardsData.size(), orderId);

            int syncedCount = syncService.syncCards(cardsData);

            result.put("success", true);
            result.put("order_id", orderId);
            result.put("synced_count", syncedCount);
            result.put("message", String.format("Synced %d cards for order %d", syncedCount, orderId));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards for order {}", orderId, e);
            result.put("success", false);
            result.put("order_id", orderId);
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
        // This could be enhanced to show actual sync statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("message", "Sync statistics coming soon");
        return ResponseEntity.ok(stats);
    }
}