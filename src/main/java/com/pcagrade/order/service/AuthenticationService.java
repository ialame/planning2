package com.pcagrade.order.service;

import com.pcagrade.order.dto.AuthenticationRequest;
import com.pcagrade.order.dto.AuthenticationResponse;
import com.pcagrade.order.dto.RegisterRequest;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Authentication Service
 * Handles user registration, login, and JWT token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Register a new employee
     */
    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        // Check if email already exists
        if (employeeRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        // Create new employee
        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set work hours per day (Integer)
        if (request.getHoursPerDay() != null) {
            employee.setWorkHoursPerDay(request.getHoursPerDay().intValue());
        } else {
            employee.setWorkHoursPerDay(8); // Default: 8 hours
        }

        // Set efficiency rating (Double)
        if (request.getEfficiency() != null) {
            employee.setEfficiencyRating(request.getEfficiency());
        } else {
            employee.setEfficiencyRating(1.0); // Default: 1.0 (normal speed)
        }

        // Set active status (Boolean)
        employee.setActive(true);

        // Assign roles (teams)
        Set<Team> teams = new HashSet<>();
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            for (String roleName : request.getRoles()) {
                Team team = teamRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
                teams.add(team);
            }
        } else {
            // Default role: VIEWER
            Team viewerRole = teamRepository.findByName("ROLE_VIEWER")
                    .orElseThrow(() -> new IllegalStateException("Default VIEWER role not found"));
            teams.add(viewerRole);
        }
        employee.setTeams(teams);

        // Save employee
        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Successfully registered new employee: {} with roles: {}",
                savedEmployee.getEmail(),
                teams.stream().map(Team::getName).toList());

        // Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedEmployee.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .email(savedEmployee.getEmail())
                .firstName(savedEmployee.getFirstName())
                .lastName(savedEmployee.getLastName())
                .roles(teams.stream().map(Team::getName).toList())
                .build();
    }

    /**
     * Authenticate employee and generate tokens
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Load user details
        Employee employee = employeeRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found"));

        // Check if employee is active using getActive() method
        if (!employee.getActive()) {
            throw new IllegalStateException("Employee account is deactivated");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        // Generate tokens
        String jwtToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("Employee authenticated successfully: {}", employee.getEmail());

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .roles(employee.getTeams().stream().map(Team::getName).toList())
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional(readOnly = true)
    public AuthenticationResponse refreshToken(String refreshToken) {
        String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail == null) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new IllegalArgumentException("Refresh token is expired or invalid");
        }

        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found"));

        String newAccessToken = jwtService.generateToken(userDetails);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .email(employee.getEmail())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .roles(employee.getTeams().stream().map(Team::getName).toList())
                .build();
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            return jwtService.isTokenValid(token, userDetails);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}