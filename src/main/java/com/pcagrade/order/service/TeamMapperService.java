package com.pcagrade.order.service;

import com.pcagrade.order.dto.TeamDto;
import com.pcagrade.order.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper Service for converting between Team entities and TeamDto
 * Handles the conversion logic between database entities and API DTOs
 */
@Service
@RequiredArgsConstructor
public class TeamMapperService {

    /**
     * Convert Team entity to TeamDto Response
     */
    public TeamDto.Response toResponse(Team team) {
        if (team == null) {
            return null;
        }

        TeamDto.Response response = new TeamDto.Response();
        response.setId(String.valueOf(team.getId()));
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setPermissionLevel(team.getPermissionLevel());
        response.setActive(team.getActive());
        response.setCreationDate(team.getCreationDate());
        response.setModificationDate(team.getModificationDate());

        return response;
    }

    /**
     * Convert TeamDto Request to Team entity
     */
    public Team toEntity(TeamDto.Request request) {
        if (request == null) {
            return null;
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setPermissionLevel(request.getPermissionLevel());
        team.setActive(true); // Default to active

        return team;
    }

    /**
     * Update existing Team entity from TeamDto Request
     */
    public void updateEntity(Team team, TeamDto.Request request) {
        if (team == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            team.setName(request.getName());
        }
        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }
        if (request.getPermissionLevel() != null) {
            team.setPermissionLevel(request.getPermissionLevel());
        }
    }

    /**
     * Convert Team entity to Summary DTO
     */
    public TeamDto.Summary toSummary(Team team) {
        if (team == null) {
            return null;
        }

        TeamDto.Summary summary = new TeamDto.Summary();
        summary.setId(String.valueOf(team.getId()));
        summary.setName(team.getName());
        summary.setMemberCount(team.getEmployees() != null ? team.getEmployees().size() : 0);
        summary.setActive(team.getActive());

        return summary;
    }

    /**
     * Convert Team entity to Detailed DTO
     */
    public TeamDto.Detailed toDetailed(Team team) {
        if (team == null) {
            return null;
        }

        TeamDto.Detailed detailed = new TeamDto.Detailed();
        detailed.setId(String.valueOf(team.getId()));
        detailed.setName(team.getName());
        detailed.setDescription(team.getDescription());
        detailed.setPermissionLevel(team.getPermissionLevel());
        detailed.setActive(team.getActive());
        detailed.setCreationDate(team.getCreationDate());
        detailed.setModificationDate(team.getModificationDate());
        detailed.setMemberCount(team.getEmployees() != null ? team.getEmployees().size() : 0);
        // activeAssignments would need additional service call
        detailed.setActiveAssignments(0);

        return detailed;
    }

    /**
     * Convert list of Team entities to list of TeamDto Responses
     */
    public List<TeamDto.Response> toResponseList(List<Team> teams) {
        if (teams == null) {
            return List.of();
        }

        return teams.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of Team entities to list of TeamDto Summaries
     */
    public List<TeamDto.Summary> toSummaryList(List<Team> teams) {
        if (teams == null) {
            return List.of();
        }

        return teams.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }
}