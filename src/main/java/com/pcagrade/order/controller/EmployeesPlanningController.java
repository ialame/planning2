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
 * 👥 EMPLOYEES PLANNING CONTROLLER - Fixed version
 * Endpoints for frontend with unique prefixes
 * ✅ FIXED: Removed all p.priority references
 */
@RestController
@RequestMapping("/api/frontend/employees")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class EmployeesPlanningController {

    private static final Logger log = LoggerFactory.getLogger(EmployeesPlanningController.class);

    @Autowired
    private EntityManager entityManager;

    /**
     * 👥 GET ALL EMPLOYEES - Management View
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmployees() {
        try {
            log.info("👥 Fetching all employees for management view");

            String sql = """
                SELECT 
                    HEX(id) as id,
                    COALESCE(first_name, 'Unknown') as firstName,
                    COALESCE(last_name, 'User') as lastName,
                    COALESCE(email, 'no-email@example.com') as email,
                    COALESCE(active, 1) as active,
                    COALESCE(work_hours_per_day, 8) as workHoursPerDay,
                    creation_date as creationDate,
                    modification_date as modificationDate
                FROM j_employee
                ORDER BY first_name ASC, last_name ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", row[0]);
                employee.put("firstName", row[1]);
                employee.put("lastName", row[2]);
                employee.put("email", row[3]);
                employee.put("active", ((Number) row[4]).intValue() == 1);
                employee.put("workHoursPerDay", row[5]);
                employee.put("creationDate", row[6]);
                employee.put("modificationDate", row[7]);

                // Computed fields
                employee.put("fullName", row[1] + " " + row[2]);
                employee.put("status", ((Number) row[4]).intValue() == 1 ?
                        "ACTIVE" : "INACTIVE");

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());

            log.info("✅ Retrieved {} employees", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching employees", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 📋 GET EMPLOYEES WITH PLANNING DATA
     * ✅ FIXED: Removed all p.priority references
     */
    @GetMapping("/planning-data")
    public ResponseEntity<Map<String, Object>> getEmployeesWithPlanningData(
            @RequestParam(required = false) String date) {

        try {
            log.info("📋 Fetching employees with planning data for date: {}", date);

            String dateFilter = date != null ?
                    " AND p.planning_date = '" + date + "'" : "";

            // ✅ FIXED: Removed p.priority, using only necessary columns
            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email,
                    COALESCE(e.active, 1) as active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    COALESCE(SUM(p.estimated_duration_minutes), 0) as totalMinutes,
                    COUNT(p.id) as taskCount,
                    COALESCE(SUM(p.card_count), 0) as cardCount,
                    ROUND(
                        COALESCE(SUM(p.estimated_duration_minutes), 0) / 
                        (COALESCE(e.work_hours_per_day, 8) * 60.0), 2
                    ) as workloadRatio
                FROM j_employee e
                LEFT JOIN j_planning p ON e.id = p.employee_id""" + dateFilter + """
                GROUP BY e.id, e.first_name, e.last_name, e.email, e.active, e.work_hours_per_day
                ORDER BY workloadRatio DESC, name ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", row[0]);
                employee.put("name", row[1]);
                employee.put("firstName", row[2]);
                employee.put("lastName", row[3]);
                employee.put("email", row[4]);
                employee.put("active", ((Number) row[5]).intValue() == 1);
                employee.put("workHoursPerDay", row[6]);
                employee.put("totalMinutes", row[7]);
                employee.put("maxMinutes", ((Number) row[6]).intValue() * 60);
                employee.put("taskCount", row[8]);
                employee.put("cardCount", row[9]);

                // Workload calculations
                Number workloadRatioNum = (Number) row[10];
                Double workloadRatio = workloadRatioNum != null ?
                        workloadRatioNum.doubleValue() : 0.0;
                employee.put("workload", workloadRatio);

                // Status determination
                String status;
                boolean available;
                if (workloadRatio >= 1.0) {
                    status = "overloaded";
                    available = false;
                } else if (workloadRatio >= 0.8) {
                    status = "busy";
                    available = false;
                } else {
                    status = "available";
                    available = true;
                }

                employee.put("status", status);
                employee.put("available", available);

                // Additional computed fields
                Integer totalMinutes = ((Number) row[7]).intValue();
                employee.put("estimatedHours", Math.round(totalMinutes / 60.0 * 100.0) / 100.0);
                employee.put("totalCards", row[9]);
                employee.put("activeOrders", row[8]);

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());
            response.put("date", date != null ? date : "all");

            log.info("✅ Retrieved {} employees with planning data", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching employees with planning data", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 👤 GET EMPLOYEE DETAILS
     */
    @GetMapping("/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeeDetails(@PathVariable String employeeId) {
        try {
            log.info("👤 Fetching details for employee: {}", employeeId);

            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email,
                    COALESCE(e.active, 1) as active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    e.creation_date as creationDate,
                    e.modification_date as modificationDate
                FROM j_employee e
                WHERE HEX(e.id) = ?
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            if (results.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Employee not found");
                return ResponseEntity.status(404).body(errorResponse);
            }

            Object[] row = results.get(0);
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", row[0]);
            employee.put("name", row[1]);
            employee.put("firstName", row[2]);
            employee.put("lastName", row[3]);
            employee.put("email", row[4]);
            employee.put("active", ((Number) row[5]).intValue() == 1);
            employee.put("workHoursPerDay", row[6]);
            employee.put("creationDate", row[7]);
            employee.put("modificationDate", row[8]);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employee", employee);

            log.info("✅ Retrieved employee details for: {}", employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching employee details", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 📋 GET EMPLOYEE ORDERS
     * ✅ FIXED: Removed p.priority, using p.delai instead
     */
    @GetMapping("/{employeeId}/orders")
    public ResponseEntity<Map<String, Object>> getEmployeeOrders(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        try {
            log.info("📋 Fetching orders for employee: {} on date: {}", employeeId, date);

            String dateFilter = date != null ?
                    " AND p.planning_date = '" + date + "'" : "";

            // ✅ FIXED: Removed p.priority, using p.delai instead
            String sql = """
            SELECT 
                HEX(p.id) as planningId,
                HEX(p.order_id) as orderId,
                o.num_commande as orderNumber,
                o.delai as delai,
                p.planning_date,
                p.start_time,
                p.estimated_duration_minutes,
                p.status,
                p.completed,
                p.card_count,
                p.progress_percentage,
                ROUND(p.estimated_duration_minutes / 60.0, 2) as estimatedHours
            FROM j_planning p
            LEFT JOIN `order` o ON p.order_id = o.id
            WHERE HEX(p.employee_id) = ?"""
                    + dateFilter + """
            ORDER BY p.planning_date DESC, p.start_time ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();

                // Map all columns
                order.put("planningId", row[0]);
                order.put("orderId", row[1]);
                order.put("orderNumber", row[2]);
                order.put("delai", row[3]); // ✅ Using delai instead of priority
                order.put("planningDate", row[4]);
                order.put("startTime", row[5]);
                order.put("estimatedDurationMinutes", row[6]);
                order.put("status", row[7]);
                order.put("completed", row[8]);
                order.put("cardCount", row[9]);
                order.put("progressPercentage", row[10]);
                order.put("estimatedHours", row[11]);

                orders.add(order);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("total", orders.size());
            response.put("employeeId", employeeId);

            if (date != null) {
                response.put("date", date);
            }

            log.info("✅ Retrieved {} orders for employee {}", orders.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching employee orders", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}