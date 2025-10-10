package com.pcagrade.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎴 SERVICE DE PLANIFICATION POKÉMON - VERSION AMÉLIORÉE
 * Remplace l'ancien PlanningService avec des algorithmes optimisés
 */
@Service
@Slf4j
public class PlanningService {

    @Autowired
    private EntityManager entityManager;

    // ========== CONSTANTES ==========
    private static final int MINUTES_PER_CARD = 3;
    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2025, 6, 1);
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(17, 0);

    // ========== MÉTHODES PUBLIQUES PRINCIPALES ==========

    /**
     * 🚀 GÉNÉRATION OPTIMISÉE DE PLANIFICATION
     * Remplace generatePlanningBatch avec algorithme amélioré
     */
    @Transactional
    public Map<String, Object> generatePlanningBatch(String dateDebut, int nombreEmployes, int tempsParCarte) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🎮 Démarrage génération planification optimisée depuis: {}", dateDebut);

            LocalDate startDate = dateDebut != null ?
                    LocalDate.parse(dateDebut) : DEFAULT_START_DATE;
            int timePerCard = tempsParCarte > 0 ? tempsParCarte : MINUTES_PER_CARD;

            // 1. 📊 CHARGEMENT DES DONNÉES
            List<Map<String, Object>> orders = loadOrdersByPriority(startDate);
            List<Map<String, Object>> employees = loadAvailableEmployees();

            if (orders.isEmpty()) {
                result.put("success", true);
                result.put("message", "Aucune commande à planifier");
                result.put("ordersProcessed", 0);
                return result;
            }

            if (employees.isEmpty()) {
                result.put("success", false);
                result.put("message", "Aucun employé disponible");
                return result;
            }

            log.info("📦 {} commandes à traiter avec {} employés", orders.size(), employees.size());

            // 2. 🔄 ALGORITHME DE RÉPARTITION OPTIMISÉ
            List<EmployeeWorkload> workloads = initializeWorkloads(employees);
            List<Map<String, Object>> createdPlannings = new ArrayList<>();
            int planningsSaved = 0;

// ✅ NOUVEAU: Nettoyer les planifications existantes pour éviter les doublons
            cleanExistingPlannings(startDate);

