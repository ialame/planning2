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
@Table(name = "j_employee")
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
     * Many-to-many relationship with Group (roles)
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "j_employee_group",
            joinColumns = @JoinColumn(name = "employee_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id")
    )
    @ToString.Exclude
    @Builder.Default
    private List<Group> groups = new ArrayList<>();

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
     * Add group to this employee
     * @param group the group to add
     */
    public void addGroup(Group group) {
        if (group != null && !this.groups.contains(group)) {
            this.groups.add(group);
            group.getEmployees().add(this);
        }
    }

    /**
     * Remove group from this employee
     * @param group the group to remove
     */
    public void removeGroup(Group group) {
        if (group != null && this.groups.contains(group)) {
            this.groups.remove(group);
            group.getEmployees().remove(this);
        }
    }

    /**
     * Check if employee belongs to a specific group
     * @param groupName the group name to check
     * @return true if employee belongs to the group
     */
    public boolean hasGroup(String groupName) {
        return groups.stream()
                .anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
    }

    /**
     * Check if employee has admin role
     * @return true if employee has admin privileges
     */
    public boolean isAdmin() {
        return groups.stream()
                .anyMatch(Group::isAdminGroup);
    }

    /**
     * Check if employee has manager role
     * @return true if employee has manager privileges
     */
    public boolean isManager() {
        return groups.stream()
                .anyMatch(Group::isManagerGroup);
    }

    /**
     * Get highest permission level from all groups
     * @return highest permission level, or 1 if no groups
     */
    public int getHighestPermissionLevel() {
        return groups.stream()
                .filter(Group::getActive)
                .mapToInt(Group::getPermissionLevel)
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
        return groups.stream()
                .filter(Group::getActive)
                .map(Group::getName)
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