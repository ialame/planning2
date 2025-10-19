package com.pcagrade.order.service;

import com.pcagrade.order.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for mapping Team entities to DTOs
 * Note: If you don't have TeamDTO, you can remove this service
 */
@Service
@RequiredArgsConstructor
public class TeamMapperService {

    /**
     * Map Team entity to DTO
     * NOTE: You need to create TeamDTO class or remove this method
     */
    public TeamDTO toDTO(Team team) {
        if (team == null) {
            return null;
        }

        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId().toString()); // Convert UUID to String
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setDisplayName(team.getDisplayName());
        dto.setColor(team.getColor());
        dto.setIcon(team.getIcon());
        dto.setActive(team.getActive());
        dto.setEmployeeCount(team.getActiveEmployeeCount());

        return dto;
    }

    /**
     * Map DTO to Team entity
     */
    public Team toEntity(TeamDTO dto) {
        if (dto == null) {
            return null;
        }

        Team team = new Team();
        team.setName(dto.getName());
        team.setDescription(dto.getDescription());
        team.setDisplayName(dto.getDisplayName());
        team.setColor(dto.getColor());
        team.setIcon(dto.getIcon());
        team.setActive(dto.getActive());

        return team;
    }

    /**
     * Update entity from DTO
     */
    public void updateEntityFromDTO(Team team, TeamDTO dto) {
        if (team == null || dto == null) {
            return;
        }

        if (dto.getDescription() != null) {
            team.setDescription(dto.getDescription());
        }
        if (dto.getDisplayName() != null) {
            team.setDisplayName(dto.getDisplayName());
        }
        if (dto.getColor() != null) {
            team.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) {
            team.setIcon(dto.getIcon());
        }
        if (dto.getActive() != null) {
            team.setActive(dto.getActive());
        }
    }

    /**
     * Simple DTO class (you can move this to a separate file)
     */
    public static class TeamDTO {
        private String id;
        private String name;
        private String description;
        private String displayName;
        private String color;
        private String icon;
        private Boolean active;
        private Integer employeeCount;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }

        public Integer getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    }
}