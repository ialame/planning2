package com.pcagrade.order.service;

import com.pcagrade.order.entity.*;
import com.pcagrade.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Work Planning Service
 * Distributes Pokemon card processing tasks among employees
 * based on order priority (delivery deadline) and employee roles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkPlanningService {

    private final OrderRepository orderRepository;
    private final EmployeeRepository employeeRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    // Processing time per card per stage (in minutes)
    private static final int MINUTES_PER_CARD = 3;

    // Processing stages and required roles
    private static final Map<String, String> STAGE_ROLE_MAP = Map.of(
            "GRADING", "ROLE_GRADER",
            "CERTIFYING", "ROLE_CERTIFIER",
            "SCANNING", "ROLE_SCANNER",
            "PACKAGING", "ROLE_PACKAGER"
    );

    /**
     * Generate work assignments for all pending orders
     * Priority is based on delivery deadline (earlier = higher priority)
     */
    @Transactional
    public List<WorkAssignment> generateWorkPlan() {
        log.info("Starting work plan generation...");

        // Clear previous assignments
        workAssignmentRepository.deleteAll();

        List<WorkAssignment> allAssignments = new ArrayList<>();

        // ✅ FIX: Process orders by their CURRENT status only

        // 1. GRADING: orders with status = 2 (A_NOTER)
        List<Order> ordersToGrade = orderRepository.findByStatusInteger(2);
        allAssignments.addAll(assignOrdersToStage(ordersToGrade, "GRADING", "ROLE_GRADER"));
        log.info("✅ Created {} GRADING assignments", ordersToGrade.size());

        // 2. CERTIFYING: orders with status = 3 (A_CERTIFIER)
        List<Order> ordersToCertify = orderRepository.findByStatusInteger(3);
        allAssignments.addAll(assignOrdersToStage(ordersToCertify, "CERTIFYING", "ROLE_CERTIFIER"));
        log.info("✅ Created {} CERTIFYING assignments", ordersToCertify.size());

        // 3. PACKAGING: orders with status = 4 (A_PREPARER)
        List<Order> ordersToPackage = orderRepository.findByStatusInteger(4);
        allAssignments.addAll(assignOrdersToStage(ordersToPackage, "PACKAGING", "ROLE_PACKAGER"));
        log.info("✅ Created {} PACKAGING assignments", ordersToPackage.size());

        // 4. SCANNING: orders with status = 10 (A_SCANNER)
        List<Order> ordersToScan = orderRepository.findByStatusInteger(10);
        allAssignments.addAll(assignOrdersToStage(ordersToScan, "SCANNING", "ROLE_SCANNER"));
        log.info("✅ Created {} SCANNING assignments", ordersToScan.size());

        // Save all assignments
        List<WorkAssignment> savedAssignments = workAssignmentRepository.saveAll(allAssignments);

        log.info("🎉 Work plan generated: {} total assignments created for {} orders",
                savedAssignments.size(),
                ordersToGrade.size() + ordersToCertify.size() + ordersToPackage.size() + ordersToScan.size());

        return savedAssignments;
    }

    // Add the helper method
    private List<WorkAssignment> assignOrdersToStage(List<Order> orders, String stage, String requiredRole) {
        List<WorkAssignment> assignments = new ArrayList<>();

        if (orders.isEmpty()) {
            log.info("No orders found for stage: {}", stage);
            return assignments;
        }

        // Get available employees for this role
        List<Employee> availableEmployees = employeeRepository
                .findByActiveAndRolesContaining(true, requiredRole);

        if (availableEmployees.isEmpty()) {
            log.warn("⚠️ No employees available for stage: {} with role: {}", stage, requiredRole);
            return assignments;
        }

        log.info("📋 Scheduling {} orders for {} stage with {} employees",
                orders.size(), stage, availableEmployees.size());

        // Create employee workload tracker
        Map<UUID, EmployeeWorkload> workloadMap = new HashMap<>();
        for (Employee emp : availableEmployees) {
            workloadMap.put(emp.getId(), new EmployeeWorkload(emp));
        }

        // Sort orders by priority
        List<Order> sortedOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getPriorityScore))
                .collect(Collectors.toList());

        // Distribute orders to employees
        for (Order order : sortedOrders) {
            Employee selectedEmployee = selectEmployeeWithMinWorkload(workloadMap);

            WorkAssignment assignment = createAssignment(
                    selectedEmployee,
                    order,
                    stage,
                    workloadMap.get(selectedEmployee.getId())
            );

            assignments.add(assignment);
            workloadMap.get(selectedEmployee.getId())
                    .addWorkload(assignment.getEstimatedDurationMinutes());
        }

        return assignments;
    }


    /**
     * Create a work assignment
     */
    private WorkAssignment createAssignment(Employee employee, Order order,
                                            String stage, EmployeeWorkload workload) {
        WorkAssignment assignment = new WorkAssignment();
        assignment.setEmployee(employee);
        assignment.setOrder(order);
        assignment.setProcessingStage(stage);
        assignment.setCardCount(order.getCards().size());
        assignment.setEstimatedDurationMinutes(order.getCards().size() * MINUTES_PER_CARD);
        assignment.setPriorityScore((long) order.getPriorityScore());

        // Schedule start time based on current workload
        LocalDateTime startTime = workload.getNextAvailableTime();
        assignment.setScheduledStart(startTime);
        assignment.setScheduledEnd(startTime.plusMinutes(assignment.getEstimatedDurationMinutes()));

        assignment.setStatus(AssignmentStatus.SCHEDULED);

        return assignment;
    }

    /**
     * Select employee with minimum workload
     */
    private Employee selectEmployeeWithMinWorkload(Map<UUID, EmployeeWorkload> workloadMap) {
        return workloadMap.values().stream()
                .min(Comparator.comparing(EmployeeWorkload::getTotalMinutes))
                .map(EmployeeWorkload::getEmployee)
                .orElseThrow(() -> new IllegalStateException("No employees available"));
    }

    /**
     * Get work assignments for specific employee
     */
    public List<WorkAssignment> getEmployeeAssignments(UUID employeeId) {
        return workAssignmentRepository.findByEmployeeIdOrderByScheduledStartAsc(employeeId);
    }

    /**
     * Get work assignments for specific order
     */
    public List<WorkAssignment> getOrderAssignments(UUID orderId) {
        return workAssignmentRepository.findByOrderIdOrderByScheduledStartAsc(orderId);
    }

    /**
     * Update assignment status
     */
    @Transactional
    public WorkAssignment updateAssignmentStatus(UUID assignmentId, AssignmentStatus newStatus) {
        WorkAssignment assignment = workAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        if (newStatus == AssignmentStatus.IN_PROGRESS) {
            assignment.start();
        } else if (newStatus == AssignmentStatus.COMPLETED) {
            assignment.complete();
        } else {
            assignment.setStatus(newStatus);
        }

        return workAssignmentRepository.save(assignment);
    }

    /**
     * Inner class to track employee workload
     */
    private static class EmployeeWorkload {
        private final Employee employee;
        private int totalMinutes;
        private LocalDateTime nextAvailableTime;

        public EmployeeWorkload(Employee employee) {
            this.employee = employee;
            this.totalMinutes = 0;
            // Start scheduling from current time
            this.nextAvailableTime = LocalDateTime.now();
        }

        public void addWorkload(int minutes) {
            this.totalMinutes += minutes;
            this.nextAvailableTime = this.nextAvailableTime.plusMinutes(minutes);
        }

        public Employee getEmployee() {
            return employee;
        }

        public int getTotalMinutes() {
            return totalMinutes;
        }

        public LocalDateTime getNextAvailableTime() {
            return nextAvailableTime;
        }
    }
}