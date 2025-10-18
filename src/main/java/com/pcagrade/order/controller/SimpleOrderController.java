package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Card;
import com.pcagrade.order.entity.CardCertification;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.repository.CardCertificationRepository;
import com.pcagrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final CardCertificationRepository cardCertificationRepository;

    public SimpleOrderController(
            OrderRepository orderRepository,
            CardCertificationRepository cardCertificationRepository) {
        this.orderRepository = orderRepository;
        this.cardCertificationRepository = cardCertificationRepository;
    }

    /**
     * GET /api/orders
     * Main endpoint for orders list with pagination and filters
     * UPDATED: Sort by date DESC only (removed priority sorting)
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

            List<Order> orders = orderRepository.findAll();

            // ✅ Apply ALL filters (not if-else, so multiple filters work together)
            if (delai != null && !delai.isEmpty() && !"all".equals(delai)) {
                log.info("   Filtering by delai: {}", delai);
                final String delaiFilter = delai;
                orders = orders.stream()
                        .filter(o -> delaiFilter.equals(o.getDelai()))
                        .collect(Collectors.toList());
            }

            if (status != null && !status.isEmpty() && !"all".equals(status)) {
                log.info("   Filtering by status: {}", status);
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    orders = orders.stream()
                            .filter(o -> o.getStatus() == orderStatus)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    log.warn("   Invalid status value: {}", status);
                }
            }

            if (search != null && !search.trim().isEmpty()) {
                log.info("   Filtering by search: {}", search);
                final String searchLower = search.trim().toLowerCase();
                orders = orders.stream()
                        .filter(o -> o.getCustomerName() != null &&
                                o.getCustomerName().toLowerCase().contains(searchLower))
                        .collect(Collectors.toList());
            }

            // ✅ Sort by date DESC BEFORE pagination
            orders = orders.stream()
                    .sorted((a, b) -> {
                        LocalDateTime dateA = a.getDate();
                        LocalDateTime dateB = b.getDate();
                        if (dateA == null && dateB == null) return 0;
                        if (dateA == null) return 1; // nulls last
                        if (dateB == null) return -1;
                        return dateB.compareTo(dateA); // DESC
                    })
                    .collect(Collectors.toList());

            log.info("📅 Sorted {} orders by date DESC", orders.size());

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
            response.put("delaiStatistics", delaiStats);
            response.put("statusStatistics", statusStats);

            log.info("✅ Returned {} orders (page {}/{})",
                    orderMaps.size(), page + 1, pagination.get("totalPages"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading orders", e);
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

        Map<String, Object> result = new HashMap<>();
        for (String delai : counts.keySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("count", counts.get(delai));
            stat.put("cards", cardCounts.getOrDefault(delai, 0));
            result.put(delai, stat);
        }

        return result;
    }

    private Map<String, Object> calculateStatusStatistics(List<Order> orders) {
        Map<OrderStatus, Long> counts = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::getStatus,
                        Collectors.counting()
                ));

        Map<OrderStatus, Integer> cardCounts = orders.stream()
                .collect(Collectors.groupingBy(
                        Order::getStatus,
                        Collectors.summingInt(Order::getCardCount)
                ));

        Map<String, Object> result = new HashMap<>();
        for (OrderStatus status : counts.keySet()) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("count", counts.get(status));
            stat.put("cards", cardCounts.getOrDefault(status, 0));
            result.put(status.name(), stat);
        }

        return result;
    }
}