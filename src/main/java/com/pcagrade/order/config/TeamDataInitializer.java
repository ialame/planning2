package com.pcagrade.order.config;

import com.pcagrade.order.entity.*;
import com.pcagrade.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Initialize default teams and sample data
 * Fixed version using "Team" instead of "Group"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeamDataInitializer implements ApplicationRunner {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🔍 Checking if team table needs initialization...");

        try {
            long teamCount = teamRepository.count();

            if (teamCount == 0) {
                log.info("📝 Initializing default teams...");
                initializeDefaultTeams();
                log.info("✅ Default teams initialized successfully");
            } else {
                log.info("✓ Teams already exist ({}), skipping initialization", teamCount);
            }

            long employeeCount = employeeRepository.count();
            if (employeeCount == 0) {
                log.info("📝 Initializing sample employees...");
                initializeSampleEmployees();
                log.info("✅ Sample employees initialized successfully");
            }

            long orderCount = orderRepository.count();
            if (orderCount == 0) {
                log.info("📝 Initializing sample orders...");
                initializeSampleOrders();
                log.info("✅ Sample orders initialized successfully");
            }

        } catch (Exception e) {
            log.error("❌ Error initializing data", e);
            throw new RuntimeException("Failed to initialize data", e);
        }
    }

    private void initializeDefaultTeams() {
        List<Team> defaultTeams = List.of(
                createTeam("Grading Team", "Team responsible for grading Pokemon cards", 1),
                createTeam("Certification Team", "Team responsible for certifying graded cards", 2),
                createTeam("Scanning Team", "Team responsible for scanning certified cards", 3),
                createTeam("Packaging Team", "Team responsible for packaging and shipping", 4)
        );

        teamRepository.saveAll(defaultTeams);
        log.info("Created {} default teams", defaultTeams.size());
    }

    private Team createTeam(String name, String description, int permissionLevel) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setPermissionLevel(permissionLevel);
        team.setActive(true);
        return team;
    }

    private void initializeSampleEmployees() {
        Team gradingTeam = teamRepository.findByName("Grading Team").orElse(null);
        Team certificationTeam = teamRepository.findByName("Certification Team").orElse(null);
        Team scanningTeam = teamRepository.findByName("Scanning Team").orElse(null);
        Team packagingTeam = teamRepository.findByName("Packaging Team").orElse(null);

        List<Employee> employees = List.of(
                createEmployee("John", "Smith", "john.smith@pcagrade.com",
                        Set.of("ROLE_GRADER"), Set.of(gradingTeam)),
                createEmployee("Sarah", "Johnson", "sarah.johnson@pcagrade.com",
                        Set.of("ROLE_GRADER"), Set.of(gradingTeam)),
                createEmployee("Michael", "Brown", "michael.brown@pcagrade.com",
                        Set.of("ROLE_CERTIFIER"), Set.of(certificationTeam)),
                createEmployee("Emily", "Davis", "emily.davis@pcagrade.com",
                        Set.of("ROLE_CERTIFIER"), Set.of(certificationTeam)),
                createEmployee("David", "Wilson", "david.wilson@pcagrade.com",
                        Set.of("ROLE_SCANNER"), Set.of(scanningTeam)),
                createEmployee("Jessica", "Martinez", "jessica.martinez@pcagrade.com",
                        Set.of("ROLE_SCANNER"), Set.of(scanningTeam)),
                createEmployee("James", "Garcia", "james.garcia@pcagrade.com",
                        Set.of("ROLE_PACKAGER"), Set.of(packagingTeam)),
                createEmployee("Lisa", "Rodriguez", "lisa.rodriguez@pcagrade.com",
                        Set.of("ROLE_PACKAGER"), Set.of(packagingTeam))
        );

        employeeRepository.saveAll(employees);
        log.info("Created {} sample employees", employees.size());
    }

    private Employee createEmployee(String firstName, String lastName, String email,
                                    Set<String> roles, Set<Team> teams) {
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setRoles(roles);
        employee.setTeams(teams != null ? teams : new HashSet<>());
        employee.setActive(true);
        employee.setDailyCapacityMinutes(480); // 8 hours
        return employee;
    }

    private void initializeSampleOrders() {
        List<Order> orders = List.of(
                createOrder("ORD-001", "Alice Williams", "alice@email.com",
                        LocalDateTime.now().plusDays(7), 15),
                createOrder("ORD-002", "Bob Thompson", "bob@email.com",
                        LocalDateTime.now().plusDays(5), 20),
                createOrder("ORD-003", "Carol Martinez", "carol@email.com",
                        LocalDateTime.now().plusDays(10), 10),
                createOrder("ORD-004", "Daniel Anderson", "daniel@email.com",
                        LocalDateTime.now().plusDays(3), 25),
                createOrder("ORD-005", "Eva Moore", "eva@email.com",
                        LocalDateTime.now().plusDays(14), 30)
        );

        orderRepository.saveAll(orders);
        log.info("Created {} sample orders", orders.size());
    }

    private Order createOrder(String orderNumber, String customerName, String customerEmail,
                              LocalDateTime deadline, int cardCount) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setDeliveryDeadline(deadline);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        // Create sample cards
        List<Card> cards = new java.util.ArrayList<>();
        for (int i = 1; i <= cardCount; i++) {
            Card card = new Card();
            card.setOrder(order);
            card.setCardName("Pikachu " + i);
            card.setCardSet("Base Set");
            card.setCardNumber(String.valueOf(25 + i));
            card.setStatus(CardStatus.PENDING);
            cards.add(card);
        }
        order.setCards(cards);

        // Calculate estimated processing time
        order.calculateEstimatedTime();

        return order;
    }
}