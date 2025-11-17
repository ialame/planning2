package com.pcagrade.order.config;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class TeamDataInitializer implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ AJOUTER @Lazy DANS LE CONSTRUCTEUR
    public TeamDataInitializer(
            TeamRepository teamRepository,
            EmployeeRepository employeeRepository,
            @Lazy PasswordEncoder passwordEncoder) {
        this.teamRepository = teamRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initializeTeams();
        initializeEmployees();
    }
    private void initializeTeams() {
        if (teamRepository.count() > 0) {
            log.info("✅ Teams already initialized");
            return;
        }

        log.info("🔧 Initializing teams...");

        List<Team> teams = List.of(
                createTeam("ROLE_GRADER", "Grader", "Grades Pokemon cards", "#3b82f6", "⭐"),
                createTeam("ROLE_CERTIFIER", "Certifier", "Certifies graded cards", "#10b981", "✓"),
                createTeam("ROLE_SCANNER", "Scanner", "Scans certified cards", "#f59e0b", "📷"),
                createTeam("ROLE_PREPARER", "Preparer", "Prepares orders for shipping", "#8b5cf6", "📦"),
                createTeam("ROLE_MANAGER", "Manager", "Manages team and operations", "#ef4444", "👔"),
                createTeam("ROLE_ADMIN", "Admin", "System administrator", "#6b7280", "⚙️")
        );

        teamRepository.saveAll(teams);
        log.info("✅ {} teams initialized", teams.size());
    }

    private Team createTeam(String name, String displayName, String description, String color, String icon) {
        Team team = new Team();
        team.setName(name);
        team.setDisplayName(displayName);
        team.setDescription(description);
        team.setColor(color);
        team.setIcon(icon);
        return team;
    }

    private void initializeEmployees() {
        if (employeeRepository.count() > 0) {
            log.info("✅ Employees already initialized");
            return;
        }

        log.info("🔧 Initializing sample employees...");

        Team graderTeam = teamRepository.findByName("ROLE_GRADER").orElse(null);
        Team certifierTeam = teamRepository.findByName("ROLE_CERTIFIER").orElse(null);
        Team scannerTeam = teamRepository.findByName("ROLE_SCANNER").orElse(null);  // ✅ Ajouter
        Team preparerTeam = teamRepository.findByName("ROLE_PREPARER").orElse(null); // ✅ Ajouter
        Team managerTeam = teamRepository.findByName("ROLE_MANAGER").orElse(null);
        Team adminTeam = teamRepository.findByName("ROLE_ADMIN").orElse(null);

        List<Employee> employees = List.of(
                createEmployee("john.grader@pcagrade.com", "John", "Smith", "password123", List.of(graderTeam)),
                createEmployee("alice.grader@pcagrade.com", "Alice", "Johnson", "password123", List.of(graderTeam)),
                createEmployee("bob.certifier@pcagrade.com", "Bob", "Wilson", "password123", List.of(certifierTeam)),
                createEmployee("jane.certifier@pcagrade.com", "Jane", "Brown", "password123", List.of(certifierTeam)),

                // ✅ AJOUTER CES LIGNES
                createEmployee("sarah.scanner@pcagrade.com", "Sarah", "Scanner", "password123", List.of(scannerTeam)),
                createEmployee("mike.scanner@pcagrade.com", "Mike", "Scanner", "password123", List.of(scannerTeam)),
                createEmployee("paul.preparer@pcagrade.com", "Paul", "Preparer", "password123", List.of(preparerTeam)),
                createEmployee("lisa.preparer@pcagrade.com", "Lisa", "Preparer", "password123", List.of(preparerTeam)),

                createEmployee("manager@pcagrade.com", "Manager", "User", "password123", List.of(managerTeam)),
                createEmployee("admin@pcagrade.com", "Admin", "User", "password123", List.of(adminTeam))
        );

        employeeRepository.saveAll(employees);
        log.info("✅ {} sample employees initialized", employees.size());
    }

    private Employee createEmployee(String email, String firstName, String lastName, String password, List<Team> teams) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setActive(true);
        employee.setWorkHoursPerDay(8);
        employee.setEfficiencyRating(1.0);

        // ✅ CONVERTIR List en Set
        employee.setTeams(new HashSet<>(teams));  // Au lieu de employee.setTeams(teams)

        return employee;
    }
}