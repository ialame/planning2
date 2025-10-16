package com.pcagrade.order.entity;


import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Employee entity representing workers who process Pokemon card orders
 *
 * Inherits ULID primary key from AbstractUlidEntity for:
 * - Chronological ordering of employee records
 * - Database synchronization compatibility
 * - Optimal query performance
 */
@Entity
@Table(name = "employee")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AbstractUlidEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Employee roles: ROLE_GRADER, ROLE_CERTIFIER, ROLE_SCANNER, ROLE_PACKAGER
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "employee_team",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private Set<Team> teams = new HashSet<>();

    @Column(name = "active")
    private Boolean active = true;

    /**
     * Workload capacity per day (in minutes)
     * Default: 480 minutes (8 hours)
     */
    @Column(name = "daily_capacity_minutes")
    private Integer dailyCapacityMinutes = 480;

    /**
     * Check if employee has specific role
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}