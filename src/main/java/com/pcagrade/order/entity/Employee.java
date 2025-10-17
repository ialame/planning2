package com.pcagrade.order.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;  // ✅ AJOUTER

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employee")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"teams"})  // ✅ MODIFIER - Exclure teams
@ToString(exclude = {"teams"})  // ✅ AJOUTER - Exclure teams du toString
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AbstractUlidEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

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
    @JsonIgnore
    private Set<Team> teams = new HashSet<>();

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "daily_capacity_minutes")
    private Integer dailyCapacityMinutes = 480;

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}