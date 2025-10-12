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

@Component
@Order(1)
public class GroupDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GroupDataInitializer.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("🔍 Checking if j_group table needs initialization...");

            Long count = (Long) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM j_group"
            ).getSingleResult();

            if (count > 0) {
                log.info("✅ j_group table already contains {} roles - skipping initialization", count);
                return;
            }

            log.info("📝 j_group table is empty - inserting default roles...");
            insertDefaultGroups();
            log.info("✅ Successfully initialized 7 default roles in j_group table");

        } catch (Exception e) {
            log.error("❌ Error initializing default groups: {}", e.getMessage(), e);
        }
    }

    private void insertDefaultGroups() {
        insertGroup("ROLE_ADMIN", "ROLE_ADMIN", "System administrators", 10);
        insertGroup("ROLE_MANAGER", "ROLE_MANAGER", "Team managers", 7);
        insertGroup("ROLE_NOTEUR", "ROLE_NOTEUR", "Card graders", 5);
        insertGroup("ROLE_CERTIFICATEUR", "ROLE_CERTIFICATEUR", "Card certifiers/encapsulators", 5);
        insertGroup("ROLE_SCANNER", "ROLE_SCANNER", "Card scanners", 4);
        insertGroup("ROLE_PREPARATEUR", "ROLE_PREPARATEUR", "Order preparers", 4);
        insertGroup("ROLE_VIEWER", "ROLE_VIEWER", "Read-only viewers", 2);
    }

    private void insertGroup(String name, String nameUpper, String description, int permissionLevel) {
        try {
            String sql = """
                INSERT INTO j_group 
                    (id, active, creation_date, description, modification_date, name, name_upper, permission_level)
                VALUES 
                    (UNHEX(REPLACE(UUID(), '-', '')), 1, NOW(), ?, NOW(), ?, ?, ?)
                """;

            int result = entityManager.createNativeQuery(sql)
                    .setParameter(1, description)
                    .setParameter(2, name)
                    .setParameter(3, nameUpper)
                    .setParameter(4, permissionLevel)
                    .executeUpdate();

            if (result > 0) {
                log.info("  ✓ Created role: {} (level {})", name, permissionLevel);
            }

        } catch (Exception e) {
            log.error("  ✗ Failed to create role {}: {}", name, e.getMessage());
        }
    }
}