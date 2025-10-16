package com.pcagrade.order.controller;

import com.pcagrade.order.dto.TeamDto;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.TeamRepository;
import com.pcagrade.order.service.TeamMapperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Enhanced Team Controller
 * REST API for managing Teams (renamed from Groups to avoid SQL keyword conflict)
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EnhancedTeamController {

    private final TeamRepository teamRepository;
    private final TeamMapperService teamMapperService;

    /**
     * Get all teams
     * GET /api/teams
     */
    @GetMapping
    public ResponseEntity<List<TeamDto.Response>> getAllTeams() {
        log.info("Getting all teams");

        List<Team> teams = teamRepository.findAll();
        List<TeamDto.Response> response = teamMapperService.toResponseList(teams);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active teams
     * GET /api/teams/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<TeamDto.Response>> getActiveTeams() {
        log.info("Getting active teams");

        List<Team> teams = teamRepository.findByActiveTrue();
        List<TeamDto.Response> response = teamMapperService.toResponseList(teams);

        return ResponseEntity.ok(response);
    }

    /**
     * Get team by ID
     * GET /api/teams/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamDto.Response> getTeamById(@PathVariable UUID id) {
        log.info("Getting team by id: {}", id);

        return teamRepository.findById(id)
                .map(teamMapperService::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get team summary by ID
     * GET /api/teams/{id}/summary
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<TeamDto.Summary> getTeamSummary(@PathVariable UUID id) {
        log.info("Getting team summary for id: {}", id);

        return teamRepository.findById(id)
                .map(teamMapperService::toSummary)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get detailed team information by ID
     * GET /api/teams/{id}/detailed
     */
    @GetMapping("/{id}/detailed")
    public ResponseEntity<TeamDto.Detailed> getTeamDetailed(@PathVariable UUID id) {
        log.info("Getting detailed team info for id: {}", id);

        return teamRepository.findById(id)
                .map(teamMapperService::toDetailed)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search teams by name
     * GET /api/teams/search?name={name}
     */
    @GetMapping("/search")
    public ResponseEntity<List<TeamDto.Response>> searchTeamsByName(
            @RequestParam String name) {
        log.info("Searching teams by name: {}", name);

        List<Team> teams = teamRepository.searchByName(name);
        List<TeamDto.Response> response = teamMapperService.toResponseList(teams);

        return ResponseEntity.ok(response);
    }

    /**
     * Create new team
     * POST /api/teams
     */
    @PostMapping
    public ResponseEntity<TeamDto.Response> createTeam(
            @RequestBody TeamDto.Request request) {
        log.info("Creating new team: {}", request.getName());

        // Validate unique name
        if (teamRepository.findByName(request.getName()).isPresent()) {
            log.warn("Team with name {} already exists", request.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Team team = teamMapperService.toEntity(request);
        Team savedTeam = teamRepository.save(team);

        TeamDto.Response response = teamMapperService.toResponse(savedTeam);

        log.info("Team created successfully with id: {}", savedTeam.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update existing team
     * PUT /api/teams/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamDto.Response> updateTeam(
            @PathVariable UUID id,
            @RequestBody TeamDto.Request request) {
        log.info("Updating team id: {}", id);

        return teamRepository.findById(id)
                .map(team -> {
                    // Check if name is being changed and if it's unique
                    if (request.getName() != null &&
                            !request.getName().equals(team.getName())) {
                        if (teamRepository.findByName(request.getName()).isPresent()) {
                            log.warn("Team with name {} already exists", request.getName());
                            return ResponseEntity.status(HttpStatus.CONFLICT).<TeamDto.Response>build();
                        }
                    }

                    teamMapperService.updateEntity(team, request);
                    Team updatedTeam = teamRepository.save(team);
                    TeamDto.Response response = teamMapperService.toResponse(updatedTeam);

                    log.info("Team updated successfully: {}", id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deactivate team (soft delete)
     * DELETE /api/teams/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateTeam(@PathVariable UUID id) {
        log.info("Deactivating team id: {}", id);

        return teamRepository.findById(id)
                .map(team -> {
                    team.setActive(false);
                    teamRepository.save(team);
                    log.info("Team deactivated successfully: {}", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Permanently delete team
     * DELETE /api/teams/{id}/permanent
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deleteTeamPermanently(@PathVariable UUID id) {
        log.warn("Permanently deleting team id: {}", id);

        if (!teamRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        teamRepository.deleteById(id);
        log.info("Team deleted permanently: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivate deactivated team
     * PUT /api/teams/{id}/reactivate
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<TeamDto.Response> reactivateTeam(@PathVariable UUID id) {
        log.info("Reactivating team id: {}", id);

        return teamRepository.findById(id)
                .map(team -> {
                    team.setActive(true);
                    Team reactivatedTeam = teamRepository.save(team);
                    TeamDto.Response response = teamMapperService.toResponse(reactivatedTeam);

                    log.info("Team reactivated successfully: {}", id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get team statistics
     * GET /api/teams/{id}/stats
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<TeamStats> getTeamStats(@PathVariable UUID id) {
        log.info("Getting statistics for team id: {}", id);

        return teamRepository.findById(id)
                .map(team -> {
                    TeamStats stats = new TeamStats();
                    stats.setTeamId(team.getId());
                    stats.setTeamName(team.getName());
                    stats.setMemberCount(team.getEmployees() != null ? team.getEmployees().size() : 0);
                    stats.setActive(team.getActive());
                    stats.setCreatedDate(team.getCreationDate());

                    return ResponseEntity.ok(stats);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Inner class for team statistics
     */
    @lombok.Data
    public static class TeamStats {
        private UUID teamId;
        private String teamName;
        private Integer memberCount;
        private Boolean active;
        private java.time.LocalDateTime createdDate;
    }
}