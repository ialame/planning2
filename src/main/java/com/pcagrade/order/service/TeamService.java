package com.pcagrade.order.service;

import com.pcagrade.order.entity.Team;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.TeamRepository;
import com.pcagrade.order.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Team Service - Business Logic Layer
 * Handles all business operations related to Teams
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;

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
    public List<Team> getActiveTeams() {
        log.debug("Getting active teams");
        return teamRepository.findByActiveTrue();
    }

    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(UUID id) {
        log.debug("Getting team by id: {}", id);
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
     * Search teams by name
     */
    public List<Team> searchTeamsByName(String searchTerm) {
        log.debug("Searching teams by name: {}", searchTerm);
        return teamRepository.searchByName(searchTerm);
    }

    /**
     * Create new team
     */
    @Transactional
    public Team createTeam(Team team) {
        log.info("Creating new team: {}", team.getName());

        // Validate unique name
        if (teamRepository.findByName(team.getName()).isPresent()) {
            log.warn("Team with name {} already exists", team.getName());
            throw new IllegalArgumentException("Team with name " + team.getName() + " already exists");
        }

        // Ensure team is active by default
        if (team.getActive() == null) {
            team.setActive(true);
        }

        Team savedTeam = teamRepository.save(team);
        log.info("Team created successfully with id: {}", savedTeam.getId());

        return savedTeam;
    }

    /**
     * Update existing team
     */
    @Transactional
    public Team updateTeam(UUID id, Team updatedTeam) {
        log.info("Updating team id: {}", id);

        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));

        // Check if name is being changed and if it's unique
        if (updatedTeam.getName() != null &&
                !updatedTeam.getName().equals(existingTeam.getName())) {
            if (teamRepository.findByName(updatedTeam.getName()).isPresent()) {
                log.warn("Team with name {} already exists", updatedTeam.getName());
                throw new IllegalArgumentException("Team with name " + updatedTeam.getName() + " already exists");
            }
            existingTeam.setName(updatedTeam.getName());
        }

        // Update other fields
        if (updatedTeam.getDescription() != null) {
            existingTeam.setDescription(updatedTeam.getDescription());
        }
        if (updatedTeam.getPermissionLevel() != null) {
            existingTeam.setPermissionLevel(updatedTeam.getPermissionLevel());
        }
        if (updatedTeam.getActive() != null) {
            existingTeam.setActive(updatedTeam.getActive());
        }

        Team savedTeam = teamRepository.save(existingTeam);
        log.info("Team updated successfully: {}", id);

        return savedTeam;
    }

    /**
     * Deactivate team (soft delete)
     */
    @Transactional
    public void deactivateTeam(UUID id) {
        log.info("Deactivating team id: {}", id);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));

        team.setActive(false);
        teamRepository.save(team);

        log.info("Team deactivated successfully: {}", id);
    }

    /**
     * Reactivate team
     */
    @Transactional
    public void reactivateTeam(UUID id) {
        log.info("Reactivating team id: {}", id);

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + id));

        team.setActive(true);
        teamRepository.save(team);

        log.info("Team reactivated successfully: {}", id);
    }

    /**
     * Delete team permanently
     */
    @Transactional
    public void deleteTeamPermanently(UUID id) {
        log.warn("Permanently deleting team id: {}", id);

        if (!teamRepository.existsById(id)) {
            throw new IllegalArgumentException("Team not found: " + id);
        }

        teamRepository.deleteById(id);
        log.info("Team deleted permanently: {}", id);
    }

    /**
     * Add employee to team
     */
    @Transactional
    public void addEmployeeToTeam(UUID teamId, UUID employeeId) {
        log.info("Adding employee {} to team {}", employeeId, teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        team.getEmployees().add(employee);
        employee.getTeams().add(team);

        teamRepository.save(team);

        log.info("Employee {} added to team {} successfully", employeeId, teamId);
    }

    /**
     * Remove employee from team
     */
    @Transactional
    public void removeEmployeeFromTeam(UUID teamId, UUID employeeId) {
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
     * Get all employees in a team
     */
    public List<Employee> getTeamEmployees(UUID teamId) {
        log.debug("Getting employees for team: {}", teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        return team.getEmployees().stream().toList();
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
     * Validate team
     */
    public void validateTeam(Team team) {
        if (team.getName() == null || team.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be empty");
        }

        if (team.getName().length() > 50) {
            throw new IllegalArgumentException("Team name cannot exceed 50 characters");
        }
    }
}