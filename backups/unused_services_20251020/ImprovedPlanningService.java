package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Improved Planning Service - Role-Based Assignment with Scanner Support
 *
 * Features:
 * - Assigns orders based on status and employee roles
 * - A_NOTER (status=2) → ROLE_NOTEUR (3 min/card)
 * - A_CERTIFIER (status=3) → ROLE_CERTIFICATEUR (3 min/card)
 * - A_PREPARER (status=4) → ROLE_PREPARATEUR (3 min/card)
 * - A_SCANNER (status=10) → ROLE_SCANNER (5 min/order - FIXED TIME)
 * - No start date filter - processes ALL pending orders
 * - Respects employee workload and working hours
 */
@Service
@Transactional
public class ImprovedPlanningService {

    private static final Logger log = LoggerFactory.getLogger(ImprovedPlanningService.class);

    @Autowired
    private EntityManager entityManager;

    // Constants
    private static final int TIME_PER_CARD_MINUTES = 3;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    // Configurable parameters
    @Value("${planning.task.break.minutes:5}")
    private int taskBreakMinutes;

    @Value("${planning.order.scanning.time:5}")
    private int scanningTimePerOrder;

    // Order Status Constants
    private static final String STATUS_GRADING = "GRADING";
    private static final String STATUS_CERTIFYING = "CERTIFYING";
    private static final String STATUS_PACKAGING = "PACKAGING";
    private static final String STATUS_SCANNING = "SCANNING";

    /**
     * Clean existing planning for a specific date
     */
    private void cleanExistingPlanning(LocalDate planningDate) {
        try {
            String deleteSql = "DELETE FROM planning WHERE planning_date = ?";
            Query query = entityManager.createNativeQuery(deleteSql);
            query.setParameter(1, planningDate);
            int deleted = query.executeUpdate();
            log.info("🧹 Cleaned {} existing planning entries for date: {}", deleted, planningDate);
        } catch (Exception e) {
            log.error("❌ Error cleaning planning: {}", e.getMessage());
        }
    }



