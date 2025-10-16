package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple Order Controller - Using OrderRepository instead of native SQL
 * Provides API endpoints for frontend Orders page
 */
@RestController
@RequestMapping("/api/orders")
public class SimpleOrderController {

    private static final Logger log = LoggerFactory.getLogger(SimpleOrderController.class);

    private final OrderRepository orderRepository;

    public SimpleOrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * GET /api/orders
     * Main endpoint for orders list with pagination and filters
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size,
            @RequestParam(required = false) String delai,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        try {
            log.info("📦 GET /api/orders - page: {}, size: {}, delai: {}, status: {}, search: {}",
                    page, size, delai, status, search);

            // Build pageable with sorting by priority
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            List<Order> orders;

            // Apply filters
            if (delai != null && !delai.isEmpty() && !"all".equals(delai)) {
                orders = orderRepository.findByDelai(delai);
            } else if (status != null && !status.isEmpty() && !"all".equals(status)) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    orders = orderRepository.findByStatus(orderStatus);
                } catch (IllegalArgumentException e) {
                    orders = orderRepository.findAll();
                }
            } else if (search != null && !search.trim().isEmpty()) {
                orders = orderRepository.findByCustomerNameContainingIgnoreCase(search);
            } else {
                orders = orderRepository.findAll();
            }

            // Sort by priority
            orders = orders.stream()
                    .sorted(Comparator.comparingInt(Order::getPriorityScore))
                    .collect(Collectors.toList());

            // Paginate manually
            int start = page * size;
            int end = Math.min(start + size, orders.size());
            List<Order> pageOrders = start < orders.size() ?
                    orders.subList(start, end) : new ArrayList<>();

            // Convert to response format
            List<Map<String, Object>> orderMaps = pageOrders.stream()
                    .map(this::orderToMap)
                    .collect(Collectors.toList());

            // Calculate statistics
            Map<String, Object> delaiStats = calculateDelaiStatistics(orders);
            Map<String, Object> statusStats = calculateStatusStatistics(orders);

            // Build pagination info
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", page);
            pagination.put("size", size);
            pagination.put("totalElements", orders.size());
            pagination.put("totalPages", (int) Math.ceil((double) orders.size() / size));
            pagination.put("pageCardTotal", pageOrders.stream()
                    .mapToInt(Order::getCardCount)
                    .sum());
            pagination.put("totalCards", orders.stream()
                    .mapToInt(Order::getCardCount)
                    .sum());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orderMaps);
            response.put("pagination", pagination);
            response.put("delaiDistribution", delaiStats);
            response.put("statusStats", statusStats);

            log.info("✅ Returned {} orders (page {}/{})",
                    orderMaps.size(), page + 1, pagination.get("totalPages"));

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
     * GET /api/orders/high-priority
     * Get high priority orders (X, F+, F)
     */
    @GetMapping("/high-priority")
    public ResponseEntity<Map<String, Object>> getHighPriorityOrders() {
        try {
            log.info("🔴 GET /api/orders/high-priority");

            List<Order> orders = orderRepository.findHighPriorityOrders();

            List<Map<String, Object>> orderMaps = orders.stream()
                    .map(this::orderToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orderMaps);
            response.put("count", orders.size());
            response.put("totalCards", orders.stream()
                    .mapToInt(Order::getCardCount)
                    .sum());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading high priority orders", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
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

            Optional<Order> orderOpt = orderRepository.findByOrderNumber(id);

            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Order not found"
                ));
            }

            Order order = orderOpt.get();
            Map<String, Object> response = orderToMap(order);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading order {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private Map<String, Object> orderToMap(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId().toString());
        map.put("orderNumber", order.getOrderNumber());
        map.put("customerName", order.getCustomerName());
        map.put("delai", order.getDelai());
        map.put("status", order.getStatus().name());
        map.put("date", order.getDate() != null ? order.getDate().toString() : null);
        map.put("totalCards", order.getTotalCards());
        map.put("cardCount", order.getCardCount());
        map.put("price", order.getPrice());
        map.put("totalPrice", order.getPrice());
        map.put("priorityScore", order.getPriorityScore());
        map.put("priorityLabel", order.getPriorityLabel());
        map.put("estimatedProcessingMinutes", order.getEstimatedProcessingMinutes());
        map.put("estimatedDeliveryDate", order.getEstimatedDeliveryDate().toString());
        map.put("isOverdue", order.isLikelyOverdue());
        map.put("reference", order.getSymfonyOrderId());
        return map;
    }

    private Map<String, Object> calculateDelaiStatistics(List<Order> orders) {
        Map<String, Long> counts = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDelai() != null ? o.getDelai() : "UNKNOWN",
                        Collectors.counting()
                ));

        Map<String, Integer> cardCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getDelai() != null ? o.getDelai() : "UNKNOWN",
                        Collectors.summingInt(Order::getCardCount)
                ));

        Map<String, Object> stats = new HashMap<>();
        for (String delai : counts.keySet()) {
            Map<String, Object> delaiStat = new HashMap<>();
            delaiStat.put("count", counts.get(delai));
            delaiStat.put("cards", cardCounts.getOrDefault(delai, 0));
            stats.put(delai, delaiStat);
        }

        return stats;
    }

    private Map<String, Object> calculateStatusStatistics(List<Order> orders) {
        Map<String, Long> counts = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Integer> cardCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().name(),
                        Collectors.summingInt(Order::getCardCount)
                ));

        Map<String, Object> stats = new HashMap<>();
        for (String status : counts.keySet()) {
            Map<String, Object> statusStat = new HashMap<>();
            statusStat.put("count", counts.get(status));
            statusStat.put("cards", cardCounts.getOrDefault(status, 0));
            stats.put(status, statusStat);
        }

        return stats;
    }
}