package com.pcagrade.order.config;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Initializes default teams and sample employees in the database.
 * Creates teams for each role type and assigns employees to teams.
 *
 * CORRECTED: Uses String IDs (ULID as String) as per your architecture
 * CORRECTED: Uses findByName() which already exists in your TeamRepository
 * CORRECTED: Uses Set<Team> for employee.teams (many-to-many relationship)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeamDataInitializer implements ApplicationRunner {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            log.info("🔍 Checking if team table needs initialization...");

            // Initialize teams if needed
            if (teamRepository.count() == 0) {
                initializeDefaultTeams();
            } else {
                log.info("✅ Teams already initialized (count: {})", teamRepository.count());
            }

            // Initialize sample employees if needed
            log.info("📝 Checking sample employees...");
            if (employeeRepository.count() == 0) {
                initializeSampleEmployees();
            } else {
                log.info("✅ Employees already initialized (count: {})", employeeRepository.count());
            }

            log.info("✅ Data initialization completed successfully");
        } catch (Exception e) {
            log.error("❌ Error initializing data", e);
            throw new RuntimeException("Failed to initialize data", e);
        }
    }

    /**
     * Creates default teams for each role type
     *
     * Note: Team.name is used as the role identifier (e.g., "ROLE_GRADER")
     */
    private void initializeDefaultTeams() {
        log.info("📝 Initializing default teams...");

        List<Team> teams = Arrays.asList(
                createTeam("ROLE_GRADER", "Team responsible for grading Pokemon cards", "Grading Team", "#3B82F6", "⭐"),
                createTeam("ROLE_CERTIFIER", "Team responsible for certifying graded cards", "Certification Team", "#10B981", "✓"),
                createTeam("ROLE_SCANNER", "Team responsible for scanning cards", "Scanning Team", "#F59E0B", "📷"),
                createTeam("ROLE_PREPARER", "Team responsible for preparing orders for shipment", "Preparation Team", "#8B5CF6", "📦")
        );

        teamRepository.saveAll(teams);
        log.info("Created {} default teams", teams.size());
        log.info("✅ Default teams initialized successfully");
    }

    /**
     * Creates sample employees for testing
     */
    private void initializeSampleEmployees() {
        log.info("📝 Initializing sample employees...");

        List<Employee> employees = Arrays.asList(
                // Graders
                createEmployee("john.grader", "John", "Smith", "john.grader@pcagrade.com", "ROLE_GRADER"),
                createEmployee("alice.grader", "Alice", "Johnson", "alice.grader@pcagrade.com", "ROLE_GRADER"),

                // Certifiers
                createEmployee("bob.certifier", "Bob", "Williams", "bob.certifier@pcagrade.com", "ROLE_CERTIFIER"),
                createEmployee("emma.certifier", "Emma", "Brown", "emma.certifier@pcagrade.com", "ROLE_CERTIFIER"),

                // Scanners
                createEmployee("charlie.scanner", "Charlie", "Davis", "charlie.scanner@pcagrade.com", "ROLE_SCANNER"),
                createEmployee("sophie.scanner", "Sophie", "Miller", "sophie.scanner@pcagrade.com", "ROLE_SCANNER"),

                // Preparers
                createEmployee("david.preparer", "David", "Wilson", "david.preparer@pcagrade.com", "ROLE_PREPARER"),
                createEmployee("lisa.preparer", "Lisa", "Moore", "lisa.preparer@pcagrade.com", "ROLE_PREPARER")
        );

        employeeRepository.saveAll(employees);
        log.info("✅ Created {} sample employees", employees.size());
    }

    /**
     * Creates a team with specified details
     */
    private Team createTeam(String name, String description, String displayName, String color, String icon) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setDisplayName(displayName);
        team.setColor(color);
        team.setIcon(icon);
        team.setActive(true);
        return team;
    }

    /**
     * Creates an employee and assigns them to the appropriate team based on role name
     *
     * CORRECTED: Uses findByName() which searches by team.name (the role identifier)
     * CORRECTED: Uses Set<Team> for many-to-many relationship
     */
    private Employee createEmployee(String username, String firstName, String lastName,
                                    String email, String roleName) {
        // Find team by name (which is the role identifier like "ROLE_GRADER")
        Team team = teamRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException(roleName + " team not found. Available teams: " +
                        teamRepository.findAll().stream()
                                .map(Team::getName)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("none")));

        Employee employee = new Employee();
        employee.setPassword(passwordEncoder.encode("password123")); // Default password
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setActive(true);
        employee.setWorkHoursPerDay(8); // 8 hours per day
        employee.setEfficiencyRating(1.0); // Normal efficiency

        // Set teams (many-to-many relationship)
        employee.setTeams(new HashSet<>());
        employee.getTeams().add(team);

        return employee;
    }
}