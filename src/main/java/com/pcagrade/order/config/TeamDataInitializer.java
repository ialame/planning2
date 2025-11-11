package com.pcagrade.order.config;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Initialize database with required teams and sample users on application startup
 * Only runs in development mode when database is empty
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TeamDataInitializer {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void initializeData() {
        log.info("🔍 Checking if team table needs initialization...");

        long teamCount = teamRepository.count();

        if (teamCount == 0) {
            log.info("📝 Initializing teams...");
            createTeams();
            log.info("✅ Teams created successfully");
        } else {
            log.info("✅ Teams already initialized (count: {})", teamCount);
        }

        // Check if we need to add missing teams (MANAGER and ADMIN)
        ensureManagerAndAdminTeams();

        // Create sample employees if database is empty
        long employeeCount = employeeRepository.count();
        if (employeeCount == 0) {
            log.info("📝 Creating sample employees...");
            createSampleEmployees();
            log.info("✅ Sample employees created successfully");
        } else {
            log.info("✅ Employees already initialized (count: {})", employeeCount);
        }

        log.info("✅ Data initialization completed successfully");
    }

    private void createTeams() {
        List<Team> teams = List.of(
                createTeam("ROLE_GRADER", "Grading Team",
                        "Team responsible for grading Pokemon cards",
                        "#3B82F6", "⭐"),

                createTeam("ROLE_CERTIFIER", "Certification Team",
                        "Team responsible for certifying graded cards",
                        "#10B981", "✓"),

                createTeam("ROLE_SCANNER", "Scanning Team",
                        "Team responsible for scanning cards",
                        "#F59E0B", "📷"),

                createTeam("ROLE_PREPARER", "Preparation Team",
                        "Team responsible for preparing orders for shipment",
                        "#8B5CF6", "📦"),

                createTeam("ROLE_MANAGER", "Management Team",
                        "Team with management and planning permissions",
                        "#DC2626", "👔"),

                createTeam("ROLE_ADMIN", "Admin Team",
                        "Team with full administrative permissions",
                        "#991B1B", "🔐")
        );

        teamRepository.saveAll(teams);
    }

    private void ensureManagerAndAdminTeams() {
        // Check if MANAGER team exists
        if (!teamRepository.findByName("ROLE_MANAGER").isPresent()) {
            log.info("📝 Creating missing ROLE_MANAGER team...");
            Team managerTeam = createTeam("ROLE_MANAGER", "Management Team",
                    "Team with management and planning permissions",
                    "#DC2626", "MGR");
            teamRepository.save(managerTeam);
            log.info("✅ ROLE_MANAGER team created");
        }

        // Check if ADMIN team exists
        if (!teamRepository.findByName("ROLE_ADMIN").isPresent()) {
            log.info("📝 Creating missing ROLE_ADMIN team...");
            Team adminTeam = createTeam("ROLE_ADMIN", "Admin Team",
                    "Team with full administrative permissions",
                    "#991B1B", "ADM");
            teamRepository.save(adminTeam);
            log.info("✅ ROLE_ADMIN team created");
        }
    }

    private Team createTeam(String name, String displayName, String description,
                            String color, String icon) {
        Team team = new Team();
        team.setName(name);
        team.setDisplayName(displayName);
        team.setDescription(description);
        team.setColor(color);
        team.setIcon(icon);
        team.setActive(true);
        return team;
    }

    private void createSampleEmployees() {
        // Get teams
        Team graderTeam = teamRepository.findByName("ROLE_GRADER")
                .orElseThrow(() -> new RuntimeException("ROLE_GRADER not found"));
        Team certifierTeam = teamRepository.findByName("ROLE_CERTIFIER")
                .orElseThrow(() -> new RuntimeException("ROLE_CERTIFIER not found"));
        Team managerTeam = teamRepository.findByName("ROLE_MANAGER")
                .orElseThrow(() -> new RuntimeException("ROLE_MANAGER not found"));
        Team adminTeam = teamRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));

        // Password: password123
        String encodedPassword = passwordEncoder.encode("password123");

        List<Employee> employees = List.of(
                // Graders
                createEmployee("john.grader@pcagrade.com", "John", "Smith",
                        encodedPassword, Set.of(graderTeam)),

                createEmployee("alice.grader@pcagrade.com", "Alice", "Johnson",
                        encodedPassword, Set.of(graderTeam)),

                // Certifiers
                createEmployee("bob.certifier@pcagrade.com", "Bob", "Wilson",
                        encodedPassword, Set.of(certifierTeam)),

                createEmployee("jane.certifier@pcagrade.com", "Jane", "Brown",
                        encodedPassword, Set.of(certifierTeam)),

                // Manager (has both GRADER and MANAGER roles)
                createEmployee("manager@pcagrade.com", "Manager", "User",
                        encodedPassword, Set.of(managerTeam)),

                // Admin (has ADMIN role)
                createEmployee("admin@pcagrade.com", "Admin", "User",
                        encodedPassword, Set.of(adminTeam))
        );

        employeeRepository.saveAll(employees);

        log.info("✅ Created {} sample employees", employees.size());
        log.info("📝 Sample credentials:");
        log.info("   - john.grader@pcagrade.com / password123 (GRADER)");
        log.info("   - manager@pcagrade.com / password123 (MANAGER)");
        log.info("   - admin@pcagrade.com / password123 (ADMIN)");
    }

    private Employee createEmployee(String email, String firstName, String lastName,
                                    String password, Set<Team> teams) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setPassword(password);
        employee.setActive(true);
        employee.setEfficiencyRating(1.0);
        employee.setTeams(teams);
        return employee;
    }
}