// ✅ NOUVEAU: Map pour suivre les assignations et éviter les doublons
            Set<String> assignedOrders = new HashSet<>();

            for (Map<String, Object> order : orders) {
                try {
                    String orderId = (String) order.get("id");

                    // ✅ Vérifier si cette commande a déjà été assignée
                    if (assignedOrders.contains(orderId)) {
                        log.warn("Order {} already assigned, skipping", order.get("numCommande"));
                        continue;
                    }

                    // ✅ Vérifier en base de données aussi
                    if (isPlanningExistsForOrder(orderId)) {
                        log.warn("Order {} already has planning in database, skipping", order.get("numCommande"));
                        assignedOrders.add(orderId);
                        continue;
                    }

                    // Trouver l'employé le moins chargé
                    EmployeeWorkload leastBusy = findLeastBusyEmployee(workloads);

                    // Calculer la durée et le timing
                    Integer cardCount = (Integer) order.get("nombreCartes");
                    if (cardCount == null || cardCount <= 0) cardCount = 1;

                    int durationMinutes = cardCount * timePerCard;
                    LocalDateTime startTime = calculateOptimalStartTime(leastBusy, order);

                    // Créer la planification
                    String planningId = UUID.randomUUID().toString().replace("-", "");
                    String employeeId = leastBusy.getEmployee().get("id").toString();
                    String priority = (String) order.getOrDefault("priorite", "MEDIUM");

                    // Sauvegarder en base
                    boolean saved = savePlanningOptimized(
                            planningId, orderId, employeeId,
                            startTime.toLocalDate(), startTime,
                            durationMinutes, priority, cardCount);

                    if (saved) {
                        planningsSaved++;
                        assignedOrders.add(orderId); // ✅ Marquer comme assigné

                        // Mettre à jour la charge de travail
                        leastBusy.addWorkload(durationMinutes, startTime);

                        // Ajouter au résultat
                        Map<String, Object> planningResult = new HashMap<>();
                        planningResult.put("id", planningId);
                        planningResult.put("orderId", orderId);
                        planningResult.put("employeeId", employeeId);
                        planningResult.put("employeeName", leastBusy.getEmployee().get("firstName") +
                                " " + leastBusy.getEmployee().get("lastName"));
                        planningResult.put("durationMinutes", durationMinutes);
                        planningResult.put("cardCount", cardCount);
                        planningResult.put("priority", priority);
                        planningResult.put("startTime", startTime);

                        createdPlannings.add(planningResult);

                        log.info("✅ Order {} assigned to employee {} (duration: {}min)",
                                order.get("numCommande"),
                                planningResult.get("employeeName"),
                                durationMinutes);
                    }

                } catch (Exception orderError) {
                    log.error("❌ Error processing order {}: {}",
                            order.get("numCommande"), orderError.getMessage());
                }
            }

            // 3. 📊 STATISTIQUES FINALES
            int totalCards = createdPlannings.stream()
                    .mapToInt(p -> (Integer) p.get("cardCount"))
                    .sum();
            int totalMinutes = createdPlannings.stream()
                    .mapToInt(p -> (Integer) p.get("durationMinutes"))
                    .sum();

            result.put("success", true);
            result.put("message", String.format("🎉 Planification terminée - %d plannings créés", planningsSaved));
            result.put("ordersProcessed", orders.size());
            result.put("employeesUsed", Math.min(employees.size(), workloads.size()));
            result.put("planningsSaved", planningsSaved);
            result.put("totalCards", totalCards);
            result.put("totalMinutes", totalMinutes);
            result.put("totalHours", String.format("%.1f", totalMinutes / 60.0));
            result.put("createdPlannings", createdPlannings);
            result.put("timePerCardMinutes", timePerCard);
            result.put("startDate", startDate.toString());

            log.info("🎉 GÉNÉRATION TERMINÉE - {} plannings sauvés, {} cartes, {}h",
                    planningsSaved, totalCards, String.format("%.1f", totalMinutes / 60.0));

            return result;

        } catch (Exception e) {
            log.error("❌ Erreur génération planification: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Erreur: " + e.getMessage());
            return result;
        }
    }

    /**
     * 💾 SAUVEGARDE UNITAIRE CONSERVÉE (pour compatibilité)
     * Garde la même signature que l'ancienne méthode
     */
    @Transactional
    public boolean savePlanning(String orderId, String employeeId, LocalDate planningDate,
                                LocalDateTime startTime, int durationMinutes, String priority) {
        return savePlanningOptimized(
                UUID.randomUUID().toString().replace("-", ""),
                orderId, employeeId, planningDate, startTime,
                durationMinutes, priority, durationMinutes / MINUTES_PER_CARD);
    }

    // ========== MÉTHODES NOUVELLES ET OPTIMISÉES ==========

    /**
     * 🔍 CHARGEMENT DES COMMANDES PAR PRIORITÉ
     */
    private List<Map<String, Object>> loadOrdersByPriority(LocalDate fromDate) {
        try {
            String sql = """
                SELECT 
                    HEX(o.id) as id,
                    o.num_commande as numCommande,
                    o.nombre_cartes as nombreCartes,
                    o.priorite as priorite,
                    o.prix_total as prixTotal,
                    o.date_creation as dateCreation,
                    o.duree_estimee_minutes as dureeEstimee
                FROM `order` o 
                WHERE o.date_creation >= :fromDate 
                  AND o.statut = 1 
                ORDER BY 
                    CASE o.priorite 
                        WHEN 'URGENT' THEN 4
                        WHEN 'HIGH' THEN 3  
                        WHEN 'MEDIUM' THEN 2
                        WHEN 'LOW' THEN 1
                        ELSE 0
                    END DESC,
                    o.date_creation ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("fromDate", fromDate);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", (String) row[0]);
                order.put("numCommande", (String) row[1]);
                order.put("nombreCartes", row[2] != null ? ((Number) row[2]).intValue() : 1);
                order.put("priorite", (String) row[3]);
                order.put("prixTotal", row[4]);
                order.put("dateCreation", row[5]);
                order.put("dureeEstimee", row[6]);
                orders.add(order);
            }

            log.info("📦 Chargement de {} commandes depuis {}", orders.size(), fromDate);
            return orders;

        } catch (Exception e) {
            log.error("❌ Erreur chargement commandes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👥 CHARGEMENT DES EMPLOYÉS DISPONIBLES
     */
    private List<Map<String, Object>> loadAvailableEmployees() {
        try {
            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email as email,
                    e.work_hours_per_day as workHoursPerDay
                FROM j_employee e 
                WHERE e.active = 1 
                ORDER BY e.work_hours_per_day DESC
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", (String) row[0]);
                employee.put("firstName", (String) row[1]);
                employee.put("lastName", (String) row[2]);
                employee.put("email", (String) row[3]);
                employee.put("workHoursPerDay", row[4] != null ? ((Number) row[4]).intValue() : 8);
                employees.add(employee);
            }

            log.info("👥 Chargement de {} employés actifs", employees.size());
            return employees;

        } catch (Exception e) {
            log.error("❌ Erreur chargement employés: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 💾 SAUVEGARDE OPTIMISÉE
     */
    @Transactional
    public boolean savePlanningOptimized(
            String planningId,
            String orderId,
            String employeeId,
            LocalDate planningDate,
            LocalDateTime startTime,
            int durationMinutes,
            String delai,  // ✅ CHANGÉ: String au lieu de priorityString
            int cardCount
    ) {
        try {
            // ✅ NOUVELLE REQUÊTE SQL avec delai VARCHAR et status INT
            String insertSql = """
            INSERT INTO j_planning (
                id, order_id, employee_id, 
                planning_date, start_time, end_time,
                estimated_duration_minutes, estimated_end_time,
                delai, status, completed, card_count,
                progress_percentage, created_at, updated_at
            ) VALUES (
                UNHEX(?), UNHEX(?), UNHEX(?),
                ?, ?, ?,
                ?, ?,
                ?, ?, ?, ?,
                ?, NOW(), NOW()
            )
        """;

            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            Query query = entityManager.createNativeQuery(insertSql);
            query.setParameter(1, planningId);
            query.setParameter(2, orderId);
            query.setParameter(3, employeeId);
            query.setParameter(4, planningDate);
            query.setParameter(5, startTime);
            query.setParameter(6, endTime);
            query.setParameter(7, durationMinutes);
            query.setParameter(8, endTime);
            query.setParameter(9, delai);  // ✅ VARCHAR: 'X', 'F+', 'F', 'C', 'E'
            query.setParameter(10, 2);     // ✅ INT: 2 = A_NOTER (default)
            query.setParameter(11, false); // completed
            query.setParameter(12, cardCount);
            query.setParameter(13, 0);     // progress_percentage

            int rowsAffected = query.executeUpdate();

            if (rowsAffected > 0) {
                log.info("✅ Planning saved: orderId={}, employeeId={}, delai={}, status=2",
                        orderId, employeeId, delai);
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage(), e);
            return false;
        }
    }
    // ========== CLASSES UTILITAIRES ==========

    /**
     * 🏗️ INITIALISATION DES CHARGES DE TRAVAIL
     */
    private List<EmployeeWorkload> initializeWorkloads(List<Map<String, Object>> employees) {
        return employees.stream()
                .map(EmployeeWorkload::new)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 RECHERCHE DE L'EMPLOYÉ LE MOINS CHARGÉ
     */
    private EmployeeWorkload findLeastBusyEmployee(List<EmployeeWorkload> workloads) {
        return workloads.stream()
                .min(Comparator.comparing(EmployeeWorkload::getCurrentWorkloadMinutes))
                .orElseThrow(() -> new RuntimeException("Aucun employé disponible"));
    }

    /**
     * ⏰ CALCUL DU CRÉNEAU OPTIMAL
     */
    private LocalDateTime calculateOptimalStartTime(EmployeeWorkload workload, Map<String, Object> order) {
        LocalDateTime baseTime = DEFAULT_START_DATE.atTime(WORK_START_TIME);

        // Si l'employé a déjà du travail, programmer après
        if (workload.getLastEndTime() != null) {
            baseTime = workload.getLastEndTime().plusMinutes(15); // 15min de pause
        }

        // Pour les urgences : essayer de programmer plus tôt
        String priority = (String) order.get("priorite");
        if ("URGENT".equals(priority) && baseTime.getHour() > 10) {
            // Essayer de caler en début de matinée
            baseTime = baseTime.withHour(9).withMinute(0);
        }

        return baseTime;
    }

    // ========== CLASSE INTERNE POUR GESTION DES CHARGES ==========

    /**
     * 📈 CLASSE POUR SUIVRE LA CHARGE DE TRAVAIL PAR EMPLOYÉ
     */
    private static class EmployeeWorkload {
        private final Map<String, Object> employee;
        private int currentWorkloadMinutes = 0;
        private LocalDateTime lastEndTime;

        public EmployeeWorkload(Map<String, Object> employee) {
            this.employee = employee;
        }

        public void addWorkload(int durationMinutes, LocalDateTime startTime) {
            this.currentWorkloadMinutes += durationMinutes;
            this.lastEndTime = startTime.plusMinutes(durationMinutes);
        }

        public Map<String, Object> getEmployee() {
            return employee;
        }

        public int getCurrentWorkloadMinutes() {
            return currentWorkloadMinutes;
        }

        public LocalDateTime getLastEndTime() {
            return lastEndTime;
        }
    }

    /**
     * Nettoyer les planifications existantes pour éviter les doublons
     */
    private void cleanExistingPlannings(LocalDate fromDate) {
        try {
            String deleteSql = """
        DELETE FROM j_planning 
        WHERE planning_date >= ?
        AND created_at >= NOW() - INTERVAL 1 HOUR
        """;

            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            deleteQuery.setParameter(1, fromDate);

            int deletedCount = deleteQuery.executeUpdate();
            log.info("🗑️ Cleaned {} recent plannings from date {}", deletedCount, fromDate);

        } catch (Exception e) {
            log.warn("Error cleaning existing plannings: {}", e.getMessage());
        }
    }

    /**
     * Vérifier si une commande a déjà une planification
     */
    private boolean isPlanningExistsForOrder(String orderId) {
        try {
            String checkSql = """
        SELECT COUNT(*) FROM j_planning 
        WHERE order_id = UNHEX(?)
        """;

            Query checkQuery = entityManager.createNativeQuery(checkSql);
            checkQuery.setParameter(1, orderId.replace("-", ""));

            Number count = (Number) checkQuery.getSingleResult();
            return count.intValue() > 0;

        } catch (Exception e) {
            log.warn("Error checking planning existence for order {}: {}", orderId, e.getMessage());
            return false;
        }
    }
}