    /**
     * Get employees by role (using employee_group and team)
     */
    private List<Map<String, Object>> getEmployeesByRole(String roleName) {
        try {
            String sql = """
                SELECT DISTINCT
                    HEX(e.id) as id,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.work_hours_per_day as workHoursPerDay,
                    g.name as role
                FROM employee e
                INNER JOIN employee_group eg ON e.id = eg.employee_id
                INNER JOIN team g ON eg.group_id = g.id
                WHERE g.name = ?
                  AND e.active = 1
                  AND g.active = 1
                ORDER BY e.first_name, e.last_name
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, roleName);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", (String) row[0]);
                employee.put("firstName", (String) row[1]);
                employee.put("lastName", (String) row[2]);
                employee.put("workHoursPerDay", ((Number) row[3]).intValue());
                employee.put("role", (String) row[4]);
                employees.add(employee);
            }

            return employees;

        } catch (Exception e) {
            log.error("❌ Error fetching employees by role: {}", e.getMessage());
            return new ArrayList<>();
        }
    }


    /**
     * Assign scanning orders to employees
     * Scanning takes FIXED time per order (not per card)
     */
    private int assignScanningOrdersToEmployees(
            List<Map<String, Object>> orders,
            Map<String, EmployeeWorkload> workloads,
            LocalDate planningDate,
            String planningStatus) {  // ⬅️ Changé de int à String

        if (orders.isEmpty() || workloads.isEmpty()) {
            log.info("⏭️ Skipping scanning assignment: {} orders, {} employees",
                    orders.size(), workloads.size());
            return 0;
        }

        int plannedCount = 0;

        for (Map<String, Object> order : orders) {
            String orderId = (String) order.get("id");
            Integer cardCount = (Integer) order.get("cardCount");
            String delai = (String) order.getOrDefault("delai", "C");

            int durationMinutes = scanningTimePerOrder;

            EmployeeWorkload leastLoaded = workloads.values().stream()
                    .min(Comparator.comparingInt(EmployeeWorkload::getTotalMinutes))
                    .orElse(null);

            if (leastLoaded == null) {
                log.warn("⚠️ No scanner employee available for order {}", orderId);
                continue;
            }

            LocalDateTime startTime = calculateStartTime(leastLoaded, durationMinutes);
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            String planningId = UUID.randomUUID().toString().replace("-", "");

            boolean saved = savePlanning(
                    planningId,
                    orderId,
                    leastLoaded.getEmployeeId(),
                    startTime.toLocalDate(),
                    startTime,
                    endTime,
                    durationMinutes,
                    delai,
                    cardCount != null ? cardCount : 0,
                    planningStatus  // ⬅️ Maintenant un String
            );

            if (saved) {
                leastLoaded.addTask(durationMinutes, startTime, endTime);  // ⬅️ Utilise la signature correcte
                plannedCount++;
            }
        }

        log.info("✅ Assigned {} scanning orders to {} scanner employees",
                plannedCount, workloads.size());
        return plannedCount;
    }



    /**
     * Calculate start time for a task considering employee's current workload
     */
    private LocalDateTime calculateStartTime(EmployeeWorkload workload, int durationMinutes) {
        if (workload.getLastEndTime() == null) {
            return workload.getCurrentDate().atTime(WORK_START_TIME);
        }

        LocalDateTime potentialStartTime = workload.getLastEndTime().plusMinutes(taskBreakMinutes);
        LocalDateTime potentialEndTime = potentialStartTime.plusMinutes(durationMinutes);

        if (potentialStartTime.toLocalTime().isBefore(WORK_END_TIME) &&
                potentialEndTime.toLocalTime().isBefore(WORK_END_TIME.plusMinutes(1))) {
            return potentialStartTime;
        }

        LocalDate nextDay = potentialStartTime.toLocalDate().plusDays(1);
        return nextDay.atTime(WORK_START_TIME);
    }

    /**
     * Save planning to database
     */
    private boolean savePlanning(
            String planningId,
            String orderId,
            String employeeId,
            LocalDate planningDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationMinutes,
            String delai,
            int cardCount,
            String status) {  // ⬅️ Changé de int à String

        try {
            String sql = """
            INSERT INTO planning 
            (id, order_id, employee_id, planning_date, start_time, end_time,
             estimated_duration_minutes, status, 
             completed, card_count, delai, created_at, updated_at)
            VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?,
                    ?, ?, 0, ?, ?, NOW(), NOW())
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planningId);
            query.setParameter(2, orderId.replace("-", ""));
            query.setParameter(3, employeeId.replace("-", ""));
            query.setParameter(4, planningDate);
            query.setParameter(5, startTime);
            query.setParameter(6, endTime);
            query.setParameter(7, durationMinutes);
            query.setParameter(8, status);  // ⬅️ Maintenant un String
            query.setParameter(9, cardCount);
            query.setParameter(10, delai);

            int result = query.executeUpdate();

            if (result > 0) {
                log.debug("✅ Planning saved: order={}, employee={}, status={}",
                        orderId, employeeId, status);
            }

