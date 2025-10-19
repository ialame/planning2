package com.pcagrade.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Team entity representing a role/group in the organization
 * Examples: ROLE_GRADER, ROLE_AUTHENTICATOR, ROLE_SCANNER, ROLE_PREPARER
 */
@Entity
@Table(name = "team")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"employees"})
@ToString(exclude = {"employees"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends AbstractUlidEntity {

    /**
     * Unique role name (e.g., "ROLE_GRADER", "ROLE_AUTHENTICATOR")
     */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Human-readable description
     */
    @Column(length = 255)
    private String description;

    /**
     * Display name for UI (e.g., "Card Grader", "Authenticator")
     */
    @Column(length = 100)
    private String displayName;

    /**
     * Team color for UI visualization (hex code)
     */
    @Column(length = 7)
    private String color;

    /**
     * Team icon/emoji for UI
     */
    @Column(length = 10)
    private String icon;

    /**
     * Is this team currently active?
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Employees belonging to this team
     */
    @ManyToMany(mappedBy = "teams")
    @JsonIgnore
    private Set<Employee> employees = new HashSet<>();

    /**
     * Get total number of active employees in this team
     */
    public int getActiveEmployeeCount() {
        if (employees == null) return 0;
        return (int) employees.stream()
                .filter(emp -> Boolean.TRUE.equals(emp.getActive()))
                .count();
    }

    /**
     * Get total daily capacity of this team in minutes
     */
    public int getTotalDailyCapacityMinutes() {
        if (employees == null) return 0;
        return employees.stream()
                .filter(emp -> Boolean.TRUE.equals(emp.getActive()))
                .mapToInt(Employee::getEffectiveDailyCapacityMinutes)
                .sum();
    }
}