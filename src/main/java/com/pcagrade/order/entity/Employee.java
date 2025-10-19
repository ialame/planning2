package com.pcagrade.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Employee entity with team-based role management
 * Uses employee_team table for role assignments via Team entities
 */
@Entity
@Table(name = "employee")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"teams"})
@ToString(exclude = {"teams"})
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AbstractUlidEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String photoUrl;

    /**
     * Teams (roles) this employee belongs to
     * Each team represents a role (ROLE_GRADER, ROLE_AUTHENTICATOR, etc.)
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "employee_team",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    @JsonIgnore
    private Set<Team> teams = new HashSet<>();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "daily_capacity_minutes")
    private Integer dailyCapacityMinutes = 480; // 8 hours = 480 minutes

    @Column(name = "efficiency_rating")
    private Double efficiencyRating = 1.0; // 1.0 = 100% efficiency

    /**
     * Check if employee has a specific role by team name
     * @param roleName Role name (e.g., "ROLE_GRADER")
     * @return true if employee belongs to this team/role
     */
    public boolean hasRole(String roleName) {
        if (teams == null || teams.isEmpty()) {
            return false;
        }
        return teams.stream()
                .anyMatch(team -> team.getName().equals(roleName));
    }

    /**
     * Check if employee has any of the specified roles
     * @param roleNames Multiple role names to check
     * @return true if employee has at least one of these roles
     */
    public boolean hasAnyRole(String... roleNames) {
        if (teams == null || teams.isEmpty()) {
            return false;
        }
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all role names this employee has
     * @return Set of role names (e.g., ["ROLE_GRADER", "ROLE_SCANNER"])
     */
    public Set<String> getRoleNames() {
        if (teams == null) {
            return new HashSet<>();
        }
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get employee full name
     * @return First name + Last name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get effective daily capacity in minutes (accounting for efficiency)
     * @return Adjusted capacity based on efficiency rating
     */
    public int getEffectiveDailyCapacityMinutes() {
        if (efficiencyRating == null || efficiencyRating <= 0) {
            return dailyCapacityMinutes;
        }
        return (int) (dailyCapacityMinutes * efficiencyRating);
    }

    /**
     * Add a team/role to this employee
     */
    public void addTeam(Team team) {
        if (teams == null) {
            teams = new HashSet<>();
        }
        teams.add(team);
        team.getEmployees().add(this);
    }

    /**
     * Remove a team/role from this employee
     */
    public void removeTeam(Team team) {
        if (teams != null) {
            teams.remove(team);
            team.getEmployees().remove(this);
        }
    }
}