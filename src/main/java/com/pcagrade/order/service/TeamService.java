package com.pcagrade.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // ADD THIS IMPORT
import org.springframework.stereotype.Service;

import com.pcagrade.order.entity.Team;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.TeamRepository;
import com.pcagrade.order.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Team Service - Business Logic Layer
 * Handles all business operations related to Teams and Roles
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;

    // ========== BASIC CRUD OPERATIONS ==========

    /**
     * Get all teams
     */
    public List<Team> getAllTeams() {
        log.debug("Getting all teams");
        return teamRepository.findAll();
    }

    /**
     * Get all active teams
     */
    public List<Team> getAllActiveTeams() {
        log.debug("Getting all active teams");
        return teamRepository.findByActiveTrue();
    }

    /**
     * Get all active teams (paginated)
     */
    public Page<Team> getAllActiveTeamsPaginated(Pageable pageable) {
        log.debug("Getting all active teams (paginated)");
        return teamRepository.findByActiveTrue(pageable);
    }

    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(UUID id) {
        log.debug("Getting team by ID: {}", id);
        return teamRepository.findById(id);
    }

    /**
     * Get team by name
     */
    public Optional<Team> getTeamByName(String name) {
        log.debug("Getting team by name: {}", name);
        return teamRepository.findByName(name);
    }

    /**
     * Search teams by name or description
     */
    public List<Team> searchTeams(String searchTerm) {
        log.debug("Searching teams with term: {}", searchTerm);
        return teamRepository.searchByNameOrDescription(searchTerm);
    }

    /**
     * Search teams by name or description (paginated)
     */
    public Page<Team> searchTeamsPaginated(String searchTerm, Pageable pageable) {
        log.debug("Searching teams (paginated) with term: {}", searchTerm);
        return teamRepository.searchByNameOrDescription(searchTerm, pageable);
    }

    /**
     * Create new team
     */
    @Transactional
    public Team createTeam(Team team) {
        log.info("Creating new team: {}", team.getName());

        validateTeam(team);

        if (teamNameExists(team.getName())) {
            throw new IllegalArgumentException("Team with name '" + team.getName() + "' already exists");
        }

        team.setActive(true);
        team.setCreationDate(LocalDateTime.now());
        team.setModificationDate(LocalDateTime.now());

        Team savedTeam = teamRepository.save(team);
        log.info("Team created successfully: {} (ID: {})", savedTeam.getName(), savedTeam.getId());

        return savedTeam;
    }

    /**
     * Update existing team
     */
    @Transactional
    public Team updateTeam(UUID id, Team team) {
        log.info("Updating team: {}", id);

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + id));

        validateTeam(team);

        // Check if new name conflicts with another team
        if (!existingTeam.getName().equals(team.getName()) && teamNameExists(team.getName())) {
            throw new IllegalArgumentException("Team with name '" + team.getName() + "' already exists");
        }

        existingTeam.setName(team.getName());
        existingTeam.setDescription(team.getDescription());
        existingTeam.setPermissionLevel(team.getPermissionLevel());
        existingTeam.setActive(team.getActive());
        existingTeam.setModificationDate(LocalDateTime.now());

        Team updatedTeam = teamRepository.save(existingTeam);
        log.info("Team updated successfully: {}", updatedTeam.getName());

        return updatedTeam;
    }

    /**
     * Delete team (soft delete)
     */
    @Transactional
    public void deleteTeam(UUID id) {
        log.info("Deleting team: {}", id);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Team not found: " + id));

        team.setActive(false);
        team.setModificationDate(LocalDateTime.now());

        teamRepository.save(team);
        log.info("Team deleted successfully: {}", id);
    }

    // ========== EMPLOYEE-TEAM RELATIONSHIP MANAGEMENT ==========

    /**
     * Get teams for a specific employee
     */
    public List<Team> getTeamsForEmployee(UUID employeeId) {
        log.debug("Getting teams for employee: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        return employee.getTeams().stream().toList();
    }

    /**
     * Assign employee to team
     */
    @Transactional
    public void assignEmployeeToTeam(UUID employeeId, UUID teamId) {
        log.info("Assigning employee {} to team {}", employeeId, teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        if (team.getEmployees().contains(employee)) {
            log.warn("Employee {} is already in team {}", employeeId, teamId);
            return;
        }

        team.getEmployees().add(employee);
        employee.getTeams().add(team);

        teamRepository.save(team);
        log.info("Employee {} assigned to team {} successfully", employeeId, teamId);
    }

    /**
     * Remove employee from team
     */
    @Transactional
    public void removeEmployeeFromTeam(UUID employeeId, UUID teamId) {
        log.info("Removing employee {} from team {}", employeeId, teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        team.getEmployees().remove(employee);
        employee.getTeams().remove(team);

        teamRepository.save(team);
        log.info("Employee {} removed from team {} successfully", employeeId, teamId);
    }

    /**
     * Update all teams for an employee (replace existing teams)
     */
    @Transactional
    public void updateEmployeeTeams(UUID employeeId, List<UUID> teamIds) {
        log.info("Updating teams for employee: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Remove employee from all current teams
        List<Team> currentTeams = employee.getTeams().stream().toList();
        for (Team team : currentTeams) {
            team.getEmployees().remove(employee);
        }
        employee.getTeams().clear();

        // Add employee to new teams
        for (UUID teamId : teamIds) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

            team.getEmployees().add(employee);
            employee.getTeams().add(team);
        }

        employeeRepository.save(employee);
        log.info("Updated {} teams for employee {}", teamIds.size(), employeeId);
    }

    /**
     * Get all employees in a team
     */
    public List<Employee> getTeamEmployees(UUID teamId) {
        log.debug("Getting employees for team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        return team.getEmployees().stream().toList();
    }

    /**
     * Count active employees in a team
     */
    public long countActiveEmployeesInTeam(UUID teamId) {
        log.debug("Counting active employees in team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        return team.getEmployees().stream()
                .filter(Employee::getActive)
                .count();
    }

    // ========== STATISTICS AND UTILITY METHODS ==========

    /**
     * Get team statistics (team name, member count, etc.)
     */
    public List<Object[]> getTeamStatistics() {
        log.debug("Getting team statistics");
        return teamRepository.getTeamStatistics();
    }

    /**
     * Get teams with no members
     */
    public List<Team> getEmptyTeams() {
        log.debug("Getting empty teams");
        return teamRepository.findTeamsWithNoMembers();
    }

    /**
     * Initialize default teams/roles
     */
    @Transactional
    public void initializeDefaultTeams() {
        log.info("Initializing default teams");

        String[][] defaultTeams = {
                {"ROLE_ADMIN", "System administrators", "10"},
                {"ROLE_MANAGER", "Team managers", "7"},
                {"ROLE_NOTEUR", "Card graders", "5"},
                {"ROLE_CERTIFICATEUR", "Card certifiers/encapsulators", "5"},
                {"ROLE_SCANNER", "Card scanners", "4"},
                {"ROLE_PREPARATEUR", "Order preparers", "4"},
                {"ROLE_VIEWER", "Read-only viewers", "2"}
        };

        for (String[] teamData : defaultTeams) {
            String name = teamData[0];

            if (teamRepository.findByName(name).isEmpty()) {
                Team team = new Team();
                team.setName(name);
                team.setDescription(teamData[1]);
                team.setPermissionLevel(Integer.parseInt(teamData[2]));
                team.setActive(true);
                team.setCreationDate(LocalDateTime.now());
                team.setModificationDate(LocalDateTime.now());

                teamRepository.save(team);
                log.info("Created default team: {}", name);
            }
        }

        log.info("Default teams initialization complete");
    }

    /**
     * Get team member count
     */
    public int getTeamMemberCount(UUID teamId) {
        log.debug("Getting member count for team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        return team.getEmployees() != null ? team.getEmployees().size() : 0;
    }

    /**
     * Check if team exists
     */
    public boolean teamExists(UUID id) {
        return teamRepository.existsById(id);
    }

    /**
     * Check if team name exists
     */
    public boolean teamNameExists(String name) {
        return teamRepository.findByName(name).isPresent();
    }

    /**
     * Validate team data
     */
    public void validateTeam(Team team) {
        if (team.getName() == null || team.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }

        if (team.getName().length() > 50) {
            throw new IllegalArgumentException("Team name cannot exceed 50 characters");
        }

        if (team.getPermissionLevel() == null || team.getPermissionLevel() < 0) {
            throw new IllegalArgumentException("Permission level must be a positive number");
        }
    }
}