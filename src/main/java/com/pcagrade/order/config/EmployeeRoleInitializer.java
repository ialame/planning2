package com.pcagrade.order.config;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Initializes teams (roles) and employees with photos on application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("disabled")
public class EmployeeRoleInitializer implements ApplicationRunner {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting initialization of teams and employees...");

        // Step 1: Initialize Teams (Roles)
        initializeTeams();

        // Step 2: Initialize Employees with photos
        initializeEmployees();

        log.info("✅ Initialization complete!");
    }

    /**
     * Initialize all teams (roles) with metadata
     */
    private void initializeTeams() {
        if (teamRepository.count() > 0) {
            log.info("Teams already initialized, skipping...");
            return;
        }

        log.info("Creating teams (roles)...");

        List<Team> teams = List.of(
                createTeam("ROLE_ADMIN", "System Administrator", "Administrator", "#DC2626", "👑"),
                createTeam("ROLE_MANAGER", "Team Manager", "Manager", "#7C3AED", "📊"),
                createTeam("ROLE_GRADER", "Card Grader", "Grader", "#2563EB", "⭐"),
                createTeam("ROLE_AUTHENTICATOR", "Card Authenticator", "Authenticator", "#16A34A", "✓"),
                createTeam("ROLE_SCANNER", "Card Scanner", "Scanner", "#EA580C", "📷"),
                createTeam("ROLE_PREPARER", "Order Preparer", "Preparer", "#CA8A04", "📦"),
                createTeam("ROLE_VIEWER", "Read-Only Viewer", "Viewer", "#6B7280", "👁")
        );

        teamRepository.saveAll(teams);
        log.info("✅ Created {} teams", teams.size());
    }

    /**
     * Initialize employees with roles and photos
     */
    private void initializeEmployees() {
        if (employeeRepository.count() > 0) {
            log.info("Employees already initialized, skipping...");
            return;
        }

        log.info("Creating employees with roles and photos...");

        // Get all teams as a map for easy lookup
        Map<String, Team> teamMap = teamRepository.findAll().stream()
                .collect(Collectors.toMap(Team::getName, Function.identity()));

        // Create employees with different role combinations
        List<Employee> employees = List.of(
                createEmployee(
                        "John", "Doe", "john.doe@pcagrade.com",
                        List.of(teamMap.get("ROLE_GRADER"), teamMap.get("ROLE_SCANNER")),
                        480, 1.0
                ),

                createEmployee(
                        "Jane", "Smith", "jane.smith@pcagrade.com",
                        List.of(teamMap.get("ROLE_AUTHENTICATOR")),
                        480, 1.1
                ),

                createEmployee(
                        "Bob", "Johnson", "bob.johnson@pcagrade.com",
                        List.of(teamMap.get("ROLE_SCANNER"), teamMap.get("ROLE_PREPARER")),
                        420, 0.9
                ),

                createEmployee(
                        "Alice", "Williams", "alice.williams@pcagrade.com",
                        List.of(teamMap.get("ROLE_GRADER"), teamMap.get("ROLE_AUTHENTICATOR")),
                        480, 1.05
                ),

                createEmployee(
                        "Charlie", "Brown", "charlie.brown@pcagrade.com",
                        List.of(teamMap.get("ROLE_PREPARER")),
                        360, 1.0
                ),

                createEmployee(
                        "Diana", "Martinez", "diana.martinez@pcagrade.com",
                        List.of(teamMap.get("ROLE_ADMIN"), teamMap.get("ROLE_MANAGER")),
                        480, 1.0
                ),

                createEmployee(
                        "Eve", "Taylor", "eve.taylor@pcagrade.com",
                        List.of(teamMap.get("ROLE_VIEWER")),
                        480, 1.0
                )
        );

        employeeRepository.saveAll(employees);

        log.info("✅ Successfully initialized {} employees with roles and photos", employees.size());

        // Log role distribution
        logRoleDistribution();
    }

    /**
     * Helper method to create a team
     */
    private Team createTeam(String name, String description, String displayName, String color, String icon) {
        return Team.builder()
                .name(name)
                .description(description)
                .displayName(displayName)
                .color(color)
                .icon(icon)
                .active(true)
                .build();
    }

    /**
     * Helper method to create an employee with teams and auto-generated photo
     */
    private Employee createEmployee(
            String firstName,
            String lastName,
            String email,
            List<Team> teams,
            int dailyCapacityMinutes,
            double efficiencyRating
    ) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);

        // Auto-generate photo URL with UI Avatars
        String photoUrl = String.format(
                "https://ui-avatars.com/api/?name=%s+%s&size=150&background=random&color=fff&bold=true",
                firstName, lastName
        );
        employee.setPhotoUrl(photoUrl);

        employee.setTeams(teams != null ? teams.stream().collect(Collectors.toSet()) : null);
        employee.setActive(true);
        employee.setDailyCapacityMinutes(dailyCapacityMinutes);
        employee.setEfficiencyRating(efficiencyRating);

        return employee;
    }

    /**
     * Log role distribution for verification
     */
    private void logRoleDistribution() {
        log.info("Team/Role distribution:");

        List<Team> teams = teamRepository.findAll();
        for (Team team : teams) {
            long count = employeeRepository.findAll().stream()
                    .filter(emp -> emp.hasRole(team.getName()))
                    .count();

            if (count > 0) {
                log.info("  {} {} - {} employees",
                        team.getIcon(), team.getDisplayName(), count);
            }
        }
    }
}