package com.pcagrade.order.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Simple Order Controller - Fixed for actual database schema
 * Uses direct SQL queries via EntityManager
 * Provides API endpoints for frontend Orders page
 */
@RestController
@RequestMapping("/api/orders")
public class SimpleOrderController {

    private static final Logger log = LoggerFactory.getLogger(SimpleOrderController.class);

    @Autowired
    private EntityManager entityManager;

    /**
     * GET /api/orders
     * Main endpoint for orders list with pagination and filters
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size,
            @RequestParam(required = false) String delai,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String search
    ) {
        try {
            log.info("📦 GET /api/orders - page: {}, size: {}, delai: {}, status: {}, search: {}",
                    page, size, delai, status, search);

            // Build base query - using actual column names from database
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT 
                    HEX(o.id) as id, 
                    o.order_number as orderNumber, 
                    o.delai, 
                    o.status,
                    o.date as creationDate,
                    o.symfony_order_id as reference,
                    o.customer_name as clientOrderNumber,
                    COALESCE(o.total_cards, 0) as cardCount,
                    COALESCE(o.total_cards, 0) as cardsWithName,
                    COALESCE(o.price, 0) as totalPrice
                FROM `order` o
                WHERE 1=1
                """);

            List<Object> parameters = new ArrayList<>();
            int paramIndex = 1;

            // Apply filters
            if (delai != null && !delai.isEmpty() && !"all".equals(delai)) {
                sqlBuilder.append(" AND o.delai = ?").append(paramIndex++);
                parameters.add(delai);
            }

            if (status != null) {
                sqlBuilder.append(" AND o.status = ?").append(paramIndex++);
                parameters.add(status);
            }

            if (search != null && !search.trim().isEmpty()) {
                sqlBuilder.append(" AND (o.order_number LIKE ?").append(paramIndex)
                        .append(" OR o.customer_name LIKE ?").append(paramIndex + 1).append(")");
                String searchPattern = "%" + search + "%";
                parameters.add(searchPattern);
                parameters.add(searchPattern);
            }

            // Count total without pagination
            String countSql = "SELECT COUNT(*) FROM `order` o WHERE 1=1" +
                    sqlBuilder.toString().substring(sqlBuilder.indexOf("WHERE 1=1") + 9);

            Query countQuery = entityManager.createNativeQuery(countSql);
            for (int i = 0; i < parameters.size(); i++) {
                countQuery.setParameter(i + 1, parameters.get(i));
            }
            int total = ((Number) countQuery.getSingleResult()).intValue();

            // Add pagination
            sqlBuilder.append(" ORDER BY o.date DESC, o.id DESC");
            sqlBuilder.append(" LIMIT ").append(size).append(" OFFSET ").append(page * size);

            // Execute main query
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                query.setParameter(i + 1, parameters.get(i));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            // Map to order objects
            List<Map<String, Object>> orders = new ArrayList<>();
            int pageCardTotal = 0;

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", row[0]);
                order.put("orderNumber", row[1]);
                order.put("delai", row[2]);
                order.put("status", row[3]);
                order.put("creationDate", row[4] != null ? row[4].toString() : "");
                order.put("reference", row[5]);
                order.put("clientOrderNumber", row[6]);

                int cardCount = row[7] != null ? ((Number) row[7]).intValue() : 0;
                int cardsWithName = row[8] != null ? ((Number) row[8]).intValue() : 0;

                order.put("cardCount", cardCount);
                order.put("cardsWithName", cardsWithName);
                order.put("namePercentage", cardCount > 0 ? Math.round((cardsWithName / (float) cardCount) * 100) : 0);
                order.put("totalPrice", row[9] != null ? ((Number) row[9]).doubleValue() : 0.0);

                orders.add(order);
                pageCardTotal += cardCount;
            }

            // Calculate total cards across all orders (not just current page)
            String cardSql = "SELECT SUM(COALESCE(o.total_cards, 0)) FROM `order` o WHERE 1=1" +
                    sqlBuilder.toString().substring(sqlBuilder.indexOf("WHERE 1=1") + 9,
                            sqlBuilder.indexOf("ORDER BY"));

            Query cardQuery = entityManager.createNativeQuery(cardSql);
            for (int i = 0; i < parameters.size(); i++) {
                cardQuery.setParameter(i + 1, parameters.get(i));
            }
            Number totalCardsResult = (Number) cardQuery.getSingleResult();
            int totalCards = totalCardsResult != null ? totalCardsResult.intValue() : 0;

            // ========== CALCULATE STATUS STATISTICS - ON ALL ORDERS ==========
            // This is the FIX: calculate stats on ALL orders, not just current page
            String statusStatsSql = """
                SELECT o.status, COUNT(*) as count
                FROM `order` o
                WHERE 1=1
                """ + (delai != null && !delai.isEmpty() && !"all".equals(delai) ? " AND o.delai = ?" : "") +
                    (status != null ? " AND o.status = ?" : "") +
                    (search != null && !search.trim().isEmpty() ?
                            " AND (o.order_number LIKE ? OR o.customer_name LIKE ?)" : "") +
                    " GROUP BY o.status";

            Query statusQuery = entityManager.createNativeQuery(statusStatsSql);
            for (int i = 0; i < parameters.size(); i++) {
                statusQuery.setParameter(i + 1, parameters.get(i));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> statusResults = statusQuery.getResultList();

            // Build status statistics map
            Map<Integer, Long> statusCounts = new HashMap<>();
            for (Object[] row : statusResults) {
                Integer statusCode = ((Number) row[0]).intValue();
                Long count = ((Number) row[1]).longValue();
                statusCounts.put(statusCode, count);
            }

            // Calculate grouped statistics
            long toReceive = statusCounts.getOrDefault(1, 0L);  // STATUS_A_RECEPTIONNER = 1
            long packageAccepted = statusCounts.getOrDefault(9, 0L);  // STATUS_COLIS_ACCEPTE = 9

            // In Processing: A_SCANNER(10), A_OUVRIR(11), A_NOTER(2), A_CERTIFIER(3),
            //                A_PREPARER(4), A_DESCELLER(7), A_VOIR(6)
            long inProcessing = Stream.of(10, 11, 2, 3, 4, 7, 6)
                    .mapToLong(s -> statusCounts.getOrDefault(s, 0L))
                    .sum();

            // To Deliver: A_DISTRIBUER(41), A_ENVOYER(42)
            long toDeliver = Stream.of(41, 42)
                    .mapToLong(s -> statusCounts.getOrDefault(s, 0L))
                    .sum();

            // Completed: ENVOYEE(5), RECU(8)
            long completed = Stream.of(5, 8)
                    .mapToLong(s -> statusCounts.getOrDefault(s, 0L))
                    .sum();

            Map<String, Long> statusStats = Map.of(
                    "toReceive", toReceive,
                    "packageAccepted", packageAccepted,
                    "inProcessing", inProcessing,
                    "toDeliver", toDeliver,
                    "completed", completed
            );

            log.info("📊 Status Statistics: toReceive={}, packageAccepted={}, inProcessing={}, toDeliver={}, completed={}",
                    toReceive, packageAccepted, inProcessing, toDeliver, completed);

            // ========== END FIX ==========

            // Pagination metadata
            int totalPages = (int) Math.ceil((double) total / size);
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("total", total);
            pagination.put("totalPages", totalPages);
            pagination.put("hasNext", page < totalPages - 1);
            pagination.put("hasPrevious", page > 0);
            pagination.put("pageCardTotal", pageCardTotal);
            pagination.put("totalCards", totalCards);

            // Calculate delai distribution
            Map<String, Long> delaiStats = orders.stream()
                    .collect(HashMap::new,
                            (map, order) -> {
                                String d = (String) order.get("delai");
                                map.merge(d != null ? d : "UNKNOWN", 1L, Long::sum);
                            },
                            HashMap::putAll);

            // Complete response with STATUS STATISTICS
            Map<String, Object> response = new HashMap<>();
            response.put("orders", orders);
            response.put("pagination", pagination);
            response.put("delaiDistribution", delaiStats);
            response.put("statusStats", statusStats);  // ← THIS IS THE KEY FIX
            response.put("filters", Map.of(
                    "delai", delai != null ? delai : "all",
                    "status", status != null ? status : "all",
                    "search", search != null ? search : ""
            ));

            log.info("✅ Returned {} orders (page {}/{}) with status statistics",
                    orders.size(), page + 1, totalPages);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading orders", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orders", new ArrayList<>());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * GET /api/orders/{id}
     * Get single order details
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String id) {
        try {
            log.info("📋 GET /api/orders/{}", id);

            String sql = """
                SELECT 
                    HEX(o.id) as id,
                    o.order_number as orderNumber,
                    o.delai,
                    o.status,
                    o.date as creationDate,
                    o.symfony_order_id as reference,
                    o.customer_name as clientOrderNumber,
                    COALESCE(o.total_cards, 0) as cardCount,
                    COALESCE(o.price, 0) as totalPrice
                FROM `order` o
                WHERE HEX(o.id) = ?
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, id.toUpperCase().replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (results.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Order not found");
                return ResponseEntity.status(404).body(errorResponse);
            }

            Object[] row = results.get(0);
            Map<String, Object> order = new HashMap<>();
            order.put("id", row[0]);
            order.put("orderNumber", row[1]);
            order.put("delai", row[2]);
            order.put("status", row[3]);
            order.put("creationDate", row[4] != null ? row[4].toString() : "");
            order.put("reference", row[5]);
            order.put("clientOrderNumber", row[6]);
            order.put("cardCount", row[7] != null ? ((Number) row[7]).intValue() : 0);
            order.put("totalPrice", row[8] != null ? ((Number) row[8]).doubleValue() : 0.0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading order {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * GET /api/orders/{id}/cards
     * Get cards for a specific order
     */
    @GetMapping("/{id}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String id) {
        try {
            log.info("🃏 GET /api/orders/{}/cards", id);

            // This would need to query card_certification_order table
            // For now, return empty response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", id);
            response.put("cards", new ArrayList<>());
            response.put("totalCards", 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading cards for order {}", id, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}