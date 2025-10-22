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

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Photo URL or base64 data URL
     *
     * Options for column type:
     *
     * 1. MEDIUMTEXT (RECOMMENDED) - Up to 16MB
     *    Supports large base64 images (up to ~12MB original image)
     */
    @Lob
    @Column(name = "photo_url", columnDefinition = "MEDIUMTEXT")
    private String photoUrl;

    /**
     * Alternative option if you want smaller limit:
     *
     * 2. TEXT - Up to 64KB
     *    Supports smaller base64 images (up to ~48KB original image)
     *
     * @Lob
     * @Column(name = "photo_url", columnDefinition = "TEXT")
     * private String photoUrl;
     */

    /**
     * Work hours per day (e.g., 8 hours)
     */
    @Column(name = "work_hours_per_day", nullable = false)
    private Integer workHoursPerDay = 8;

    /**
     * Is employee currently active?
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Employee efficiency rating (0.0 to 2.0)
     * 1.0 = normal speed
     * 1.5 = 50% faster
     * 0.8 = 20% slower
     */
    @Column(name = "efficiency_rating", nullable = false)
    private Double efficiencyRating = 1.0;

    /**
     * Teams (roles) this employee belongs to
     * Each team represents a role (ROLE_GRADER, ROLE_AUTHENTICATOR, etc.)
     */
    @ManyToMany
    @JoinTable(
            name = "employee_team",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    @JsonIgnore
    private Set<Team> teams = new HashSet<>();

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Get employee's full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get daily capacity in minutes (without efficiency consideration)
     * Used for backward compatibility
     * Returns Integer (nullable) instead of int for null safety
     */
    public Integer getDailyCapacityMinutes() {
        if (workHoursPerDay == null) {
            return null;
        }
        return workHoursPerDay * 60;
    }

    /**
     * Set daily capacity in minutes
     * Converts back to work hours (backward compatibility)
     */
    public void setDailyCapacityMinutes(Integer minutes) {
        if (minutes == null) {
            this.workHoursPerDay = 8; // default value
        } else {
            this.workHoursPerDay = minutes / 60;
        }
    }

    /**
     * Get effective daily capacity in minutes considering efficiency
     * Formula: work_hours_per_day * 60 * efficiency_rating
     */
    public int getEffectiveDailyCapacityMinutes() {
        return (int) (workHoursPerDay * 60 * efficiencyRating);
    }

    /**
     * Add a team to this employee
     */
    public void addTeam(Team team) {
        if (teams == null) {
            teams = new HashSet<>();
        }
        teams.add(team);
        team.getEmployees().add(this);
    }

    /**
     * Remove a team from this employee
     */
    public void removeTeam(Team team) {
        if (teams != null) {
            teams.remove(team);
            team.getEmployees().remove(this);
        }
    }

    /**
     * Get role names from teams
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
     * Check if employee has a specific role
     */
    public boolean hasRole(String roleName) {
        if (teams == null) {
            return false;
        }
        return teams.stream()
                .anyMatch(team -> team.getName().equals(roleName));
    }

    /**
     * Get number of active teams
     */
    public int getActiveTeamCount() {
        if (teams == null) {
            return 0;
        }
        return (int) teams.stream()
                .filter(team -> Boolean.TRUE.equals(team.getActive()))
                .count();
    }
}