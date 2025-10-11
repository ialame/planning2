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
 * Minimal Sync Controller - Syncs only Orders from Symfony
 *
 * IMPORTANT: We only sync Orders, NOT Cards!
 * - Card table is a read-only catalog shared with other projects
 * - Order table contains all info needed for planning (total_cards, delai, status)
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
     * Sync orders from Symfony
     * POST /api/sync/orders
     *
     * This is the main sync endpoint - syncs all orders needed for planning
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
     * Sync all data (currently only orders, since cards are read-only catalog)
     * POST /api/sync/all
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll() {
        log.info("🔄 Starting complete sync...");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // For now, we only sync orders
            // Card catalog is read-only and doesn't need sync
            ResponseEntity<Map<String, Object>> ordersResponse = syncOrders();
            result.put("orders", ordersResponse.getBody());

            long duration = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("duration_ms", duration);
            result.put("message", "Complete sync successful (orders only)");
            result.put("note", "Card catalog is read-only and does not require sync");

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
        stats.put("message", "Sync statistics");
        stats.put("synced_entities", "orders");
        stats.put("note", "Card catalog is read-only and managed separately");
        return ResponseEntity.ok(stats);
    }
}