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
 *
 * CORRECTED: Uses String ID (ULID as VARCHAR/CHAR) as per your architecture
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
     * Supports large base64 images (up to ~12MB original image)
     */
    @Lob
    @Column(name = "photo_url", columnDefinition = "MEDIUMTEXT")
    private String photoUrl;

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
     * Each team represents a role (ROLE_GRADER, ROLE_CERTIFIER, etc.)
     * Many-to-many relationship
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "employee_team",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private Set<Team> teams = new HashSet<>();

    /**
     * Get daily capacity in minutes (workHoursPerDay * 60 * efficiencyRating)
     * Returns null if workHoursPerDay or efficiencyRating is null
     */
    public Integer getDailyCapacityMinutes() {
        if (workHoursPerDay == null || efficiencyRating == null) {
            return null;
        }
        return (int) (workHoursPerDay * 60 * efficiencyRating);
    }

    /**
     * Set daily capacity in minutes
     * Converts back to workHoursPerDay based on current efficiency rating
     */
    public void setDailyCapacityMinutes(Integer minutes) {
        if (minutes == null) {
            this.workHoursPerDay = null;
            return;
        }
        if (efficiencyRating != null && efficiencyRating > 0) {
            this.workHoursPerDay = (int) Math.round(minutes / (60.0 * efficiencyRating));
        } else {
            this.workHoursPerDay = minutes / 60;
        }
    }

    /**
     * Get effective daily capacity considering efficiency
     * Returns null if calculation is not possible
     */
    public Integer getEffectiveDailyCapacityMinutes() {
        return getDailyCapacityMinutes();
    }

    /**
     * Get all role names this employee has
     */
    public Set<String> getRoleNames() {
        if (teams == null) return new HashSet<>();
        return teams.stream()
                .map(Team::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Check if employee has a specific role
     */
    public boolean hasRole(String roleName) {
        if (teams == null) return false;
        return teams.stream()
                .anyMatch(team -> team.getName().equals(roleName));
    }

    /**
     * Add a team (role) to this employee
     */
    public void addTeam(Team team) {
        if (teams == null) {
            teams = new HashSet<>();
        }
        teams.add(team);
        if (team.getEmployees() != null && !team.getEmployees().contains(this)) {
            team.getEmployees().add(this);
        }
    }

    /**
     * Remove a team (role) from this employee
     */
    public void removeTeam(Team team) {
        if (teams != null) {
            teams.remove(team);
        }
        if (team.getEmployees() != null) {
            team.getEmployees().remove(this);
        }
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}