package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class PlanningGenerationService {
    private static final Logger log = LoggerFactory.getLogger(PlanningGenerationService.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmployeeService employeeService;

    /**
     * 🎯 MÉTHODE TRANSACTIONNELLE qui fonctionne
     * La clé : @Transactional avec REQUIRES_NEW pour isoler la transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> createPlanningsTransactional(String startDate, Integer timePerCard, Boolean cleanFirst) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🎯 TRANSACTIONAL PLANNING SERVICE - REQUIRES_NEW");

            // ========== CLEAN FIRST ==========
            if (cleanFirst) {
                try {
                    String deleteQuery = "DELETE FROM planning";
                    Query deleteQ = entityManager.createNativeQuery(deleteQuery);
                    int deleted = deleteQ.executeUpdate();
                    entityManager.flush(); // Force immediate execution
                    log.info("🧹 Cleaned {} existing plannings", deleted);
                } catch (Exception cleanError) {
                    log.error("❌ Clean failed: {}", cleanError.getMessage());
                    throw cleanError; // Fail fast if clean fails
                }
            }

            // ========== GET EMPLOYEES ==========
            List<Map<String, Object>> employees = employeeService.getAllActiveEmployees();
            if (employees.isEmpty()) {
                result.put("success", false);
                result.put("message", "No active employees found");
                return result;
            }

            // ========== GET ORDERS ==========
            String orderQuery = """
                SELECT 
                    HEX(o.id) as orderId,
                    o.num_commande as orderNumber,
                    o.date as orderDate
                FROM card_order o
                WHERE o.date >= ?
                ORDER BY o.date ASC
                LIMIT 10
            """;

            Query query = entityManager.createNativeQuery(orderQuery);
            query.setParameter(1, startDate);
            @SuppressWarnings("unchecked")
            List<Object[]> orderResults = query.getResultList();

            if (orderResults.isEmpty()) {
                result.put("success", false);
                result.put("message", "No orders found from date: " + startDate);
                return result;
            }

            // ========== CREATE PLANNINGS ==========
            List<Map<String, Object>> createdPlannings = new ArrayList<>();
            int planningsSaved = 0;

            for (int i = 0; i < orderResults.size(); i++) {
                Object[] orderData = orderResults.get(i);
                String orderId = (String) orderData[0];
                String orderNumber = (String) orderData[1];
                Object orderDate = orderData[2];

                Map<String, Object> employee = employees.get(i % employees.size());
                String employeeId = (String) employee.get("id");
                String employeeName = employee.get("firstName") + " " + employee.get("lastName");

                // Calculate timing
                int cardCount = 20;
                int durationMinutes = cardCount * timePerCard;
                LocalDateTime startTime = LocalDate.parse(startDate).atTime(9, 0).plusHours(i);

                String planningId = UUID.randomUUID().toString().replace("-", "");

                // ========== SIMPLE INSERT ==========
                String insertQuery = """
                    INSERT INTO planning (
                        id, order_id, employee_id, planning_date, start_time,
                        estimated_duration_minutes, status, card_count, created_at, updated_at
                    ) VALUES (
                        UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, 'PLANNED', ?, NOW(), NOW()
                    )
                """;

                Query insertQ = entityManager.createNativeQuery(insertQuery);
                insertQ.setParameter(1, planningId);
                insertQ.setParameter(2, orderId);
                insertQ.setParameter(3, employeeId);
                insertQ.setParameter(4, startTime.toLocalDate());
                insertQ.setParameter(5, startTime.toLocalTime());
                insertQ.setParameter(6, durationMinutes);
                insertQ.setParameter(7, cardCount);

                int rowsInserted = insertQ.executeUpdate();

                if (rowsInserted > 0) {
                    planningsSaved++;

                    Map<String, Object> planningResult = new HashMap<>();
                    planningResult.put("planningId", planningId);
                    planningResult.put("orderId", orderId);
                    planningResult.put("orderNumber", orderNumber);
                    planningResult.put("orderDate", orderDate);
                    planningResult.put("employeeId", employeeId);
                    planningResult.put("employeeName", employeeName);
                    planningResult.put("plannedDate", startTime.toLocalDate().toString());
                    planningResult.put("startTime", startTime.toLocalTime().toString());
                    planningResult.put("durationMinutes", durationMinutes);
                    planningResult.put("cardCount", cardCount);

                    createdPlannings.add(planningResult);
                    log.info("✅ Created planning for order: {} -> Employee: {}", orderNumber, employeeName);
                }
            }

            // ========== FORCE COMMIT ==========
            entityManager.flush();

            // ========== RESULT ==========
            result.put("success", true);
            result.put("message", String.format("✅ TRANSACTIONAL SUCCESS: %d plannings created", planningsSaved));
            result.put("processedOrders", planningsSaved);
            result.put("totalOrdersAnalyzed", orderResults.size());
            result.put("activeEmployees", employees.size());
            result.put("planningsCreated", createdPlannings);
            result.put("timePerCard", timePerCard);
            result.put("startDate", startDate);
            result.put("algorithm", "SERVICE_TRANSACTIONAL");

            log.info("🎉 TRANSACTIONAL SERVICE SUCCESS: {} plannings created", planningsSaved);
            return result;

        } catch (Exception e) {
            log.error("❌ Transactional service failed: {}", e.getMessage(), e);

            result.put("success", false);
            result.put("message", "Service failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());

            // Re-throw to trigger transaction rollback
            throw new RuntimeException("Planning service failed", e);
        }
    }
}