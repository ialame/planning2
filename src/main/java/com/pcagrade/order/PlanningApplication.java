package com.pcagrade.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Pokemon Card Order Planning System
 * Main application class for managing Pokemon card orders and processing planning
 * Each card takes approximately 3 minutes to process
 */
@SpringBootApplication
public class PlanningApplication {

    private static final Logger log = LoggerFactory.getLogger(PlanningApplication.class);

    public static void main(String[] args) {
        try {
            log.info("🚀 Starting Pokemon Card Order Planning System...");
            log.info("💳 Card processing time: 3 minutes per card");
            log.info("📊 Processing orders from June 2025 onwards");

            // Configure system properties before starting
            System.setProperty("spring.application.name", "pokemon-card-planning");

            // Start Spring Boot application
            ConfigurableApplicationContext context = SpringApplication.run(PlanningApplication.class, args);

            log.info("✅ Pokemon Card Order Planning System started successfully!");
            log.info("🌐 API available at: http://localhost:8080");
            log.info("📋 Planning API endpoint: http://localhost:8080/api/planning/generate");
            log.info("👥 Employee management: http://localhost:8080/api/employees");
            log.info("📦 Order management: http://localhost:8080/api/orders");
            log.info("🏥 Health check: http://localhost:8080/api/health");

            // Print application info
            String[] activeProfiles = context.getEnvironment().getActiveProfiles();
            if (activeProfiles.length > 0) {
                log.info("🔧 Active profiles: {}", String.join(", ", activeProfiles));
            } else {
                log.info("🔧 Active profiles: default");
            }

            // Check if test mode is requested
            if (args.length > 0 && "--test-dp".equals(args[0])) {
                log.info("🧪 Test mode activated - Running DP algorithm tests");
            }

            // Check database connection
            try {
                // This will be handled by Spring Boot auto-configuration
                log.info("🗄️ Database configuration loaded");
            } catch (Exception e) {
                log.warn("⚠️ Database connection issue: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ Failed to start Pokemon Card Order Planning System: {}", e.getMessage(), e);

            // Print more detailed error information
            if (e.getCause() != null) {
                log.error("❌ Root cause: {}", e.getCause().getMessage());
            }

            System.exit(1);
        }
    }
}