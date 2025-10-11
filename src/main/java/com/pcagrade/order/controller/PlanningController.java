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
            log.info("📋 Fetching all plannings from j_planning table");

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
                FROM j_planning p
                LEFT JOIN `order` o ON p.order_id = o.id  
                LEFT JOIN j_employee e ON p.employee_id = e.id
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
     * 📋 GET EMPLOYEE PLANNING
     * ✅ FIXED: Removed p.priority and p.cards, using p.delai and real card count
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getEmployeePlanning(
            @PathVariable String employeeId,
            @RequestParam(required = false) String date) {

        try {
            log.info("📋 Getting planning for employee: {} on date: {}", employeeId, date);

            // ✅ FIXED: Removed p.priority and p.cards columns
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
                -- ORDER INFO WITH REAL CARD COUNT
                o.num_commande as orderNumber,
                o.num_commande_client as clientOrderNumber,
                o.date as orderDate,
                o.status as orderStatus,
                -- ✅ REAL CARD COUNT from card_certification_order table
                COALESCE(
                    (SELECT COUNT(*)
                     FROM card_certification_order cco
                     WHERE cco.order_id = o.id),
                    0
                ) as cardCount,
                -- ✅ CARDS WITH NAMES
                COALESCE(
                    (SELECT COUNT(*)
                     FROM card_certification_order cco
                     INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
                     LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
                     WHERE cco.order_id = o.id
                     AND (ct.name IS NOT NULL AND ct.name != '' AND ct.name != 'NULL')),
                    0
                ) as cardsWithName
            FROM j_planning p
            INNER JOIN `order` o ON p.order_id = o.id
            WHERE HEX(p.employee_id) = ?
             ORDER BY p.start_time ASC
            """;

            log.info("🔍 Executing query for employeeId: {}, date: {}", employeeId, date);

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeeId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            log.info("📋 Query returned {} results", results.size());

            List<Map<String, Object>> orders = new ArrayList<>();
            int totalCards = 0;
            int totalDuration = 0;

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                int i = 0;

                order.put("planningId", row[i++]);
                order.put("orderId", row[i++]);
                order.put("startTime", row[i++]);
                order.put("endTime", row[i++]);

                // Duration
                Number durationNum = (Number) row[i++];
                int duration = durationNum != null ? durationNum.intValue() : 0;
                order.put("duration", duration);
                order.put("durationMinutes", duration);
                order.put("estimatedDuration", duration);

                // ✅ FIXED: Using delai instead of priority
                order.put("delai", row[i++]);
                order.put("status", row[i++]);
                order.put("planningDate", row[i++]);
                order.put("orderNumber", row[i++]);
                order.put("clientOrderNumber", row[i++]);
                order.put("orderDate", row[i++]);
                order.put("orderStatus", row[i++]);

                // ✅ REAL CARD COUNT
                Number cardCountNum = (Number) row[i++];
                int cardCount = cardCountNum != null ? cardCountNum.intValue() : 0;
                order.put("cardCount", cardCount);

                Number cardsWithNameNum = (Number) row[i++];
                int cardsWithName = cardsWithNameNum != null ? cardsWithNameNum.intValue() : 0;
                order.put("cardsWithName", cardsWithName);

                // Compatibility fields for frontend
                order.put("id", order.get("orderId"));
                order.put("showCards", false);
                order.put("loadingCards", false);
                order.put("cards", new ArrayList<>());

                orders.add(order);
                totalCards += cardCount;
                totalDuration += duration;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orders", orders);
            response.put("summary", Map.of(
                    "totalOrders", orders.size(),
                    "totalCards", totalCards,
                    "totalDuration", totalDuration,
                    "estimatedHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0
            ));

            log.info("✅ Returning {} orders for employee {} (Total cards: {})", orders.size(), employeeId, totalCards);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error loading employee plannings: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orders", new ArrayList<>());

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
            log.info("🗑️ Cleaning up j_planning table");

            String countSql = "SELECT COUNT(*) FROM j_planning";
            Query countQuery = entityManager.createNativeQuery(countSql);
            Number beforeCount = (Number) countQuery.getSingleResult();

            String deleteSql = "DELETE FROM j_planning";
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
                FROM j_planning
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
     */
    @GetMapping("/order/{orderId}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String orderId) {
        try {
            log.info("🃏 Fetching cards for order: {}", orderId);

            String sql = """
            SELECT 
                HEX(cc.id) as id,
                cc.code_barre,
                COALESCE(ct.name, CONCAT('Card #', cc.code_barre)) as name,
                COALESCE(ct.label_name, CONCAT('Label #', cc.code_barre)) as label_name,
                3 as duration,
                COALESCE(cc.annotation, 0) as amount
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
            WHERE HEX(cco.order_id) = ?
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, orderId.toUpperCase());

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("id", row[0]);
                card.put("barcode", row[1]);
                card.put("name", row[2]);
                card.put("labelName", row[3]);
                card.put("duration", row[4]);
                card.put("amount", row[5] != null ? ((Number) row[5]).doubleValue() : 0.0);

                cards.add(card);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cards", cards);
            response.put("total", cards.size());

            log.info("✅ Retrieved {} cards for order {}", cards.size(), orderId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching order cards", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 📊 GET PLANNINGS BY STATUS - Filter plannings by task type
     * GET /api/planning/by-status/{status}
     *
     * @param status Planning status (2=grading, 3=certification, 4=preparation)
     */
    @GetMapping("/by-status/{status}")
    public ResponseEntity<Map<String, Object>> getPlanningsByStatus(@PathVariable int status) {
        try {
            log.info("📊 Fetching plannings for status: {}", status);

            // ✅ Query plannings filtered by status
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
                o.num_commande as orderNumber,
                o.num_commande_client as clientOrderNumber,
                CONCAT(COALESCE(e.first_name, 'Unknown'), ' ', COALESCE(e.last_name, 'User')) as employeeName,
                e.email as employeeEmail
            FROM j_planning p
            LEFT JOIN `order` o ON p.order_id = o.id  
            LEFT JOIN j_employee e ON p.employee_id = e.id
            WHERE p.status = ?
            ORDER BY p.planning_date ASC, p.start_time ASC
            """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, status);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> plannings = new ArrayList<>();
            int totalDuration = 0;
            int totalCards = 0;

            for (Object[] row : results) {
                Map<String, Object> planning = new HashMap<>();
                planning.put("id", row[0]);
                planning.put("orderId", row[1]);
                planning.put("employeeId", row[2]);
                planning.put("planningDate", row[3]);
                planning.put("startTime", row[4]);
                planning.put("endTime", row[5]);

                Integer duration = (Integer) row[6];
                planning.put("estimatedDurationMinutes", duration);
                planning.put("delai", row[7]);
                planning.put("status", row[8]);
                planning.put("completed", row[9]);

                Integer cardCount = (Integer) row[10];
                planning.put("cardCount", cardCount);
                planning.put("progressPercentage", row[11]);
                planning.put("createdAt", row[12]);
                planning.put("updatedAt", row[13]);
                planning.put("orderNumber", row[14]);
                planning.put("clientOrderNumber", row[15]);
                planning.put("employeeName", row[16]);
                planning.put("employeeEmail", row[17]);

                // Formatted duration
                if (duration != null) {
                    planning.put("formattedDuration", formatDuration(duration));
                    planning.put("estimatedHours", Math.round(duration / 60.0 * 100.0) / 100.0);
                    totalDuration += duration;
                }

                if (cardCount != null) {
                    totalCards += cardCount;
                }

                plannings.add(planning);
            }

            // Determine task type label
            String taskType = switch (status) {
                case 2 -> "Grading";
                case 3 -> "Certification";
                case 4 -> "Preparation";
                default -> "Unknown";
            };

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("plannings", plannings);
            response.put("total", plannings.size());
            response.put("status", status);
            response.put("taskType", taskType);
            response.put("summary", Map.of(
                    "totalPlannings", plannings.size(),
                    "totalCards", totalCards,
                    "totalDuration", totalDuration,
                    "totalHours", Math.round(totalDuration / 60.0 * 100.0) / 100.0
            ));

            log.info("✅ Retrieved {} {} plannings", plannings.size(), taskType);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching plannings by status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        FROM j_employee e
        INNER JOIN j_employee_group eg ON e.id = eg.employee_id
        INNER JOIN j_group g ON eg.group_id = g.id
        WHERE e.active = 1 AND g.active = 1
        GROUP BY g.group_name
    """;
        Query roleQuery = entityManager.createNativeQuery(roleSql);
        List<Object[]> roles = roleQuery.getResultList();

        result.put("employeeRoles", roles);

        return ResponseEntity.ok(result);
    }
}