package com.pcagrade.order.service;

import com.pcagrade.order.util.PlanningUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Greedy Planning Service - Simple greedy algorithm for task assignment
 */
@Service
@Slf4j
public class GreedyPlanningService {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private EntityManager entityManager;
    /**
     * Execute greedy planning algorithm
     * @param day target day
     * @param month target month
     * @param year target year
     * @return planning result
     */
    public Map<String, Object> executeGreedyPlanning(int day, int month, int year) {
        try {
            log.info("🎲 Starting Greedy Planning for date: {}/{}/{}", day, month, year);

            Map<String, Object> result = new HashMap<>();

            // ✅ NOUVEAU: Nettoyer les planifications existantes pour cette date
            cleanExistingPlanningsForDate(day, month, year);

            // 1. Get active employees
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                log.warn("❌ No active employees found");
                result.put("success", false);
                result.put("message", "❌ No employees available");
                return result;
            }
            log.info("Found {} active employees", employees.size());

            // 2. Get orders to plan
            List<Map<String, Object>> orders = orderService.getOrdersForPlanning(day, month, year);
            if (orders.isEmpty()) {
                log.info("No orders found for planning");
                result.put("success", true);
                result.put("message", "No orders to plan");
                result.put("plannings", new ArrayList<>());
                return result;
            }
            log.info("Found {} orders to plan", orders.size());

            // 3. Execute greedy algorithm with improved distribution
            List<Map<String, Object>> createdPlannings = new ArrayList<>();
            int employeeIndex = 0;

            // ✅ CORRECTION: Sauvegarder en base de données immédiatement pour éviter les doublons
            for (Map<String, Object> order : orders) {
                int currentEmployeeIndex = employeeIndex % employees.size();
                Map<String, Object> employee = employees.get(currentEmployeeIndex);
                String employeeId = (String) employee.get("id");
                String orderId = (String) order.get("id");

                // ✅ Vérification finale avant création
                if (!isPlanningAlreadyExists(orderId, employeeId)) {

                    // Calculer les données de planification
                    Integer cardCount = (Integer) order.get("cardCount");
                    if (cardCount == null) {
                        cardCount = (Integer) order.get("nombreCartes");
                    }
                    if (cardCount == null) {
                        cardCount = 10; // Default fallback
                    }

                    int durationMinutes = Math.max(60, 30 + cardCount * 3);

                    // ✅ Récupérer le delai
                    String delai = PlanningUtils.getDelaiForPlanning(order);

                    // ✅ Créer le LocalDate et LocalDateTime
                    LocalDate planningDate = LocalDate.of(year, month, day);
                    LocalDateTime startTime = LocalDateTime.of(planningDate, LocalTime.of(9, 0));  // ← AJOUTÉ

                    // ✅ CORRIGÉ: Passer les 7 paramètres
                    boolean saved = savePlanningToDatabase(
                            orderId,
                            employeeId,
                            planningDate,    // 3
                            startTime,       // 4 ← AJOUTÉ
                            durationMinutes, // 5
                            cardCount,       // 6
                            delai            // 7
                    );

                    if (saved) {
                        String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                        // Créer l'objet résultat
                        Map<String, Object> planning = new HashMap<>();
                        planning.put("order_id", orderId);
                        planning.put("employee_id", employeeId);
                        planning.put("employee_name", employeeName);
                        planning.put("duration_minutes", durationMinutes);
                        planning.put("card_count", cardCount);
                        planning.put("order_number", order.get("orderNumber"));
                        planning.put("delai", delai);

                        createdPlannings.add(planning);

                        log.info("✅ Order {} assigned to employee {} (saved to DB)",
                                order.get("orderNumber"), employeeName);
                    }
                }
                employeeIndex++; // Toujours incrémenter pour maintenir la rotation
            }

            // Suite du code existant...
            result.put("success", true);
            result.put("message", String.format("✅ Greedy planning completed: %d assignments created",
                    createdPlannings.size()));
            result.put("plannings", createdPlannings);
            result.put("totalPlannings", createdPlannings.size());
            result.put("totalEmployees", employees.size());
            result.put("totalOrders", orders.size());

            return result;

        } catch (Exception e) {
            log.error("❌ Error in greedy planning: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error in greedy planning: " + e.getMessage());
            return error;
        }
    }

    /**
     * Execute simple greedy planning without date parameters
     * @return planning result
     */
    public Map<String, Object> executeGreedyPlanning() {
        LocalDate today = LocalDate.now();
        return executeGreedyPlanning(today.getDayOfMonth(), today.getMonthValue(), today.getYear());
    }

    /**
     * Get algorithm information
     * @return algorithm details
     */
    public Map<String, Object> getAlgorithmInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Greedy Planning Algorithm");
        info.put("description", "Simple greedy algorithm that assigns orders to employees in round-robin fashion");
        info.put("timeComplexity", "O(n)");
        info.put("spaceComplexity", "O(1)");
        info.put("advantages", List.of(
                "Fast execution",
                "Simple implementation",
                "Predictable behavior",
                "Even workload distribution"
        ));
        info.put("disadvantages", List.of(
                "May not find optimal solution",
                "Doesn't consider employee skills",
                "Ignores priority optimization",
                "No time slot optimization"
        ));
        return info;
    }

    /**
     * Validate planning parameters
     * @param day day of month
     * @param month month (1-12)
     * @param year year
     * @return true if valid
     */
    private boolean isValidDate(int day, int month, int year) {
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculate basic statistics for greedy planning
     * @param plannings list of planning entries
     * @param employees list of employees
     * @return statistics map
     */
    private Map<String, Object> calculateGreedyStatistics(List<Map<String, Object>> plannings,
                                                          List<Map<String, Object>> employees) {
        Map<String, Object> stats = new HashMap<>();

        // Total assignments
        stats.put("totalAssignments", plannings.size());

        // Assignments per employee
        Map<String, Integer> assignmentCounts = new HashMap<>();
        int totalDuration = 0;

        for (Map<String, Object> planning : plannings) {
            String employeeId = (String) planning.get("employee_id");
            assignmentCounts.merge(employeeId, 1, Integer::sum);

            Integer duration = (Integer) planning.get("duration_minutes");
            if (duration != null) {
                totalDuration += duration;
            }
        }

        stats.put("assignmentCounts", assignmentCounts);
        stats.put("totalDurationMinutes", totalDuration);
        stats.put("averageDurationMinutes", plannings.isEmpty() ? 0 : totalDuration / plannings.size());

        // Employee utilization
        if (!employees.isEmpty()) {
            stats.put("employeesUsed", assignmentCounts.size());
            stats.put("employeeUtilizationPercent",
                    Math.round((double) assignmentCounts.size() / employees.size() * 100));
        }

        return stats;
    }
    /**
     * Check if planning already exists for this order and employee
     * @param orderId the order ID
     * @param employeeId the employee ID
     * @return true if planning exists, false otherwise
     */
    private boolean isPlanningAlreadyExists(String orderId, String employeeId) {
        try {
            String checkSql = """
        SELECT COUNT(*) FROM j_planning 
        WHERE order_id = UNHEX(?) 
        AND employee_id = UNHEX(?)
        """;

            Query checkQuery = entityManager.createNativeQuery(checkSql);
            checkQuery.setParameter(1, orderId.replace("-", ""));
            checkQuery.setParameter(2, employeeId.replace("-", ""));

            Number count = (Number) checkQuery.getSingleResult();
            boolean exists = count.intValue() > 0;

            if (exists) {
                log.debug("Planning already exists for order {} and employee {}", orderId, employeeId);
            }

            return exists;

        } catch (Exception e) {
            log.warn("Error checking existing planning: {}", e.getMessage());
            return false; // Si erreur, on assume qu'il n'existe pas pour éviter de bloquer
        }
    }
    private void cleanExistingPlanningsForDate(int day, int month, int year) {
        try {
            LocalDate targetDate = LocalDate.of(year, month, day);

            String deleteSql = """
        DELETE FROM j_planning 
        WHERE planning_date = ?
        """;

            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            deleteQuery.setParameter(1, targetDate);

            int deletedCount = deleteQuery.executeUpdate();
            log.info("🗑️ Cleaned {} existing plannings for date {}", deletedCount, targetDate);

        } catch (Exception e) {
            log.warn("Error cleaning existing plannings: {}", e.getMessage());
        }
    }

    private boolean savePlanningToDatabase(
            String orderId,
            String employeeId,
            LocalDate planningDate,
            LocalDateTime startDateTime,
            int durationMinutes,
            int cardCount,
            String delai  // ✅ AJOUTÉ
    ) {
        try {
            String planningId = UUID.randomUUID().toString().replace("-", "");
            LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

            // ✅ NOUVELLE REQUÊTE SQL
            String insertSql = """
            INSERT INTO j_planning (
                id, order_id, employee_id, 
                planning_date, start_time, end_time,
                estimated_duration_minutes, estimated_end_time,
                delai, status, completed, card_count,
                notes, progress_percentage, created_at, updated_at
            ) VALUES (
                UNHEX(?), UNHEX(?), UNHEX(?), 
                ?, ?, ?, 
                ?, ?,
                ?, ?, ?, ?,
                ?, ?, NOW(), NOW()
            )
        """;

            Query insertQuery = entityManager.createNativeQuery(insertSql);
            insertQuery.setParameter(1, planningId);
            insertQuery.setParameter(2, orderId.replace("-", ""));
            insertQuery.setParameter(3, employeeId.replace("-", ""));
            insertQuery.setParameter(4, planningDate);
            insertQuery.setParameter(5, startDateTime);
            insertQuery.setParameter(6, endDateTime);
            insertQuery.setParameter(7, durationMinutes);
            insertQuery.setParameter(8, endDateTime);
            insertQuery.setParameter(9, delai);  // ✅ VARCHAR
            insertQuery.setParameter(10, 2);     // ✅ INT: status = 2 (A_NOTER)
            insertQuery.setParameter(11, 0);     // completed = false
            insertQuery.setParameter(12, cardCount);
            insertQuery.setParameter(13, String.format("Auto-generated: %d cards", cardCount));
            insertQuery.setParameter(14, 0);     // progress_percentage

            int rowsAffected = insertQuery.executeUpdate();
            return rowsAffected > 0;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage());
            return false;
        }
    }

}