package com.pcagrade.order.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Employee Role Initializer
 *
 * Automatically assigns 2-3 roles to each test employee to cover all 7 roles.
 * This ensures that the planning system has employees with the necessary
 * permissions to handle all types of tasks.
 *
 * Runs AFTER TeamDataInitializer (Order 1) and EmployeeDataInitializer (Order 2)
 */
@Component
@Order(3) // Execute after employees are created
public class EmployeeRoleInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRoleInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("🔍 Checking if employee roles need initialization...");

            // Check if any employee-taem assignments exist
            Long count = (Long) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM employee_team"
            ).getSingleResult();

            if (count > 0) {
                log.info("✅ employee_taem table already contains {} assignments - skipping initialization", count);
                return;
            }

            log.info("📝 Assigning roles to test employees...");
            assignRolesToEmployees();
            log.info("✅ Successfully assigned roles to all test employees");

        } catch (Exception e) {
            log.error("❌ Error assigning roles to employees: {}", e.getMessage(), e);
        }
    }

    /**
     * Assign roles to employees to cover all 7 roles
     *
     * Distribution:
     * - Sophie Martin: NOTEUR (grader), SCANNER, VIEWER
     * - Thomas Dubois: CERTIFICATEUR (certifier), PREPARATEUR (preparer)
     * - Marie Bernard: SCANNER, PREPARATEUR, VIEWER
     * - Pierre Petit: MANAGER, ADMIN
     * - Julie Moreau: NOTEUR, CERTIFICATEUR, VIEWER
     *
     * This ensures all 7 roles are covered across the 5 employees
     */
    private void assignRolesToEmployees() {
        // Get employees by email (predictable)
        Map<String, String> employees = getEmployeeIds();

        // Get teams by name
        Map<String, String> taems = getTeamIds();

        if (employees.isEmpty() || taems.isEmpty()) {
            log.warn("⚠️ No employees or teams found - skipping role assignment");
            return;
        }

        // Employee 1: Sophie Martin - Card grader with basic access
        assignRoles(employees.get("sophie.martin@pcagrade.com"), List.of(
                taems.get("ROLE_NOTEUR"),       // Main role: grading cards
                taems.get("ROLE_SCANNER"),      // Can also scan
                taems.get("ROLE_VIEWER")        // Read-only access
        ), "Sophie Martin");

        // Employee 2: Thomas Dubois - Certifier and preparer
        assignRoles(employees.get("thomas.dubois@pcagrade.com"), List.of(
                taems.get("ROLE_CERTIFICATEUR"), // Main role: certifying/encapsulating
                taems.get("ROLE_PREPARATEUR")    // Can prepare orders
        ), "Thomas Dubois");

        // Employee 3: Marie Bernard - Multi-skilled scanner and preparer
        assignRoles(employees.get("marie.bernard@pcagrade.com"), List.of(
                taems.get("ROLE_SCANNER"),      // Main role: scanning
                taems.get("ROLE_PREPARATEUR"),  // Can prepare orders
                taems.get("ROLE_VIEWER")        // Read-only access
        ), "Marie Bernard");

        // Employee 4: Pierre Petit - Senior manager with admin rights
        assignRoles(employees.get("pierre.petit@pcagrade.com"), List.of(
                taems.get("ROLE_MANAGER"),      // Team management
                taems.get("ROLE_ADMIN")         // Full system access
        ), "Pierre Petit");

        // Employee 5: Julie Moreau - Trainee learning multiple roles
        assignRoles(employees.get("julie.moreau@pcagrade.com"), List.of(
                taems.get("ROLE_NOTEUR"),       // Learning to grade
                taems.get("ROLE_CERTIFICATEUR"), // Learning to certify
                taems.get("ROLE_VIEWER")        // Read-only monitoring
        ), "Julie Moreau");

        log.info("✅ All 7 roles covered across 5 employees");
    }

    /**
     * Get employee IDs by email
     */
    private Map<String, String> getEmployeeIds() {
        String sql = "SELECT email, HEX(id) FROM employee";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],  // email
                        row -> (String) row[1]   // id (hex)
                ));
    }

    /**
     * Get taem IDs by name
     */
    private Map<String, String> getTeamIds() {
        String sql = "SELECT name, HEX(id) FROM taem";

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (String) row[0],  // name
                        row -> (String) row[1]   // id (hex)
                ));
    }

    /**
     * Assign multiple roles to an employee
     */
    private void assignRoles(String employeeId, List<String> taemIds, String employeeName) {
        if (employeeId == null || taemIds == null || taemIds.isEmpty()) {
            log.warn("⚠️ Cannot assign roles to {} - missing IDs", employeeName);
            return;
        }

        int successCount = 0;

        for (String taemId : taemIds) {
            if (taemId == null) continue;

            try {
                String sql = """
                    INSERT INTO employee_taem (employee_id, taem_id)
                    VALUES (UNHEX(?), UNHEX(?))
                    """;

                int result = entityManager.createNativeQuery(sql)
                        .setParameter(1, employeeId)
                        .setParameter(2, taemId)
                        .executeUpdate();

                if (result > 0) {
                    successCount++;
                }

            } catch (Exception e) {
                log.error("  ✗ Failed to assign role to {}: {}", employeeName, e.getMessage());
            }
        }

        if (successCount > 0) {
            log.info("  ✓ Assigned {} roles to {}", successCount, employeeName);
        }
    }
}