            return result > 0;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage(), e);
            return false;
        }
    }


    /**
     * Build workload summary for response
     */
    private List<Map<String, Object>> buildWorkloadSummary(Map<String, EmployeeWorkload> workloads) {
        return workloads.values().stream().map(w -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("employeeId", w.getEmployeeId());
            summary.put("employeeName", w.getEmployeeName());
            summary.put("totalMinutes", w.getTotalMinutes());
            summary.put("totalHours", String.format("%.1f", w.getTotalMinutes() / 60.0).replace('.', ','));
            summary.put("workloadPercentage", w.getWorkloadPercentage());
            summary.put("status", w.getWorkloadPercentage() > 100 ? "FULL" : "AVAILABLE");
            return summary;
        }).collect(Collectors.toList());
    }

    /**
     * Inner class to track employee workload
     */
    private class EmployeeWorkload {
        private final String employeeId;
        private final String employeeName;
        private final int workHoursPerDay;
        private int totalMinutes = 0;
        private LocalDateTime lastEndTime = null;
        private LocalDate currentDate;

        public EmployeeWorkload(Map<String, Object> employee) {
            this.employeeId = (String) employee.get("id");
            this.employeeName = employee.get("firstName") + " " + employee.get("lastName");
            this.workHoursPerDay = (Integer) employee.get("workHoursPerDay");
            this.currentDate = LocalDate.now();
        }

        public void addTask(int minutes, LocalDateTime startTime, LocalDateTime endTime) {
            this.totalMinutes += minutes;
            this.lastEndTime = endTime;
            if (startTime.toLocalDate().isAfter(this.currentDate)) {
                this.currentDate = startTime.toLocalDate();
            }
        }

        public String getEmployeeId() { return employeeId; }
        public String getEmployeeName() { return employeeName; }
        public int getTotalMinutes() { return totalMinutes; }
        public LocalDateTime getLastEndTime() { return lastEndTime; }
        public LocalDate getCurrentDate() { return currentDate; }

        public int getWorkloadPercentage() {
            int dailyCapacity = workHoursPerDay * 60;
            return dailyCapacity > 0 ? (totalMinutes * 100) / dailyCapacity : 0;
        }
    }


    /**
     * Get orders by OrderStatus ENUM (NOT integer!)
     * ✅ FIXED: Use OrderStatus enum values instead of integers
     */
    private List<Map<String, Object>> getOrdersByStatus(String statusEnum) {
        try {
            String sql = """
        SELECT 
            HEX(o.id) as id,
            o.order_number as orderNumber,
            COALESCE(o.total_cards, 0) as cardCount,
            o.delai,
            o.date as date,
            o.status as status
        FROM card_order o
        WHERE o.status = ?
        ORDER BY 
            CASE o.delai
                WHEN 'X' THEN 1
                WHEN 'F+' THEN 2
                WHEN 'F' THEN 3
                WHEN 'C' THEN 4
                WHEN 'E' THEN 5
                ELSE 6
            END ASC,
            o.date ASC
        """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, statusEnum);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", row[0]);
                order.put("orderNumber", row[1]);
                order.put("cardCount", ((Number) row[2]).intValue());
                order.put("delai", row[3]);
                order.put("date", row[4]);
                order.put("status", row[5]);
                orders.add(order);
            }

            log.info("✅ Found {} orders with status '{}'", orders.size(), statusEnum);
            return orders;

        } catch (Exception e) {
            log.error("❌ Error fetching orders by status '{}': {}", statusEnum, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Main planning execution - UPDATED to use OrderStatus ENUM
     */
    public Map<String, Object> executeRoleBasedPlanning(LocalDate planningDate, boolean cleanFirst) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🎯 Starting Role-Based Planning for date: {}", planningDate);

            // Step 1: Clean existing planning if requested
            if (cleanFirst) {
                cleanExistingPlanning(planningDate);
            }

            // Step 2: Get orders by OrderStatus ENUM (not integer!)
            log.info("📋 Fetching orders with status GRADING...");
            List<Map<String, Object>> ordersToGrade = getOrdersByStatus(STATUS_GRADING);

            log.info("📋 Fetching orders with status CERTIFYING...");
            List<Map<String, Object>> ordersToCertify = getOrdersByStatus(STATUS_CERTIFYING);

            log.info("📋 Fetching orders with status PACKAGING...");
            List<Map<String, Object>> ordersToPrepare = getOrdersByStatus(STATUS_PACKAGING);

            log.info("📋 Fetching orders with status SCANNING...");
            List<Map<String, Object>> ordersToScan = getOrdersByStatus(STATUS_SCANNING);


            log.info("📋 Found {} orders to grade, {} orders to certify, {} orders to prepare, {} orders to scan",
                    ordersToGrade.size(), ordersToCertify.size(), ordersToPrepare.size(), ordersToScan.size());

            // Step 3: Get employees by role
            log.info("👥 Fetching ROLE_NOTEUR employees...");
            List<Map<String, Object>> graders = getEmployeesByRole("ROLE_NOTEUR");

            log.info("👥 Fetching ROLE_CERTIFICATEUR employees...");
            List<Map<String, Object>> certifiers = getEmployeesByRole("ROLE_CERTIFICATEUR");

            log.info("👥 Fetching ROLE_PREPARATEUR employees...");
            List<Map<String, Object>> preparers = getEmployeesByRole("ROLE_PREPARATEUR");

            log.info("👥 Fetching ROLE_SCANNEUR employees...");
            List<Map<String, Object>> scanners = getEmployeesByRole("ROLE_SCANNEUR");

            log.info("👥 Found {} graders, {} certifiers, {} preparers, {} scanners",
                    graders.size(), certifiers.size(), preparers.size(), scanners.size());

            // Validate we have employees
            if (graders.isEmpty() && !ordersToGrade.isEmpty()) {
                result.put("success", false);
                result.put("message", "No NOTEUR employees found but there are orders to grade");
                return result;
            }

            if (certifiers.isEmpty() && !ordersToCertify.isEmpty()) {
                result.put("success", false);
                result.put("message", "No CERTIFICATEUR employees found but there are orders to certify");
                return result;
            }

            if (preparers.isEmpty() && !ordersToPrepare.isEmpty()) {
                result.put("success", false);
                result.put("message", "No PREPARATEUR employees found but there are orders to prepare");
                return result;
            }

            if (scanners.isEmpty() && !ordersToScan.isEmpty()) {
                result.put("success", false);
                result.put("message", "No SCANNEUR employees found but there are orders to scan");
                return result;
            }

            // Step 4: Track workloads
            Map<String, EmployeeWorkload> graderWorkloads = new HashMap<>();
            graders.forEach(e -> graderWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            Map<String, EmployeeWorkload> certifierWorkloads = new HashMap<>();
            certifiers.forEach(e -> certifierWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            Map<String, EmployeeWorkload> preparerWorkloads = new HashMap<>();
            preparers.forEach(e -> preparerWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            Map<String, EmployeeWorkload> scannerWorkloads = new HashMap<>();
            scanners.forEach(e -> scannerWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            // Step 5: Assign orders - pass the actual status string
            int plannedGrading = assignOrdersToEmployees(ordersToGrade, graderWorkloads, planningDate, STATUS_GRADING);
            int plannedCertification = assignOrdersToEmployees(ordersToCertify, certifierWorkloads, planningDate, STATUS_CERTIFYING);
            int plannedPreparation = assignOrdersToEmployees(ordersToPrepare, preparerWorkloads, planningDate, STATUS_PACKAGING);
            int plannedScanning = assignScanningOrdersToEmployees(ordersToScan, scannerWorkloads, planningDate, STATUS_SCANNING);

            // Step 6: Build result
            result.put("success", true);
            result.put("message", String.format("✅ Planning completed: %d grading + %d certification + %d preparation + %d scanning tasks",
                    plannedGrading, plannedCertification, plannedPreparation, plannedScanning));
            result.put("totalPlanned", plannedGrading + plannedCertification + plannedPreparation + plannedScanning);
            result.put("plannedGrading", plannedGrading);
            result.put("plannedCertification", plannedCertification);
            result.put("plannedPreparation", plannedPreparation);
            result.put("plannedScanning", plannedScanning);
            result.put("startDate", planningDate.toString());
            result.put("graderWorkloads", buildWorkloadSummary(graderWorkloads));
            result.put("certifierWorkloads", buildWorkloadSummary(certifierWorkloads));
            result.put("preparerWorkloads", buildWorkloadSummary(preparerWorkloads));
            result.put("scannerWorkloads", buildWorkloadSummary(scannerWorkloads));

            log.info("🎉 Planning completed successfully: {} total tasks scheduled across multiple days starting from {}",
                    plannedGrading + plannedCertification + plannedPreparation + plannedScanning, planningDate);
            return result;

        } catch (Exception e) {
            log.error("❌ Error executing role-based planning: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Assign orders to employees - UPDATED signature to accept String status
     */
    private int assignOrdersToEmployees(
            List<Map<String, Object>> orders,
            Map<String, EmployeeWorkload> workloads,
            LocalDate planningDate,
            String planningStatus) {  // ⬅️ Changé de int à String

        if (orders.isEmpty() || workloads.isEmpty()) {
            log.info("⏭️ Skipping assignment for status {}: {} orders, {} employees",
                    planningStatus, orders.size(), workloads.size());
            return 0;
        }

        int plannedCount = 0;

        for (Map<String, Object> order : orders) {
            String orderId = (String) order.get("id");
            Integer cardCount = (Integer) order.get("cardCount");
            String delai = (String) order.getOrDefault("delai", "C");

            if (cardCount == null || cardCount == 0) {
                log.warn("⚠️ Order {} has no cards, skipping", orderId);
                continue;
            }

            int durationMinutes = cardCount * TIME_PER_CARD_MINUTES;

            // Find least loaded employee
            EmployeeWorkload leastLoaded = workloads.values().stream()
                    .min(Comparator.comparingInt(EmployeeWorkload::getTotalMinutes))
                    .orElse(null);

            if (leastLoaded == null) {
                log.warn("⚠️ No employee available for order {}", orderId);
                continue;
            }

            LocalDateTime startTime = calculateStartTime(leastLoaded, durationMinutes);
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            String planningId = UUID.randomUUID().toString().replace("-", "");

            boolean saved = savePlanning(
                    planningId,
                    orderId,
                    leastLoaded.getEmployeeId(),
                    startTime.toLocalDate(),
                    startTime,
                    endTime,
                    durationMinutes,
                    delai,
                    cardCount,
                    planningStatus  // ⬅️ Maintenant un String
            );

            if (saved) {
                leastLoaded.addTask(durationMinutes, startTime, endTime);  // ⬅️ Utilise la signature correcte
                plannedCount++;
            }
        }

        log.info("✅ Assigned {} orders (status={}) to {} employees",
                plannedCount, planningStatus, workloads.size());
        return plannedCount;
    }

    /**
     * Save planning to database - UPDATED to handle String status
     */
    private boolean savePlanningToDatabase(
            String orderId,
            String employeeId,
            LocalDate planningDate,
            LocalDateTime startTime,
            int durationMinutes,
            int cardCount,
            String delai,
            String statusString) {  // Changed from int to String

        try {
            String planningId = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            String sql = """
            INSERT INTO planning (
                id, order_id, employee_id,
                planning_date, start_time, end_time,
                estimated_duration_minutes,
                card_count, delai, status,
                created_at, updated_at
            ) VALUES (
                UNHEX(?), UNHEX(?), UNHEX(?),
                ?, ?, ?,
                ?,
                ?, ?, ?,
                NOW(), NOW()
            )
        """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planningId);
            query.setParameter(2, orderId);
            query.setParameter(3, employeeId);
            query.setParameter(4, planningDate);
            query.setParameter(5, startTime);
            query.setParameter(6, endTime);
            query.setParameter(7, durationMinutes);
            query.setParameter(8, cardCount);
            query.setParameter(9, delai);
            query.setParameter(10, statusString);  // Use string status

            int result = query.executeUpdate();
            return result > 0;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage());
            return false;
        }
    }


}