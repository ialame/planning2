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


    // Add these methods to SimpleOrderController.java after the getOrder method

    /**
     * GET /api/orders/{id}/cards
     * Get all cards for a specific order from card_certification table
     */
    @GetMapping("/{id}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String id) {
        try {
            log.info("🃏 GET /api/orders/{}/cards", id);

            // Try to find order by order number first
            Optional<Order> orderOpt = orderRepository.findByOrderNumber(id);

            // If not found, try by UUID
            if (orderOpt.isEmpty()) {
                try {
                    UUID uuid = UUID.fromString(id);
                    orderOpt = orderRepository.findById(uuid);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order identifier: {}", id);
                }
            }

            if (orderOpt.isEmpty()) {
                log.warn("Order not found: {}", id);
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "error", "Order not found"
                ));
            }

            Order order = orderOpt.get();

            // ✅ Get cards from card_certification table
            List<CardCertification> certifications = cardCertificationRepository.findByOrderId(order.getId());

            log.info("✅ Found {} cards in card_certification for order {}", certifications.size(), order.getOrderNumber());

            // Convert certifications to response format
            List<Map<String, Object>> cardMaps = new ArrayList<>();

            if (certifications.isEmpty() && order.getTotalCards() != null && order.getTotalCards() > 0) {
                // Fallback: generate virtual cards
                log.info("   No certifications found, generating {} virtual cards", order.getTotalCards());

                for (int i = 1; i <= order.getTotalCards(); i++) {
                    Map<String, Object> virtualCard = new HashMap<>();
                    virtualCard.put("id", UUID.randomUUID().toString());
                    virtualCard.put("cardName", "Card " + i);
                    virtualCard.put("name", "Card " + i);
                    virtualCard.put("cardNumber", String.valueOf(i));
                    virtualCard.put("labelName", order.getOrderNumber() + "-" + i);
                    virtualCard.put("label_name", order.getOrderNumber() + "-" + i);
                    virtualCard.put("code_barre", order.getOrderNumber() + "-" + i);
                    virtualCard.put("barcode", order.getOrderNumber() + "-" + i);
                    virtualCard.put("status", order.getStatus().name());
                    virtualCard.put("grade", "Not graded");
                    virtualCard.put("duration", 3);
                    virtualCard.put("quantity", 1);
                    virtualCard.put("amount", 1);
                    cardMaps.add(virtualCard);
                }
            } else {
                // Use actual certifications
                cardMaps = certifications.stream()
                        .map(this::certificationToMap)
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cards", cardMaps);
            response.put("total", cardMaps.size());
            response.put("orderNumber", order.getOrderNumber());
            response.put("orderStatus", order.getStatus().name());
            response.put("source", certifications.isEmpty() ? "virtual" : "card_certification");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading cards for order {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Convert CardCertification to Map
     */
    private Map<String, Object> certificationToMap(CardCertification cert) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", cert.getId().toString());
        map.put("cardName", cert.getCardName() != null ? cert.getCardName() : "Unknown Card");
        map.put("name", cert.getCardName() != null ? cert.getCardName() : "Unknown Card");
        map.put("cardId", cert.getCardId() != null ? cert.getCardId() : "N/A");
        map.put("cardNumber", cert.getCardId() != null ? cert.getCardId().toString() : "N/A");
        map.put("labelName", cert.getSymfonyCertificationId());
        map.put("label_name", cert.getSymfonyCertificationId());
        map.put("code_barre", cert.getSymfonyCertificationId());
        map.put("barcode", cert.getSymfonyCertificationId());

        // Status based on completion flags
        String status;
        if (Boolean.TRUE.equals(cert.getPackagingCompleted())) {
            status = "COMPLETED";
        } else if (Boolean.TRUE.equals(cert.getScanningCompleted())) {
            status = "PACKAGING";
        } else if (Boolean.TRUE.equals(cert.getCertificationCompleted())) {
            status = "SCANNING";
        } else if (Boolean.TRUE.equals(cert.getGradingCompleted())) {
            status = "CERTIFYING";
        } else {
            status = "GRADING";
        }
        map.put("status", status);

        // Completion flags
        map.put("gradingCompleted", cert.getGradingCompleted());
        map.put("certificationCompleted", cert.getCertificationCompleted());
        map.put("scanningCompleted", cert.getScanningCompleted());
        map.put("packagingCompleted", cert.getPackagingCompleted());

        // Additional fields
        map.put("assignedGrade", cert.getGradingCompleted() ? "Graded" : null);
        map.put("grade", cert.getGradingCompleted() ? "Graded" : "Not graded");
        map.put("duration", 3);
        map.put("quantity", 1);
        map.put("amount", 1);

        return map;
    }
}