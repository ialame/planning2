package com.pcagrade.order.controller;

import com.pcagrade.order.service.EmployeeService;
//import com.pcagrade.order.service.GreedyPlanningService;
import com.pcagrade.order.service.ImprovedPlanningService;
import com.pcagrade.order.service.PlanningService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/planning")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PlanningController {
    private static final Logger log = LoggerFactory.getLogger(PlanningController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private PlanningService planningService;

//    @Autowired
//    private GreedyPlanningService greedyPlanningService;

    @Autowired
    private ImprovedPlanningService improvedPlanningService;

    /**
     * 📋 GET ALL PLANNINGS
     * ✅ FIXED: Removed p.priority, using p.delai and p.card_count
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPlannings() {
        try {
            log.info("📋 Fetching all plannings from planning table");

            // ✅ FIXED: Removed p.priority, replaced p.estimated_card_count with p.card_count
            String sql = """
                SELECT 
                    HEX(p.id) as id,
                    HEX(p.order_id) as orderId,
                    HEX(p.employee_id) as employeeId,
                    p.planning_date,
                    p.start_time,
                    p.estimated_duration_minutes,
                    p.delai,
                    p.status,
                    p.completed,
                    p.card_count,
                    p.progress_percentage,
                    p.created_at,
                    p.updated_at,
                    o.num_commande as orderNumber,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as employeeName
                FROM planning p
                LEFT JOIN `order` o ON p.order_id = o.id  
                LEFT JOIN employee e ON p.employee_id = e.id
                ORDER BY p.planning_date ASC, p.start_time ASC
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> plannings = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> planning = new HashMap<>();
                planning.put("id", row[0]);
                planning.put("orderId", row[1]);
                planning.put("employeeId", row[2]);
                planning.put("planningDate", row[3]);
                planning.put("startTime", row[4]);
                planning.put("estimatedDurationMinutes", row[5]);
                planning.put("delai", row[6]);  // ✅ Using delai instead of priority
                planning.put("status", row[7]);
                planning.put("completed", row[8]);
                planning.put("cardCount", row[9]);
                planning.put("progressPercentage", row[10]);
                planning.put("createdAt", row[11]);
                planning.put("updatedAt", row[12]);
                planning.put("orderNumber", row[13]);
                planning.put("employeeName", row[14]);

                // Formatted duration
                Integer duration = (Integer) row[5];
                if (duration != null) {
                    planning.put("formattedDuration", formatDuration(duration));
                    planning.put("estimatedHours", Math.round(duration / 60.0 * 100.0) / 100.0);
                }

                plannings.add(planning);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("plannings", plannings);
            response.put("total", plannings.size());

            log.info("✅ Retrieved {} plannings successfully", plannings.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching plannings", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 📋 GET EMPLOYEE PLANNING - FINAL FIX (Sans card_certification_order)
     * Location: src/main/java/com/pcagrade/order/controller/PlanningController.java
     *
     * REMPLACEZ la méthode getEmployeePlanning() complète par ce code
     */

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeePlanning(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        try {
            log.info("📋 Getting planning for employee: {} on date: {}", employeeId, date);

            // Build date filter
            String dateFilter = date != null && !date.isEmpty() ?
                    " AND p.planning_date = '" + date + "'" : "";

            // ✅ FIXED: Using correct column names from actual database schema
            String sql = """
            SELECT 
                HEX(p.id) as planningId,
                HEX(p.order_id) as orderId,
                p.start_time,
                p.end_time,
                p.estimated_duration_minutes as duration,
                p.delai,
                p.status,
                p.planning_date,
                p.card_count,
                p.completed,
                p.progress_percentage,
                -- ORDER INFO (using correct column names)
                o.order_number as orderNumber,
                o.customer_name as clientOrderNumber,
                o.date as date,
                o.status as orderStatus,
                o.delai as orderDelai,
                -- Card count from order table directly
                COALESCE(o.total_cards, 0) as cardCount,
                COALESCE(o.total_cards, 0) as cardsWithName
            FROM planning p
            INNER JOIN `order` o ON p.order_id = o.id
            WHERE HEX(p.employee_id) = ?
            """ + dateFilter + """
            ORDER BY p.planning_date DESC, p.start_time ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            log.info("📊 Found {} planning entries for employee {}", results.size(), employeeId);

            List<Map<String, Object>> orders = new ArrayList<>();
            int totalCards = 0;
            int totalDuration = 0;

            for (Object[] row : results) {
                int i = 0;
                Map<String, Object> order = new HashMap<>();

                // Planning info
                order.put("planningId", row[i++]);
                order.put("orderId", row[i++]);
                order.put("startTime", row[i++]);
                order.put("endTime", row[i++]);

                Number durationNum = (Number) row[i++];
                int duration = durationNum != null ? durationNum.intValue() : 0;
                order.put("duration", duration);
                order.put("durationMinutes", duration);
                order.put("estimatedDurationMinutes", duration);

                order.put("delai", row[i++]);
                order.put("status", row[i++]);
                order.put("planningDate", row[i++]);
                order.put("cardCount", row[i++]);
                order.put("completed", row[i++]);
                order.put("progressPercentage", row[i++]);

                // Order info
                order.put("orderNumber", row[i++]);
                order.put("clientOrderNumber", row[i++]);
                order.put("orderDate", row[i++]);
                order.put("orderStatus", row[i++]);
                order.put("orderDelai", row[i++]);

                Number cardCountNum = (Number) row[i++];
                int cardCount = cardCountNum != null ? cardCountNum.intValue() : 0;
                order.put("totalCards", cardCount);

                Number cardsWithNameNum = (Number) row[i++];
                int cardsWithName = cardsWithNameNum != null ? cardsWithNameNum.intValue() : 0;
                order.put("cardsWithName", cardsWithName);

                // Frontend compatibility fields
                order.put("id", order.get("orderId"));
                order.put("showCards", false);
                order.put("loadingCards", false);
                order.put("cards", new ArrayList<>());

                orders.add(order);
                totalCards += cardCount;
                totalDuration += duration;
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("plannings", orders); // For compatibility
            response.put("summary", Map.of(
                    "totalOrders", orders.size(),
                    "totalCards", totalCards,
                    "totalDuration", totalDuration,
                    "totalMinutes", totalDuration,
                    "estimatedHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0
            ));

            log.info("✅ Returning {} orders for employee {} on date {} (Total cards: {})",
                    orders.size(), employeeId, date != null ? date : "ALL", totalCards);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading employee planning: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orders", new ArrayList<>());
            errorResponse.put("plannings", new ArrayList<>());

            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 📊 GET EMPLOYEES WITH PLANNING DATA
     * ✅ FIXED: Removed p.priority references
     */
    @GetMapping("/employees-stats")
    public ResponseEntity<Map<String, Object>> getEmployeesWithPlanningStats(
            @RequestParam(required = false) String date) {

        try {
            log.info("📊 Fetching employees with planning stats for date: {}", date);

            String dateFilter = date != null ?
                    " AND p.planning_date = '" + date + "'" : "";

            // ✅ FIXED: Removed p.priority, using only necessary columns
            String sql = """
                SELECT 
                    HEX(e.id) as employeeId,
                    CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as name,
                    e.first_name,
                    e.last_name,
                    e.email,
                    e.active,
                    COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                    COALESCE(SUM(p.estimated_duration_minutes), 0) as totalMinutes,
                    COUNT(p.id) as taskCount,
                    COALESCE(SUM(p.card_count), 0) as cardCount,
                    ROUND(COALESCE(SUM(p.estimated_duration_minutes), 0) / (COALESCE(e.work_hours_per_day, 8) * 60.0), 2) as workloadRatio
                FROM employee e
                LEFT JOIN planning p ON e.id = p.employee_id""" + dateFilter + """
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
                employee.put("active", row[5]);
                employee.put("workHoursPerDay", row[6]);
                employee.put("totalMinutes", row[7]);
                employee.put("maxMinutes", ((Number) row[6]).intValue() * 60);
                employee.put("taskCount", row[8]);
                employee.put("cardCount", row[9]);
                employee.put("workloadRatio", row[10]);

                // Status based on workload
                Double workloadRatio = (Double) row[10];
                String status;
                if (workloadRatio >= 1.0) {
                    status = "overloaded";
                } else if (workloadRatio >= 0.8) {
                    status = "busy";
                } else {
                    status = "available";
                }
                employee.put("status", status);
                employee.put("available", workloadRatio < 0.8);

                employees.add(employee);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("employees", employees);
            response.put("total", employees.size());
            response.put("date", date != null ? date : "all");

            log.info("✅ Retrieved {} employees with planning stats", employees.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching employees with planning stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 🗑️ DELETE ALL PLANNINGS
     */
    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupPlannings() {
        try {
            log.info("🗑️ Cleaning up planning table");

            String countSql = "SELECT COUNT(*) FROM planning";
            Query countQuery = entityManager.createNativeQuery(countSql);
            Number beforeCount = (Number) countQuery.getSingleResult();

            String deleteSql = "DELETE FROM planning";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deletedRows = deleteQuery.executeUpdate();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Planning table cleaned successfully");
            response.put("rowsDeleted", deletedRows);
            response.put("beforeCount", beforeCount.intValue());

            log.info("✅ Deleted {} planning records", deletedRows);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error cleaning planning table", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 📈 GET PLANNING STATS
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPlanningStats() {
        try {
            log.info("📈 Fetching planning statistics");

            String sql = """
                SELECT 
                    COUNT(*) as totalPlannings,
                    COUNT(DISTINCT employee_id) as employeesUsed,
                    COUNT(DISTINCT order_id) as ordersPlanned,
                    SUM(estimated_duration_minutes) as totalMinutes,
                    SUM(card_count) as totalCards
                FROM planning
                """;

            Query query = entityManager.createNativeQuery(sql);
            Object[] result = (Object[]) query.getSingleResult();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPlannings", ((Number) result[0]).intValue());
            stats.put("employeesUsed", ((Number) result[1]).intValue());
            stats.put("ordersPlanned", ((Number) result[2]).intValue());

            Number totalMinutesNum = (Number) result[3];
            int totalMinutes = totalMinutesNum != null ? totalMinutesNum.intValue() : 0;
            stats.put("totalMinutes", totalMinutes);
            stats.put("totalHours", Math.round(totalMinutes / 60.0 * 100.0) / 100.0);
            stats.put("totalCards", result[4] != null ? ((Number) result[4]).intValue() : 0);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching planning stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 🎯 GENERATE ROLE-BASED PLANNING
     */
    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateRoleBasedPlanning(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> result = new HashMap<>();

        try {
            String planningDateStr = (String) request.getOrDefault("planningDate",
                    LocalDate.now().toString());
            boolean cleanFirst = (Boolean) request.getOrDefault("cleanFirst", true);

            LocalDate planningDate = LocalDate.parse(planningDateStr);

            log.info("🎯 Generating role-based planning for date: {}, cleanFirst: {}",
                    planningDate, cleanFirst);

            result = improvedPlanningService.executeRoleBasedPlanning(planningDate, cleanFirst);

            if ((Boolean) result.get("success")) {
                log.info("✅ Role-based planning successful: {}", result.get("message"));
                return ResponseEntity.ok(result);
            } else {
                log.warn("⚠️ Role-based planning failed: {}", result.get("message"));
                return ResponseEntity.status(500).body(result);
            }

        } catch (Exception e) {
            log.error("❌ Error in role-based planning generation", e);

            result.put("success", false);
            result.put("message", "Role-based planning failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 🃏 GET ORDER CARDS
     * Location: src/main/java/com/pcagrade/order/controller/PlanningController.java
     *
     * Replace the existing getOrderCards method with this version
     *
     * Fixes: Handles Boolean fields correctly, doesn't try to cast Boolean to Number
     */
    @GetMapping("/order/{orderId}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String orderId) {
        try {
            log.info("🃏 Fetching cards for order: {}", orderId);

            // Query with explicit type casting for booleans
            String sql = """
            SELECT 
                HEX(cc.id) as id,
                cc.code_barre,
                cc.card_name,
                COALESCE(cc.custom_label, cc.code_barre) as label_name,
                3 as duration,
                CAST(cc.grading_completed AS SIGNED) as grading_completed,
                CAST(cc.certification_completed AS SIGNED) as certification_completed,
                CAST(cc.scanning_completed AS SIGNED) as scanning_completed,
                CAST(cc.packaging_completed AS SIGNED) as packaging_completed
            FROM card_certification cc
            WHERE HEX(cc.order_id) = ?
                AND cc.deleted = 0
            ORDER BY cc.id
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, orderId.toUpperCase().replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();
            int totalDuration = 0;

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();

                // Basic info (row[0] to row[4])
                card.put("id", row[0]);
                card.put("barcode", row[1]);
                card.put("code_barre", row[1]); // Alias for compatibility

                // Card name
                String cardName = row[2] != null ? row[2].toString() : null;
                card.put("name", cardName != null ? cardName : "Card #" + row[1]);
                card.put("cardName", cardName);

                // Label
                card.put("labelName", row[3]);
                card.put("label_name", row[3]); // Alias for compatibility

                // Duration (always 3 minutes per card in this system)
                int duration = row[4] != null ? ((Number) row[4]).intValue() : 3;
                card.put("duration", duration);

                // Amount/Quantity (default to 1 since we don't have this field)
                int amount = 1;
                card.put("amount", amount);
                card.put("quantity", amount);

                // Processing status - now correctly handled as integers (row[5] to row[8])
                boolean gradingCompleted = row[5] != null && ((Number) row[5]).intValue() == 1;
                boolean certificationCompleted = row[6] != null && ((Number) row[6]).intValue() == 1;
                boolean scanningCompleted = row[7] != null && ((Number) row[7]).intValue() == 1;
                boolean packagingCompleted = row[8] != null && ((Number) row[8]).intValue() == 1;

                card.put("gradingCompleted", gradingCompleted);
                card.put("certificationCompleted", certificationCompleted);
                card.put("scanningCompleted", scanningCompleted);
                card.put("packagingCompleted", packagingCompleted);

                // Calculate status text based on completion
                String status;
                if (packagingCompleted) {
                    status = "completed";
                } else if (scanningCompleted) {
                    status = "scanning_done";
                } else if (certificationCompleted) {
                    status = "certification_done";
                } else if (gradingCompleted) {
                    status = "grading_done";
                } else {
                    status = "pending";
                }
                card.put("status", status);

                // Grade placeholder (not available in current schema)
                card.put("grade", "N/A");

                cards.add(card);
                totalDuration += (duration * amount);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cards", cards);
            response.put("totalCards", cards.size());

            long cardsWithName = cards.stream()
                    .filter(c -> c.get("cardName") != null && !c.get("cardName").toString().isEmpty())
                    .count();
            response.put("cardsWithName", cardsWithName);

            response.put("estimatedDuration", totalDuration);

            int namePercentage = cards.isEmpty() ? 0 :
                    (int) ((cardsWithName * 100.0) / cards.size());
            response.put("namePercentage", namePercentage);

            log.info("✅ Retrieved {} cards for order {} (Duration: {} min, {}% with names)",
                    cards.size(), orderId, totalDuration, namePercentage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching cards for order {}: {}", orderId, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("cards", new ArrayList<>());
            errorResponse.put("totalCards", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Pokemon Card Planning");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }

    /**
     * Format duration helper
     */
    private String formatDuration(int minutes) {
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        return remainingMinutes > 0 ? hours + "h" + remainingMinutes + "min" : hours + "h";
    }

    @PostMapping("/generate-debug")
    @Transactional
    public ResponseEntity<Map<String, Object>> generateDebugPlanning() {
        Map<String, Object> result = new HashMap<>();

        // Get counts
        String countSql = "SELECT status, COUNT(*) FROM `order` WHERE status IN (2,3,4) GROUP BY status";
        Query countQuery = entityManager.createNativeQuery(countSql);
        List<Object[]> counts = countQuery.getResultList();

        result.put("orderCounts", counts);

        // Get employee roles
        String roleSql = """
        SELECT g.group_name, COUNT(DISTINCT e.id)
        FROM employee e
        INNER JOIN employee_group eg ON e.id = eg.employee_id
        INNER JOIN team g ON eg.group_id = g.id
        WHERE e.active = 1 AND g.active = 1
        GROUP BY g.group_name
    """;
        Query roleQuery = entityManager.createNativeQuery(roleSql);
        List<Object[]> roles = roleQuery.getResultList();

        result.put("employeeRoles", roles);

        return ResponseEntity.ok(result);
    }

    /**
     * FIX for PlanningController - GET /api/planning/by-status/{status}
     *
     * This fixes the SQL query to use correct column names:
     * - order_number instead of num_commande
     * - customer_name instead of num_commande_client
     *
     * LOCATION: src/main/java/com/pcagrade/order/controller/PlanningController.java
     * REPLACE the getPlanningsByStatus() method with this:
     */

    @GetMapping("/by-status/{status}")
    public ResponseEntity<Map<String, Object>> getPlanningsByStatus(@PathVariable int status) {
        try {
            log.info("📊 Fetching plannings for status: {}", status);

            // ✅ FIXED: Using correct column names from actual database schema
            String sql = """
        SELECT 
            HEX(p.id) as id,
            HEX(p.order_id) as orderId,
            HEX(p.employee_id) as employeeId,
            p.planning_date,
            p.start_time,
            p.end_time,
            p.estimated_duration_minutes,
            p.delai,
            p.status,
            p.completed,
            p.card_count,
            p.progress_percentage,
            p.created_at,
            p.updated_at,
            o.order_number as orderNumber,
            o.customer_name as clientOrderNumber,
            CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as employeeName,
            e.email as employeeEmail
        FROM planning p
        LEFT JOIN `order` o ON p.order_id = o.id  
        LEFT JOIN employee e ON p.employee_id = e.id
        WHERE p.status = ?
        ORDER BY p.planning_date ASC, p.start_time ASC
        """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, status);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> plannings = new ArrayList<>();
            int totalCards = 0;
            int totalMinutes = 0;

            for (Object[] row : results) {
                Map<String, Object> planning = new HashMap<>();

                planning.put("id", row[0]);
                planning.put("orderId", row[1]);
                planning.put("employeeId", row[2]);
                planning.put("planningDate", row[3] != null ? row[3].toString() : null);
                planning.put("startTime", row[4] != null ? row[4].toString() : null);
                planning.put("endTime", row[5] != null ? row[5].toString() : null);

                int duration = row[6] != null ? ((Number) row[6]).intValue() : 0;
                planning.put("estimatedDurationMinutes", duration);

                planning.put("delai", row[7]);
                planning.put("status", row[8]);
                planning.put("completed", row[9]);

                int cardCount = row[10] != null ? ((Number) row[10]).intValue() : 0;
                planning.put("cardCount", cardCount);

                planning.put("progressPercentage", row[11] != null ? ((Number) row[11]).intValue() : 0);
                planning.put("createdAt", row[12]);
                planning.put("updatedAt", row[13]);
                planning.put("orderNumber", row[14]);
                planning.put("clientOrderNumber", row[15]);
                planning.put("employeeName", row[16]);
                planning.put("employeeEmail", row[17]);

                totalCards += cardCount;
                totalMinutes += duration;

                plannings.add(planning);
            }

            // Summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalPlannings", plannings.size());
            summary.put("totalCards", totalCards);
            summary.put("totalMinutes", totalMinutes);
            summary.put("totalHours", Math.round(totalMinutes / 60.0 * 100.0) / 100.0);
            summary.put("status", status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("plannings", plannings);
            response.put("summary", summary);

            log.info("✅ Found {} plannings for status {}", plannings.size(), status);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching plannings by status {}", status, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("plannings", new ArrayList<>());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


}