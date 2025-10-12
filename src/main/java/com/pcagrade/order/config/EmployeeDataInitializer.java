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

/**
 * Employee Data Initializer
 *
 * Automatically creates 5 test employees when the application starts
 * for the first time. These employees can be used for testing the
 * planning features.
 *
 * Runs AFTER GroupDataInitializer (Order = 2)
 */

@Component
@Order(2) // Execute after GroupDataInitializer (Order = 1)
public class EmployeeDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("🔍 Checking if j_employee table needs initialization...");

            Long count = (Long) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM j_employee"
            ).getSingleResult();

            if (count > 0) {
                log.info("✅ j_employee table already contains {} employees - skipping initialization", count);
                return;
            }

            log.info("📝 j_employee table is empty - creating 5 test employees...");
            createTestEmployees();
            log.info("✅ Successfully created 5 test employees");

        } catch (Exception e) {
            log.error("❌ Error initializing test employees: {}", e.getMessage(), e);
        }
    }

    /**
     * Create 5 test employees with different profiles
     */
    private void createTestEmployees() {
        // Employee 1: Full-time grader (8h/day, high efficiency)
        createEmployee(
                "Sophie",
                "Martin",
                "sophie.martin@pcagrade.com",
                8.0,
                1.2,
                "Full-time card grader with high efficiency"
        );

        // Employee 2: Part-time certifier (6h/day, standard efficiency)
        createEmployee(
                "Thomas",
                "Dubois",
                "thomas.dubois@pcagrade.com",
                6.0,
                1.0,
                "Part-time certifier, standard productivity"
        );

        // Employee 3: Full-time scanner (8h/day, very efficient)
        createEmployee(
                "Marie",
                "Bernard",
                "marie.bernard@pcagrade.com",
                8.0,
                1.3,
                "Highly efficient full-time scanner"
        );

        // Employee 4: Senior manager (7h/day, coordinator)
        createEmployee(
                "Pierre",
                "Petit",
                "pierre.petit@pcagrade.com",
                7.0,
                1.0,
                "Senior manager and coordinator"
        );

        // Employee 5: New trainee (5h/day, learning)
        createEmployee(
                "Julie",
                "Moreau",
                "julie.moreau@pcagrade.com",
                5.0,
                0.8,
                "New trainee, still learning"
        );
    }

    /**
     * Create a single employee
     */
    private void createEmployee(
            String firstName,
            String lastName,
            String email,
            Double workHoursPerDay,
            Double efficiencyRating,
            String notes) {

        try {
            String sql = """
                INSERT INTO j_employee 
                    (id, first_name, last_name, email, work_hours_per_day, efficiency_rating, active, creation_date, modification_date)
                VALUES 
                    (UNHEX(REPLACE(UUID(), '-', '')), ?, ?, ?, ?, ?, 1, NOW(), NOW())
                """;

            int result = entityManager.createNativeQuery(sql)
                    .setParameter(1, firstName)
                    .setParameter(2, lastName)
                    .setParameter(3, email)
                    .setParameter(4, workHoursPerDay)
                    .setParameter(5, efficiencyRating)
                    .executeUpdate();

            if (result > 0) {
                log.info("  ✓ Created employee: {} {} ({} - {}h/day, efficiency: {})",
                        firstName, lastName, email, workHoursPerDay, efficiencyRating);
            }

        } catch (Exception e) {
            log.error("  ✗ Failed to create employee {} {}: {}",
                    firstName, lastName, e.getMessage());
        }
    }
}