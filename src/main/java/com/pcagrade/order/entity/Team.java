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
 * Team entity representing a group of employees working together
 * Renamed from "Group" to avoid SQL reserved keyword conflict
 *
 * Inherits ULID primary key from AbstractUlidEntity for:
 * - Chronological ordering
 * - Database synchronization compatibility
 * - Optimal index performance
 */
@Entity
@Table(name = "team")
@Data  // ✅ MUST HAVE THIS - Generates getters/setters
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Team extends AbstractUlidEntity {

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(length = 50)
    private String nameUpper;

    @Column(length = 255)
    private String description;

    @Column(name = "permission_level")
    private Integer permissionLevel;

    @Column(name = "active")
    private Boolean active = true;

    @ManyToMany(mappedBy = "teams")
    private Set<Employee> employees = new HashSet<>();

    @PrePersist
    protected void onTeamCreate() {
        super.onCreate();
        if (name != null) {
            nameUpper = name.toUpperCase();
        }
    }

    @PreUpdate
    protected void onTeamUpdate() {
        super.onUpdate();
        if (name != null) {
            nameUpper = name.toUpperCase();
        }
    }
}