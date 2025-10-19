package com.pcagrade.order.controller;

import com.pcagrade.order.dto.*;
import com.pcagrade.order.entity.*;
import com.pcagrade.order.service.WorkPlanningService;
import com.pcagrade.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Planning Controller - REST API for work planning
 */
@RestController
@RequestMapping("/api/planning")
@Slf4j
@CrossOrigin(origins = "*")
public class PlanningController {

    private final WorkPlanningService workPlanningService;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final CardCertificationRepository cardCertificationRepository;


    // Update the constructor to inject it:
    public PlanningController(
            WorkPlanningService workPlanningService,
            OrderRepository orderRepository,
            EmployeeRepository employeeRepository,
            WorkAssignmentRepository workAssignmentRepository,
            CardCertificationRepository cardCertificationRepository) {

        this.workPlanningService = workPlanningService;
        this.orderRepository = orderRepository;
        this.employeeRepository = employeeRepository;
        this.workAssignmentRepository = workAssignmentRepository;
        this.cardCertificationRepository = cardCertificationRepository;
    }


    /**
     * Generate work plan for all pending orders
     * POST /api/planning/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<PlanningResponse> generateWorkPlan() {
        log.info("Received request to generate work plan");

        try {
            List<WorkAssignment> assignments = workPlanningService.generateWorkPlan();

            PlanningResponse response = new PlanningResponse();
            response.setSuccess(true);
            response.setMessage("Work plan generated successfully");
            response.setAssignmentsCreated(assignments.size());
            response.setAssignments(assignments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));

            // Generate summary
            WorkPlanSummaryDTO summary = generateSummary(assignments);
            response.setSummary(summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating work plan", e);
            PlanningResponse response = new PlanningResponse();
            response.setSuccess(false);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get all work assignments
     * GET /api/planning/assignments
     */
    @GetMapping("/assignments")
    public ResponseEntity<List<WorkAssignmentDTO>> getAllAssignments() {
        List<WorkAssignment> assignments = workAssignmentRepository.findAll();
        List<WorkAssignmentDTO> dtos = assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get assignments for specific employee
     * GET /api/planning/assignments/employee/{employeeId}
     */
    @GetMapping("/assignments/employee/{employeeId}")
    public ResponseEntity<List<WorkAssignmentDTO>> getEmployeeAssignments(
            @PathVariable UUID employeeId) {
        List<WorkAssignment> assignments = workPlanningService.getEmployeeAssignments(employeeId);
        List<WorkAssignmentDTO> dtos = assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get assignments for specific order
     * GET /api/planning/assignments/order/{orderId}
     */
    @GetMapping("/assignments/order/{orderId}")
    public ResponseEntity<List<WorkAssignmentDTO>> getOrderAssignments(
            @PathVariable UUID orderId) {
        List<WorkAssignment> assignments = workPlanningService.getOrderAssignments(orderId);
        List<WorkAssignmentDTO> dtos = assignments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update assignment status
     * PUT /api/planning/assignments/{assignmentId}/status
     */
    @PutMapping("/assignments/{assignmentId}/status")
    public ResponseEntity<WorkAssignmentDTO> updateAssignmentStatus(
            @PathVariable UUID assignmentId,
            @RequestParam String status) {
        try {
            AssignmentStatus newStatus = AssignmentStatus.valueOf(status.toUpperCase());
            WorkAssignment updated = workPlanningService.updateAssignmentStatus(assignmentId, newStatus);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get employee workload summary
     * GET /api/planning/workload
     */
    @GetMapping("/workload")
    public ResponseEntity<List<EmployeeWorkloadDTO>> getEmployeeWorkload() {
        List<Employee> employees = employeeRepository.findByActiveTrue();
        List<EmployeeWorkloadDTO> workloads = employees.stream()
                .map(this::calculateEmployeeWorkload)
                .sorted(Comparator.comparing(EmployeeWorkloadDTO::getUtilizationPercentage).reversed())
                .collect(Collectors.toList());
        return ResponseEntity.ok(workloads);
    }

    /**
     * Get pending orders sorted by priority
     * GET /api/planning/orders/pending
     */
    @GetMapping("/orders/pending")
    public ResponseEntity<List<OrderDTO>> getPendingOrders() {
        List<Order> orders = orderRepository.findByStatusOrderedByDeadline(OrderStatus.PENDING);
        List<OrderDTO> dtos = orders.stream()
                .map(this::convertOrderToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get overdue orders
     * GET /api/planning/orders/overdue
     */
    @GetMapping("/orders/overdue")
    public ResponseEntity<List<OrderDTO>> getOverdueOrders() {
        List<Order> orders = orderRepository.findPotentiallyOverdueOrders();
        List<OrderDTO> dtos = orders.stream()
                .map(this::convertOrderToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ========== Helper Methods ==========

    private WorkAssignmentDTO convertToDTO(WorkAssignment assignment) {
        WorkAssignmentDTO dto = new WorkAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setEmployeeId(assignment.getEmployee().getId());
        dto.setEmployeeName(assignment.getEmployee().getFirstName() + " " +
                assignment.getEmployee().getLastName());
        dto.setOrderId(assignment.getOrder().getId());
        dto.setOrderNumber(assignment.getOrder().getOrderNumber());
        dto.setProcessingStage(assignment.getProcessingStage());
        dto.setCardCount(assignment.getCardCount());
        dto.setEstimatedDurationMinutes(assignment.getEstimatedDurationMinutes());
        dto.setScheduledStart(assignment.getScheduledStart());
        dto.setScheduledEnd(assignment.getScheduledEnd());
        dto.setActualStart(assignment.getActualStart());
        dto.setActualEnd(assignment.getActualEnd());
        dto.setStatus(assignment.getStatus().name());
        dto.setPriorityScore(assignment.getPriorityScore());
        return dto;
    }

    private OrderDTO convertOrderToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerName(order.getCustomerName());
        dto.setStatus(order.getStatus().name());
        dto.setCardCount(order.getCards().size());
        dto.setEstimatedProcessingMinutes(order.getEstimatedProcessingMinutes());
        dto.setPriorityScore((long) order.getPriorityScore());
        return dto;
    }

    private EmployeeWorkloadDTO calculateEmployeeWorkload(Employee employee) {
        Long assignmentCount = workAssignmentRepository
                .countActiveAssignmentsByEmployee(employee.getId());

        List<WorkAssignment> assignments = workAssignmentRepository
                .findByEmployeeIdAndStatus(employee.getId(), AssignmentStatus.SCHEDULED);

        int scheduledMinutes = assignments.stream()
                .mapToInt(WorkAssignment::getEstimatedDurationMinutes)
                .sum();

        double utilization = (double) scheduledMinutes / employee.getDailyCapacityMinutes() * 100;

        EmployeeWorkloadDTO dto = new EmployeeWorkloadDTO();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        dto.setRoles(employee.getRoles());
        dto.setScheduledMinutes(scheduledMinutes);
        dto.setDailyCapacityMinutes(employee.getDailyCapacityMinutes());
        dto.setUtilizationPercentage(utilization);
        dto.setAssignmentCount(assignmentCount.intValue());

        // Calculate next available time
        if (!assignments.isEmpty()) {
            dto.setNextAvailableTime(assignments.get(assignments.size() - 1).getScheduledEnd());
        }

        return dto;
    }

    private WorkPlanSummaryDTO generateSummary(List<WorkAssignment> assignments) {
        WorkPlanSummaryDTO summary = new WorkPlanSummaryDTO();

        Set<UUID> orderIds = assignments.stream()
                .map(a -> a.getOrder().getId())
                .collect(Collectors.toSet());

        Set<UUID> employeeIds = assignments.stream()
                .map(a -> a.getEmployee().getId())
                .collect(Collectors.toSet());

        summary.setTotalOrders(orderIds.size());
        summary.setTotalAssignments(assignments.size());
        summary.setEmployeesInvolved(employeeIds.size());

        summary.setTotalEstimatedMinutes(assignments.stream()
                .mapToInt(WorkAssignment::getEstimatedDurationMinutes)
                .sum());

        if (!assignments.isEmpty()) {
            summary.setEarliestDeadline(assignments.stream()
                    .map(WorkAssignment::getScheduledStart)
                    .min(Comparator.naturalOrder())
                    .orElse(null));

            summary.setLatestDeadline(assignments.stream()
                    .map(WorkAssignment::getScheduledEnd)
                    .max(Comparator.naturalOrder())
                    .orElse(null));
        }

        // Generate stage statistics
        Map<String, List<WorkAssignment>> byStage = assignments.stream()
                .collect(Collectors.groupingBy(WorkAssignment::getProcessingStage));

        List<StageStatistics> stageStats = byStage.entrySet().stream()
                .map(entry -> {
                    List<WorkAssignment> stageAssignments = entry.getValue();
                    return new StageStatistics(
                            entry.getKey(),
                            stageAssignments.size(),
                            stageAssignments.stream().mapToInt(WorkAssignment::getCardCount).sum(),
                            stageAssignments.stream().mapToInt(WorkAssignment::getEstimatedDurationMinutes).sum(),
                            (int) stageAssignments.stream().map(a -> a.getEmployee().getId()).distinct().count()
                    );
                })
                .collect(Collectors.toList());

        summary.setStageStatistics(stageStats);

        return summary;
    }

    // Add these methods to PlanningController.java after the existing endpoints

    /**
     * Get assignments for specific employee (alternative endpoint)
     * GET /api/planning/employee/{employeeId}
     * Can optionally filter by date
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeePlanningAssignments(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        log.info("📋 Getting assignments for employee: {} on date: {}", employeeId, date);

        try {
            // Parse employee ID (handle both with and without dashes)
            UUID uuid = parseUUID(employeeId);

            // Get all assignments for this employee
            List<WorkAssignment> assignments = workAssignmentRepository
                    .findByEmployeeIdOrderByScheduledStartAsc(uuid);

            log.info("   Found {} total assignments for employee", assignments.size());

            // Filter by date if provided
            if (date != null && !date.isEmpty()) {
                LocalDate filterDate = LocalDate.parse(date);
                assignments = assignments.stream()
                        .filter(a -> a.getScheduledStart() != null &&
                                a.getScheduledStart().toLocalDate().equals(filterDate))
                        .collect(Collectors.toList());
                log.info("   Filtered to {} assignments for date: {}", assignments.size(), date);
            }

            if (assignments.isEmpty()) {
                log.info("   No assignments found");
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "assignments", List.of(),
                        "total", 0,
                        "message", "No assignments found for this employee" + (date != null ? " on " + date : "")
                ));
            }

            // Convert to detailed DTOs with order information
            List<Map<String, Object>> detailedAssignments = assignments.stream()
                    .map(this::convertToDetailedDTO)
                    .collect(Collectors.toList());

            // Calculate summary
            int totalCards = assignments.stream()
                    .mapToInt(WorkAssignment::getCardCount)
                    .sum();

            int totalMinutes = assignments.stream()
                    .mapToInt(WorkAssignment::getEstimatedDurationMinutes)
                    .sum();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("assignments", detailedAssignments);
            response.put("total", assignments.size());
            response.put("summary", Map.of(
                    "totalCards", totalCards,
                    "totalMinutes", totalMinutes,
                    "totalHours", Math.round(totalMinutes / 60.0 * 100.0) / 100.0
            ));

            log.info("✅ Returning {} assignments", assignments.size());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid employee ID: {}", employeeId, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid employee ID format"
            ));
        } catch (Exception e) {
            log.error("❌ Error getting employee assignments", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to parse UUID from string (with or without dashes)
     */
    private UUID parseUUID(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be null or empty");
        }

        // Remove dashes if present
        String cleanId = id.replace("-", "");

        // Add dashes in UUID format if missing
        if (cleanId.length() == 32) {
            cleanId = cleanId.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            );
        }

        return UUID.fromString(cleanId);
    }

    /**
     * Convert WorkAssignment to detailed DTO with order information
     */
    private Map<String, Object> convertToDetailedDTO(WorkAssignment assignment) {
        Map<String, Object> dto = new HashMap<>();

        // Assignment info
        dto.put("id", assignment.getId().toString());
        dto.put("processingStage", assignment.getProcessingStage());
        dto.put("cardCount", assignment.getCardCount());
        dto.put("estimatedDurationMinutes", assignment.getEstimatedDurationMinutes());
        dto.put("estimatedHours", Math.round(assignment.getEstimatedDurationMinutes() / 60.0 * 100.0) / 100.0);
        dto.put("scheduledStart", assignment.getScheduledStart());
        dto.put("scheduledEnd", assignment.getScheduledEnd());
        dto.put("actualStart", assignment.getActualStart());
        dto.put("actualEnd", assignment.getActualEnd());
        dto.put("status", assignment.getStatus().name());
        dto.put("priorityScore", assignment.getPriorityScore());

        // Order info
        Order order = assignment.getOrder();
        if (order != null) {
            dto.put("orderId", order.getId().toString());
            dto.put("orderNumber", order.getOrderNumber());
            dto.put("customerName", order.getCustomerName());
            dto.put("delai", order.getDelai());
            dto.put("orderStatus", order.getStatus().name());
            dto.put("orderDate", order.getDate());
            dto.put("priorityLabel", order.getPriorityLabel());
        }

        // Employee info
        Employee employee = assignment.getEmployee();
        if (employee != null) {
            dto.put("employeeId", employee.getId().toString());
            dto.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
        }

        // Date info for frontend
        if (assignment.getScheduledStart() != null) {
            dto.put("planningDate", assignment.getScheduledStart().toLocalDate().toString());
            dto.put("startTime", assignment.getScheduledStart().toLocalTime().toString());
        }
        if (assignment.getScheduledEnd() != null) {
            dto.put("endTime", assignment.getScheduledEnd().toLocalTime().toString());
        }

        return dto;
    }

    /**
     * Get cards for a specific order from card_certification table
     * GET /api/planning/order/{orderId}/cards
     */
    @GetMapping("/order/{orderId}/cards")
    public ResponseEntity<?> getOrderCards(@PathVariable String orderId) {
        log.info("📋 Getting cards for order: {}", orderId);

        try {
            // Try to find order by UUID first
            UUID orderUuid;
            try {
                orderUuid = parseUUID(orderId);
            } catch (IllegalArgumentException e) {
                // If not a UUID, try to find by order number
                log.info("   Not a UUID, searching by order number: {}", orderId);
                Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderId);
                if (orderOpt.isEmpty()) {
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Order not found: " + orderId
                    ));
                }
                orderUuid = orderOpt.get().getId();
            }

            // Get the order
            Optional<Order> orderOpt = orderRepository.findById(orderUuid);
            if (orderOpt.isEmpty()) {
                log.warn("   Order not found: {}", orderId);
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Order not found"
                ));
            }

            Order order = orderOpt.get();

            // ✅ Get cards from card_certification table
            List<CardCertification> certifications = cardCertificationRepository.findByOrderId(orderUuid);

            log.info("   Found {} cards in card_certification for order {}", certifications.size(), order.getOrderNumber());

            // Convert certifications to card DTOs
            List<Map<String, Object>> cardDtos = new ArrayList<>();

            if (certifications.isEmpty() && order.getTotalCards() != null && order.getTotalCards() > 0) {
                // Fallback: generate virtual cards if no certifications found
                log.info("   No certifications found, generating {} virtual cards based on total_cards", order.getTotalCards());

                for (int i = 1; i <= order.getTotalCards(); i++) {
                    Map<String, Object> virtualCard = new HashMap<>();
                    virtualCard.put("id", UUID.randomUUID().toString());
                    virtualCard.put("cardName", "Card " + i);
                    virtualCard.put("name", "Card " + i);
                    virtualCard.put("cardNumber", String.valueOf(i));
                    virtualCard.put("labelName", order.getOrderNumber() + "-" + i);
                    virtualCard.put("barcode", order.getOrderNumber() + "-" + i);
                    virtualCard.put("status", order.getStatus().name());
                    virtualCard.put("grade", "Not graded");
                    virtualCard.put("duration", 3);
                    virtualCard.put("quantity", 1);
                    virtualCard.put("gradingCompleted", false);
                    virtualCard.put("certificationCompleted", false);
                    virtualCard.put("scanningCompleted", false);
                    virtualCard.put("packagingCompleted", false);
                    cardDtos.add(virtualCard);
                }
            } else {
                // Use actual certifications
                for (CardCertification cert : certifications) {
                    cardDtos.add(convertCertificationToMap(cert));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cards", cardDtos,
                    "total", cardDtos.size(),
                    "orderNumber", order.getOrderNumber(),
                    "orderStatus", order.getStatus().name(),
                    "source", certifications.isEmpty() ? "virtual" : "card_certification"
            ));

        } catch (Exception e) {
            log.error("❌ Error getting cards for order: {}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }


    /**
     * Convert CardCertification to Map for display
     * UPDATED: Uses getId() instead of getSymfonyCertificationId()
     * since we now use Symfony ID as primary key
     */
    private Map<String, Object> convertCertificationToMap(CardCertification cert) {
        Map<String, Object> dto = new HashMap<>();

        // Use ID as string (it's the Symfony certification ID)
        String certificationId = cert.getId() != null ? cert.getId().toString() : "N/A";

        dto.put("id", certificationId);
        dto.put("cardName", cert.getCardName() != null ? cert.getCardName() : "Unknown Card");
        dto.put("name", cert.getCardName() != null ? cert.getCardName() : "Unknown Card");
        dto.put("cardId", cert.getCardId() != null ? cert.getCardId().toString() : "N/A");
        dto.put("cardNumber", cert.getCardId() != null ? cert.getCardId().toString() : "N/A");

        // Use code_barre for labelName and barcode
        // Fallback to certification ID if code_barre is empty
        String barcode = (cert.getCodeBarre() != null && !cert.getCodeBarre().isEmpty())
                ? cert.getCodeBarre()
                : certificationId;

        dto.put("labelName", barcode);
        dto.put("barcode", barcode);
        dto.put("code_barre", barcode);

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
        dto.put("status", status);

        // Completion flags
        dto.put("gradingCompleted", cert.getGradingCompleted());
        dto.put("certificationCompleted", cert.getCertificationCompleted());
        dto.put("scanningCompleted", cert.getScanningCompleted());
        dto.put("packagingCompleted", cert.getPackagingCompleted());

        // Additional fields
        dto.put("grade", cert.getGradingCompleted() ? "Graded" : "Not graded");
        dto.put("duration", 3);
        dto.put("quantity", 1);

        return dto;
    }

    /**
     * Convert Card entity to Map
     */
    private Map<String, Object> convertCardToMap(Card card) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", card.getId().toString());
        dto.put("cardName", card.getCardName());
        dto.put("name", card.getCardName());
        dto.put("cardSet", card.getCardSet());
        dto.put("cardNumber", card.getCardNumber());
        dto.put("labelName", card.getCardNumber());
        dto.put("code_barre", card.getCardNumber());
        dto.put("barcode", card.getCardNumber());
        dto.put("status", card.getStatus() != null ? card.getStatus().name() : "PENDING");
        dto.put("assignedGrade", card.getAssignedGrade());
        dto.put("grade", card.getAssignedGrade());
        dto.put("duration", 3);
        dto.put("quantity", 1);
        return dto;
    }


}