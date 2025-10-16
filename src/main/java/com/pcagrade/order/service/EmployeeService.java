package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Employee Service - English Version
 * Handles all employee-related business logic
 * FIXED: Uses dailyCapacityMinutes instead of workHoursPerDay
 */
@Service
@Transactional
@Validated
@Slf4j
public class EmployeeService {

    private static final int DEFAULT_DAILY_CAPACITY_MINUTES = 480; // 8 hours * 60 minutes
    private static final int MAX_DAILY_CAPACITY_MINUTES = 720; // 12 hours * 60 minutes

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new employee
     */
    public Employee createEmployee(@Valid @NotNull Employee employee) {
        try {
            log.info("Creating new employee: {} {}", employee.getFirstName(), employee.getLastName());

            // Validate business rules
            validateNewEmployee(employee);

            // Set default values if not provided
            if (employee.getDailyCapacityMinutes() == null) {
                employee.setDailyCapacityMinutes(DEFAULT_DAILY_CAPACITY_MINUTES);
            }
            if (employee.getActive() == null) {
                employee.setActive(true);
            }
            if (employee.getCreationDate() == null) {
                employee.setCreationDate(LocalDateTime.now());
            }
            if (employee.getModificationDate() == null) {
                employee.setModificationDate(LocalDateTime.now());
            }

            Employee savedEmployee = employeeRepository.save(employee);
            log.info("Employee created successfully with ID: {}", savedEmployee.getId());
            return savedEmployee;

        } catch (Exception e) {
            log.error("Error creating employee", e);
            throw new RuntimeException("Error creating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing employee
     */
    public Employee updateEmployee(@Valid @NotNull Employee employee) {
        log.info("Updating employee: {}", employee.getId());

        if (employee.getId() == null) {
            throw new IllegalArgumentException("Employee ID cannot be null for update");
        }

        employee.setModificationDate(LocalDateTime.now());

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", updatedEmployee.getId());
        return updatedEmployee;
    }

    /**
     * Get employee by ID
     */
    public Optional<Employee> findById(String id) {
        try {
            log.debug("Finding employee by ID: {}", id);
            UUID uuid = UUID.fromString(id.length() == 32 ?
                    id.replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5") : id);
            return employeeRepository.findById(uuid);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid employee ID format: {}", id);
            return Optional.empty();
        }
    }

    /**
     * Get all active employees
     */
    public List<Map<String, Object>> getAllActiveEmployees() {
        try {
            log.debug("Getting all active employees");

            String sql = """
                SELECT 
                    HEX(e.id) as id,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.email,
                    COALESCE(e.active, 1) as active,
                    COALESCE(e.daily_capacity_minutes, 480) as dailyCapacityMinutes,
                    e.creation_date as creationDate
                FROM employee e
                WHERE COALESCE(e.active, 1) = 1
                ORDER BY e.first_name, e.last_name
                """;

            Query query = entityManager.createNativeQuery(sql);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            for (Object[] row : results) {
                try {
                    Map<String, Object> employee = new HashMap<>();

                    String id = (String) row[0];
                    String firstName = (String) row[1];
                    String lastName = (String) row[2];
                    Object emailObj = row[3];
                    Object activeObj = row[4];
                    Object capacityObj = row[5];
                    Object creationDateObj = row[6];

                    employee.put("id", id);
                    employee.put("firstName", firstName != null ? firstName : "Unknown");
                    employee.put("lastName", lastName != null ? lastName : "User");
                    employee.put("email", emailObj);
                    employee.put("dailyCapacityMinutes", capacityObj != null ?
                            ((Number) capacityObj).intValue() : 480);
                    // Convert to hours for display
                    employee.put("workHoursPerDay", capacityObj != null ?
                            ((Number) capacityObj).intValue() / 60 : 8);
                    employee.put("active", activeObj != null ?
                            ((Number) activeObj).intValue() == 1 : true);
                    employee.put("creationDate", creationDateObj);
                    employee.put("fullName", firstName + " " + lastName);

                    employees.add(employee);

                } catch (Exception e) {
                    log.error("Error processing employee row", e);
                }
            }

            log.debug("Successfully processed {} employees", employees.size());
            return employees;

        } catch (Exception e) {
            log.error("Error getting active employees", e);
            return new ArrayList<>();
        }
    }

    /**
     * Validate new employee business rules
     */
    private void validateNewEmployee(Employee employee) {
        if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (employee.getLastName() == null || employee.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
            if (!employee.getEmail().contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            if (existsByEmail(employee.getEmail())) {
                throw new IllegalArgumentException("Employee with this email already exists: " + employee.getEmail());
            }
        }

        if (employee.getDailyCapacityMinutes() != null &&
                (employee.getDailyCapacityMinutes() < 60 || employee.getDailyCapacityMinutes() > MAX_DAILY_CAPACITY_MINUTES)) {
            throw new IllegalArgumentException("Daily capacity must be between 60 and " + MAX_DAILY_CAPACITY_MINUTES + " minutes");
        }
    }

    /**
     * Check if employee exists by email
     */
    private boolean existsByEmail(String email) {
        try {
            return employeeRepository.findByEmail(email).isPresent();
        } catch (Exception e) {
            log.warn("Error checking email existence: {}", email, e);
            return false;
        }
    }

    /**
     * Create employee from Map data (for JSON endpoints)
     */
    public Employee createEmployeeFromMap(Map<String, Object> employeeData) {
        try {
            log.info("Creating employee from map data");

            // Extract required fields
            String firstName = (String) employeeData.get("firstName");
            String lastName = (String) employeeData.get("lastName");
            String email = (String) employeeData.get("email");

            if (firstName == null || firstName.trim().isEmpty() ||
                    lastName == null || lastName.trim().isEmpty()) {
                throw new IllegalArgumentException("First name and last name are required");
            }

            // Extract optional fields with defaults
            Integer dailyCapacityMinutes = DEFAULT_DAILY_CAPACITY_MINUTES;

            // Check for workHoursPerDay (convert to minutes)
            if (employeeData.containsKey("workHoursPerDay")) {
                Object workHoursObj = employeeData.get("workHoursPerDay");
                if (workHoursObj instanceof Number) {
                    int workHours = ((Number) workHoursObj).intValue();
                    dailyCapacityMinutes = workHours * 60;
                }
            }

            // Or check for dailyCapacityMinutes directly
            if (employeeData.containsKey("dailyCapacityMinutes")) {
                Object capacityObj = employeeData.get("dailyCapacityMinutes");
                if (capacityObj instanceof Number) {
                    dailyCapacityMinutes = ((Number) capacityObj).intValue();
                }
            }

            Boolean active = true;
            if (employeeData.containsKey("active")) {
                active = (Boolean) employeeData.get("active");
            }

            // Create Employee entity using constructor (no builder)
            Employee employee = new Employee();
            employee.setFirstName(firstName.trim());
            employee.setLastName(lastName.trim());
            employee.setEmail(email != null ? email.trim() : null);
            employee.setDailyCapacityMinutes(dailyCapacityMinutes);
            employee.setActive(active);
            employee.setCreationDate(LocalDateTime.now());
            employee.setModificationDate(LocalDateTime.now());

            // Save using the standard create method
            return createEmployee(employee);

        } catch (Exception e) {
            log.error("Error creating employee from map", e);
            throw new RuntimeException("Error creating employee: " + e.getMessage(), e);
        }
    }
}