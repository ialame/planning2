package com.pcagrade.order.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
                    o.order_date as creationDate,
                    o.symfony_order_id as reference,
                    o.customer_name as clientOrderNumber,
                    COALESCE(o.total_cards, o.card_count, 0) as cardCount,
                    COALESCE(o.total_cards, o.card_count, 0) as cardsWithName,
                    COALESCE(o.price, o.total_price, 0) as totalPrice
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
                String likePattern = "%" + search.trim() + "%";
                parameters.add(likePattern);
                parameters.add(likePattern);
                paramIndex += 2;
            }

            // Order by priority and date
            sqlBuilder.append(" ORDER BY CASE o.delai WHEN 'X' THEN 1 WHEN 'F+' THEN 2 WHEN 'F' THEN 3 WHEN 'C' THEN 4 ELSE 5 END, o.order_date DESC");

            // Count total (for pagination)
            String countSql = "SELECT COUNT(*) FROM `order` o WHERE 1=1";
            if (delai != null && !delai.isEmpty() && !"all".equals(delai)) {
                countSql += " AND o.delai = ?";
            }
            if (status != null) {
                countSql += " AND o.status = ?";
            }
            if (search != null && !search.trim().isEmpty()) {
                countSql += " AND (o.order_number LIKE ? OR o.customer_name LIKE ?)";
            }

            Query countQuery = entityManager.createNativeQuery(countSql);
            int countParamIndex = 1;
            if (delai != null && !delai.isEmpty() && !"all".equals(delai)) {
                countQuery.setParameter(countParamIndex++, delai);
            }
            if (status != null) {
                countQuery.setParameter(countParamIndex++, status);
            }
            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.trim() + "%";
                countQuery.setParameter(countParamIndex++, likePattern);
                countQuery.setParameter(countParamIndex++, likePattern);
            }

            Number totalCount = (Number) countQuery.getSingleResult();
            long total = totalCount.longValue();
            int totalPages = (int) Math.ceil((double) total / size);

            // Add pagination to main query
            sqlBuilder.append(" LIMIT ").append(size).append(" OFFSET ").append(page * size);

            // Execute main query
            Query query = entityManager.createNativeQuery(sqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                query.setParameter(i + 1, parameters.get(i));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            // Convert results to list of maps
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
                float totalPrice = row[9] != null ? ((Number) row[9]).floatValue() : 0.0f;

                order.put("cardCount", cardCount);
                order.put("cardsWithName", cardsWithName);
                order.put("namePercentage", cardCount > 0 ? Math.round((cardsWithName / (float) cardCount) * 100) : 0);
                order.put("totalPrice", totalPrice);

                pageCardTotal += cardCount;
                orders.add(order);
            }

            // Calculate total cards across ALL orders
            String totalCardsSql = "SELECT COALESCE(SUM(COALESCE(o.total_cards, o.card_count, 0)), 0) FROM `order` o WHERE 1=1";
            Query totalCardsQuery = entityManager.createNativeQuery(totalCardsSql);
            Number allCardsTotal = (Number) totalCardsQuery.getSingleResult();
            int totalCards = allCardsTotal != null ? allCardsTotal.intValue() : 0;

            // Pagination metadata
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("total", total);
            pagination.put("totalPages", totalPages);
            pagination.put("hasNext", page < totalPages - 1);
            pagination.put("hasPrevious", page > 0);
            pagination.put("pageCardTotal", pageCardTotal);
            pagination.put("totalCards", totalCards);

            // Quick statistics
            Map<String, Long> delaiStats = new HashMap<>();
            String statsSql = "SELECT o.delai, COUNT(*) FROM `order` o WHERE 1=1 GROUP BY o.delai";
            @SuppressWarnings("unchecked")
            List<Object[]> statsResults = entityManager.createNativeQuery(statsSql).getResultList();
            for (Object[] stat : statsResults) {
                String delaiKey = stat[0] != null ? stat[0].toString() : "unknown";
                Long count = ((Number) stat[1]).longValue();
                delaiStats.put(delaiKey, count);
            }

            // Complete response
            Map<String, Object> response = new HashMap<>();
            response.put("orders", orders);
            response.put("pagination", pagination);
            response.put("delaiDistribution", delaiStats);
            response.put("filters", Map.of(
                    "delai", delai != null ? delai : "all",
                    "status", status != null ? status : "all",
                    "search", search != null ? search : ""
            ));

            log.info("✅ Returned {} orders (page {}/{})", orders.size(), page + 1, totalPages);
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
                    o.order_date as creationDate,
                    o.symfony_order_id as reference,
                    o.customer_name as clientOrderNumber,
                    COALESCE(o.total_cards, o.card_count, 0) as cardCount,
                    COALESCE(o.price, o.total_price, 0) as totalPrice
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
            order.put("totalPrice", row[8] != null ? ((Number) row[8]).floatValue() : 0.0f);

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
            log.info("🎴 GET /api/orders/{}/cards", id);

            // Get total cards from order
            String orderSql = "SELECT COALESCE(total_cards, card_count, 0) FROM `order` WHERE HEX(id) = ?";
            Query orderQuery = entityManager.createNativeQuery(orderSql);
            orderQuery.setParameter(1, id.toUpperCase().replace("-", ""));

            Object result = orderQuery.getSingleResult();
            int totalCards = result != null ? ((Number) result).intValue() : 0;

            // For now, return basic info
            // Card details would come from card_certification_order table if it exists
            List<Map<String, Object>> cards = new ArrayList<>();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", id);
            response.put("cards", cards);
            response.put("totalCards", totalCards);
            response.put("cardsWithName", totalCards);
            response.put("namePercentage", 100);
            response.put("estimatedDuration", totalCards * 3);

            log.info("✅ Order {} has {} cards", id, totalCards);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading cards for order {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orderId", id);
            errorResponse.put("cards", new ArrayList<>());
            errorResponse.put("totalCards", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}