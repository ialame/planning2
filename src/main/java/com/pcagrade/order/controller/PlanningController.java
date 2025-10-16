package com.pcagrade.order.controller;

import com.pcagrade.order.dto.*;
import com.pcagrade.order.entity.*;
import com.pcagrade.order.service.WorkPlanningService;
import com.pcagrade.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Planning Controller - REST API for work planning
 */
@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PlanningController {

    private final WorkPlanningService workPlanningService;
    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

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
}