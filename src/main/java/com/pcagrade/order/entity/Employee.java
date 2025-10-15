package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee Entity - English Version
 * Represents an employee in the Pokemon card processing system
 */
@Entity
@Table(name = "employee")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AbstractUlidEntity {

    /**
     * Employee's first name
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * Employee's last name
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * Employee's email address
     */
    @Column(name = "email", length = 150)
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Work hours per day (default: 8 hours)
     */
    @Column(name = "work_hours_per_day")
    @Positive(message = "Work hours per day must be positive")
    @Builder.Default
    private Integer workHoursPerDay = 8;

    /**
     * Whether the employee is active
     */
    @Column(name = "active")
    @NotNull
    @Builder.Default
    private Boolean active = true;

    /**
     * Efficiency rating (default: 1.0 = 100%)
     */
    @Column(name = "efficiency_rating")
    @Builder.Default
    private Double efficiencyRating = 1.0;

    /**
     * Date when the employee was created
     */
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    /**
     * Date when the employee was last modified
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ========== RELATIONSHIPS ==========

    /**
     * Planning assignments for this employee
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Planning> plannings = new ArrayList<>();

    /**
     * Many-to-many relationship with Team (roles)
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "employee_team",
            joinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "team_id", referencedColumnName = "id")
    )
    @ToString.Exclude
    @Builder.Default
    private List<Team> teams = new ArrayList<>();

    // ========== BUSINESS METHODS ==========

    /**
     * Get employee's full name
     * @return firstName + " " + lastName
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get employee's initials
     * @return first letter of firstName + first letter of lastName
     */
    public String getInitials() {
        if (firstName != null && lastName != null &&
                !firstName.isEmpty() && !lastName.isEmpty()) {
            return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
        }
        return "??";
    }

    /**
     * Calculate daily capacity in minutes
     * @return work hours * 60 minutes * efficiency rating
     */
    public int getDailyCapacityMinutes() {
        if (workHoursPerDay == null || efficiencyRating == null) {
            return 8 * 60; // Default: 8 hours
        }
        return (int) (workHoursPerDay * 60 * efficiencyRating);
    }

    /**
     * Get the number of cards this employee can process per day
     * @param processingTimePerCard time in minutes to process one card
     * @return number of cards
     */
    public int getDailyCardCapacity(int processingTimePerCard) {
        if (processingTimePerCard <= 0) {
            return 0;
        }
        return getDailyCapacityMinutes() / processingTimePerCard;
    }

    // ========== GROUP/ROLE MANAGEMENT METHODS ==========

    /**
     * Add team to this employee
     * @param team the team to add
     */
    public void addTeam(Team team) {
        if (team != null && !this.teams.contains(team)) {
            this.teams.add(team);
            team.getEmployees().add(this);
        }
    }

    /**
     * Remove team from this employee
     * @param team the team to remove
     */
    public void removeTeam(Team team) {
        if (team != null && this.teams.contains(team)) {
            this.teams.remove(team);
            team.getEmployees().remove(this);
        }
    }

    /**
     * Check if employee belongs to a specific team
     * @param teamName the team name to check
     * @return true if employee belongs to the team
     */
    public boolean hasTeam(String teamName) {
        return teams.stream()
                .anyMatch(team -> team.getName().equalsIgnoreCase(teamName));
    }

    /**
     * Check if employee has admin role
     * @return true if employee has admin privileges
     */
    public boolean isAdmin() {
        return teams.stream()
                .anyMatch(Team::isAdminGroup);
    }

    /**
     * Check if employee has manager role
     * @return true if employee has manager privileges
     */
    public boolean isManager() {
        return teams.stream()
                .anyMatch(Team::isManagerGroup);
    }

    /**
     * Get highest permission level from all teams
     * @return highest permission level, or 1 if no teams
     */
    public int getHighestPermissionLevel() {
        return teams.stream()
                .filter(Team::getActive)
                .mapToInt(Team::getPermissionLevel)
                .max()
                .orElse(1);
    }

    /**
     * Check if employee has at least the required permission level
     * @param requiredLevel minimum required permission level
     * @return true if employee has sufficient permissions
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return getHighestPermissionLevel() >= requiredLevel;
    }

    /**
     * Get list of active group names
     * @return list of group names where employee is member
     */
    public List<String> getActiveGroupNames() {
        return teams.stream()
                .filter(Team::getActive)
                .map(Team::getName)
                .sorted()
                .toList();
    }

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
        if (modificationDate == null) {
            modificationDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();
    }
}