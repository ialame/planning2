package com.pcagrade.order.service;

import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing teams (roles)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Get all teams
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    /**
     * Get all active teams with pagination
     */
    public Page<Team> getActiveTeams(Pageable pageable) {
        return teamRepository.findByActiveTrue(pageable);
    }

    /**
     * Get team by ID
     */
    public Team getTeamById(UUID teamId) {
        return teamRepository.findById(teamId.toString())
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));
    }

    /**
     * Get team by name
     */
    public Team getTeamByName(String name) {
        return teamRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Team not found with name: " + name));
    }

    /**
     * Search teams by keyword
     */
    public List<Team> searchTeams(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return teamRepository.findAll();
        }
        return teamRepository.searchByNameOrDescription(searchTerm);
    }

    /**
     * Create a new team
     */
    public Team createTeam(Team team) {
        if (teamRepository.existsByName(team.getName())) {
            throw new RuntimeException("Team with name " + team.getName() + " already exists");
        }
        return teamRepository.save(team);
    }

    /**
     * Update a team
     */
    public Team updateTeam(UUID teamId, Team updatedTeam) {
        Team team = teamRepository.findById(teamId.toString())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        team.setDescription(updatedTeam.getDescription());
        team.setDisplayName(updatedTeam.getDisplayName());
        team.setColor(updatedTeam.getColor());
        team.setIcon(updatedTeam.getIcon());
        team.setActive(updatedTeam.getActive());

        return teamRepository.save(team);
    }

    /**
     * Delete a team
     */
    public void deleteTeam(UUID teamId) {
        if (!teamRepository.existsById(teamId.toString())) {
            throw new RuntimeException("Team not found");
        }
        teamRepository.deleteById(teamId.toString());
    }

    /**
     * Add employee to team
     */
    public void addEmployeeToTeam(UUID teamId, UUID employeeId) {
        Team team = getTeamById(teamId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.addTeam(team);
        employeeRepository.save(employee);
    }

    /**
     * Add employee to team (String version)
     */
    public void addEmployeeToTeam(String teamId, String employeeId) {
        addEmployeeToTeam(UUID.fromString(teamId), UUID.fromString(employeeId));
    }

    /**
     * Remove employee from team
     */
    public void removeEmployeeFromTeam(UUID teamId, UUID employeeId) {
        Team team = getTeamById(teamId);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.removeTeam(team);
        employeeRepository.save(employee);
    }

    /**
     * Remove employee from team (String version)
     */
    public void removeEmployeeFromTeam(String teamId, String employeeId) {
        removeEmployeeFromTeam(UUID.fromString(teamId), UUID.fromString(employeeId));
    }

    /**
     * Get teams for a specific employee
     */
    public List<Team> getTeamsByEmployee(Employee employee) {
        return teamRepository.findByEmployeesContaining(employee);
    }

    /**
     * Get team statistics
     */
    public List<Object[]> getTeamStatistics() {
        return teamRepository.getTeamStatistics();
    }

    /**
     * Get teams with no members
     */
    public List<Team> getEmptyTeams() {
        return teamRepository.findTeamsWithNoMembers();
    }

    /**
     * Get total capacity for a team
     */
    public Integer getTeamCapacity(String teamName) {
        return teamRepository.getTotalTeamCapacity(teamName);
    }

    /**
     * Get all active teams (non-paginated)
     */
    public List<Team> getAllActiveTeams() {
        return teamRepository.findByActiveTrue();
    }

    /**
     * Get all active teams with pagination
     */
    public Page<Team> getAllActiveTeamsPaginated(Pageable pageable) {
        return teamRepository.findByActiveTrue(pageable);
    }

    /**
     * Search teams with pagination
     */
    public Page<Team> searchTeamsPaginated(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return teamRepository.findAll(pageable);
        }
        return teamRepository.searchByNameOrDescription(searchTerm, pageable);
    }

    /**
     * Count active employees in a team
     */
    public long countActiveEmployeesInTeam(UUID teamId) {
        Team team = getTeamById(teamId);
        return team.getActiveEmployeeCount();
    }

    /**
     * Assign employee to team (alias for addEmployeeToTeam)
     */
    public void assignEmployeeToTeam(UUID teamId, UUID employeeId) {
        addEmployeeToTeam(teamId, employeeId);
    }

    /**
     * Update employee teams - replace all teams for an employee
     */
    public void updateEmployeeTeams(UUID employeeId, List<String> teamNames) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Clear existing teams
        employee.getTeams().clear();

        // Add new teams
        for (String teamName : teamNames) {
            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));
            employee.addTeam(team);
        }

        employeeRepository.save(employee);
    }

    /**
     * Initialize default teams (roles)
     */
    public void initializeDefaultTeams() {
        List<String> defaultTeams = List.of(
                "ROLE_ADMIN",
                "ROLE_MANAGER",
                "ROLE_GRADER",
                "ROLE_AUTHENTICATOR",
                "ROLE_SCANNER",
                "ROLE_PREPARER",
                "ROLE_VIEWER"
        );

        for (String teamName : defaultTeams) {
            if (!teamRepository.existsByName(teamName)) {
                Team team = Team.builder()
                        .name(teamName)
                        .displayName(teamName.replace("ROLE_", ""))
                        .active(true)
                        .build();
                teamRepository.save(team);
                log.info("Created default team: {}", teamName);
            }
        }
    }

    /**
     * Get all employees in a team
     */
    public List<Employee> getTeamEmployees(UUID teamId) {
        Team team = getTeamById(teamId);
        return team.getEmployees().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .toList();
    }

    /**
     * Get teams by employee ID (convenience method)
     */
    public List<Team> getTeamsByEmployeeId(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return getTeamsByEmployee(employee);
    }
}