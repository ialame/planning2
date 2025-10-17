package com.pcagrade.order.entity;

import com.pcagrade.order.entity.ulid.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "team")
@Data
//@EqualsAndHashCode(callSuper = true)
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