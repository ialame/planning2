package com.pcagrade.order.service;

import com.pcagrade.order.entity.*;
import com.pcagrade.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    private static final int MINUTES_PER_CARD = 3;

    /**
     * Generate work plan for all orders based on their OrderStatus ENUM
     */
    @Transactional
    public List<WorkAssignment> generateWorkPlan() {
        log.info("🚀 Starting work plan generation...");

        // Clear previous assignments
        workAssignmentRepository.deleteAll();
        log.info("🗑️ Cleared previous assignments");

        List<WorkAssignment> allAssignments = new ArrayList<>();

        // ✅ DEBUG: Check what orders exist in database
        List<Order> allOrders = orderRepository.findAll();
        log.info("📦 Total orders in database: {}", allOrders.size());

        // Count orders by status ENUM
        Map<OrderStatus, Long> statusCounts = allOrders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        log.info("📊 Orders by status (ENUM):");
        statusCounts.forEach((status, count) ->
                log.info("   {}: {} orders", status, count)
        );

        // Check employees
        log.info("👥 Checking available employees:");
        List<Employee> allEmployees = employeeRepository.findByActiveTrue();
        log.info("   Total active employees: {}", allEmployees.size());

        Map<String, Long> roleCount = allEmployees.stream()
                .flatMap(e -> e.getRoles().stream())
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        roleCount.forEach((role, count) ->
                log.info("   {}: {} employees", role, count)
        );

        // ✅ FIXED: Process orders by OrderStatus ENUM (not integer)
        allAssignments.addAll(processStage(OrderStatus.GRADING, "GRADING", "ROLE_GRADER"));
        allAssignments.addAll(processStage(OrderStatus.CERTIFYING, "CERTIFYING", "ROLE_CERTIFIER"));
        allAssignments.addAll(processStage(OrderStatus.PACKAGING, "PACKAGING", "ROLE_PACKAGER"));
        allAssignments.addAll(processStage(OrderStatus.SCANNING, "SCANNING", "ROLE_SCANNER"));

        // Save all assignments
        List<WorkAssignment> savedAssignments = workAssignmentRepository.saveAll(allAssignments);

        log.info("✅ Work plan generated: {} total assignments", savedAssignments.size());

        // Summary by stage
        Map<String, Long> stageCounts = savedAssignments.stream()
                .collect(Collectors.groupingBy(WorkAssignment::getProcessingStage, Collectors.counting()));

        log.info("📊 Assignments by stage:");
        stageCounts.forEach((stage, count) ->
                log.info("   {}: {} tasks", stage, count)
        );

        return savedAssignments;
    }

    /**
     * Process a single stage using OrderStatus ENUM
     */
    private List<WorkAssignment> processStage(OrderStatus orderStatus, String stage, String requiredRole) {
        log.info("🔄 Processing {} stage (status = {}, role = {})", stage, orderStatus, requiredRole);

        // ✅ Find orders with this OrderStatus ENUM
        List<Order> orders = orderRepository.findByStatus(orderStatus);
        log.info("   Found {} orders with status {}", orders.size(), orderStatus);

        if (orders.isEmpty()) {
            log.info("   ⏭️ No orders to process for {}", stage);
            return new ArrayList<>();
        }

        // Find employees with required role
        List<Employee> employees = employeeRepository.findByActiveAndRolesContaining(true, requiredRole);
        log.info("   Found {} employees with role {}", employees.size(), requiredRole);

        if (employees.isEmpty()) {
            log.warn("   ⚠️ No employees available for {} stage!", stage);
            return new ArrayList<>();
        }

        // Sort orders by priority (delai code: X > F+ > F > C > E)
        // Handle null dates safely
        orders.sort(Comparator.comparingInt(Order::getPriorityScore).reversed()
                .thenComparing(o -> o.getDate() != null ? o.getDate() : LocalDate.MIN));

        // Log top priority orders
        log.info("   📋 Top 5 priority orders:");
        orders.stream().limit(5).forEach(o ->
                log.info("      - {} ({} cards, delai: {}, priority: {})",
                        o.getOrderNumber(), o.getCardCount(), o.getDelai(), o.getPriorityScore())
        );

        // Assign orders to employees using load balancing
        return assignOrdersToEmployees(orders, employees, stage);
    }

    /**
     * Assign orders to employees with load balancing
     */
    private List<WorkAssignment> assignOrdersToEmployees(
            List<Order> orders,
            List<Employee> employees,
            String stage) {

        List<WorkAssignment> assignments = new ArrayList<>();

        // Track each employee's current end time
        Map<UUID, LocalDateTime> employeeSchedule = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // Initialize all employees at current time
        for (Employee emp : employees) {
            employeeSchedule.put(emp.getId(), now);
        }

        int assignmentCount = 0;
        int skippedCount = 0;

        for (Order order : orders) {
            int cardCount = order.getCardCount();

            if (cardCount == 0) {
                log.debug("   ⚠️ Order {} has 0 cards, skipping", order.getOrderNumber());
                skippedCount++;
                continue;
            }

            // Find employee with earliest availability
            Employee selectedEmployee = employees.stream()
                    .min(Comparator.comparing(e -> employeeSchedule.get(e.getId())))
                    .orElse(employees.get(0));

            LocalDateTime startTime = employeeSchedule.get(selectedEmployee.getId());
            int durationMinutes = cardCount * MINUTES_PER_CARD;
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            // Create assignment
            WorkAssignment assignment = new WorkAssignment();
            assignment.setOrder(order);
            assignment.setEmployee(selectedEmployee);
            assignment.setProcessingStage(stage);
            assignment.setCardCount(cardCount);
            assignment.setEstimatedDurationMinutes(durationMinutes);
            assignment.setScheduledStart(startTime);
            assignment.setScheduledEnd(endTime);
            assignment.setStatus(AssignmentStatus.SCHEDULED);
            assignment.setPriorityScore((long) order.getPriorityScore());

            assignments.add(assignment);

            // Update employee's next available time
            employeeSchedule.put(selectedEmployee.getId(), endTime);

            assignmentCount++;

            // Log every 100th assignment
            if (assignmentCount % 100 == 0) {
                log.debug("   ... {} assignments created", assignmentCount);
            }
        }

        if (skippedCount > 0) {
            log.warn("   ⚠️ Skipped {} orders with 0 cards", skippedCount);
        }

        log.info("   ✅ Created {} assignments for {} stage", assignmentCount, stage);

        // Log workload distribution
        Map<String, Integer> workloadByEmployee = new HashMap<>();
        for (WorkAssignment assignment : assignments) {
            String empName = assignment.getEmployee().getFirstName() + " " +
                    assignment.getEmployee().getLastName();
            workloadByEmployee.merge(empName, assignment.getEstimatedDurationMinutes(), Integer::sum);
        }

        log.info("   👥 Workload distribution:");
        workloadByEmployee.forEach((name, minutes) ->
                log.info("      - {}: {} minutes ({} hours)", name, minutes, minutes / 60)
        );

        return assignments;
    }

    /**
     * Get all assignments for an employee
     */
    public List<WorkAssignment> getEmployeeAssignments(UUID employeeId) {
        return workAssignmentRepository.findByEmployeeIdOrderByScheduledStartAsc(employeeId);
    }

    /**
     * Get all assignments for an order
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
                .orElseThrow(() -> new RuntimeException("Assignment not found: " + assignmentId));

        assignment.setStatus(newStatus);

        if (newStatus == AssignmentStatus.IN_PROGRESS && assignment.getActualStart() == null) {
            assignment.setActualStart(LocalDateTime.now());
        } else if (newStatus == AssignmentStatus.COMPLETED && assignment.getActualEnd() == null) {
            assignment.setActualEnd(LocalDateTime.now());
        }

        return workAssignmentRepository.save(assignment);
    }
}