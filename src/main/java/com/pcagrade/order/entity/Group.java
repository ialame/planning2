package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Group Entity - Role Management
 * Represents a group/role in the Pokemon card processing system
 */
@Entity
@Table(name = "j_group")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group extends AbstractUlidEntity {

    /**
     * Group name (e.g., "ADMIN", "MANAGER", "PROCESSOR", "VIEWER")
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Group name is required")
    private String name;

    /**
     * Computed column for case-insensitive searches (MariaDB compatible)
     */
    @Column(name = "name_upper", length = 50, insertable = false, updatable = false)
    private String nameUpper;

    /**
     * Group description
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Whether the group is active
     */
    @Column(name = "active")
    @NotNull
    @Builder.Default
    private Boolean active = true;

    /**
     * Group permissions level (1=lowest, 10=highest)
     */
    @Column(name = "permission_level")
    @Builder.Default
    private Integer permissionLevel = 1;

    /**
     * Date when the group was created
     */
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    /**
     * Date when the group was last modified
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ========== RELATIONSHIPS ==========

    /**
     * Many-to-many relationship with Employee
     */
    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Employee> employees = new ArrayList<>();

    // ========== BUSINESS METHODS ==========

    /**
     * Check if this group has a specific permission level or higher
     * @param requiredLevel the minimum required permission level
     * @return true if group has sufficient permissions
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return this.permissionLevel != null && this.permissionLevel >= requiredLevel;
    }

    /**
     * Check if this group is an admin group (permission level >= 8)
     * @return true if admin group
     */
    public boolean isAdminGroup() {
        return hasPermissionLevel(8);
    }

    /**
     * Check if this group is a manager group (permission level >= 5)
     * @return true if manager group
     */
    public boolean isManagerGroup() {
        return hasPermissionLevel(5);
    }

    /**
     * Add employee to this group
     * @param employee the employee to add
     */
    public void addEmployee(Employee employee) {
        if (employee != null && !this.employees.contains(employee)) {
            this.employees.add(employee);
            employee.getGroups().add(this);
        }
    }

    /**
     * Remove employee from this group
     * @param employee the employee to remove
     */
    public void removeEmployee(Employee employee) {
        if (employee != null && this.employees.contains(employee)) {
            this.employees.remove(employee);
            employee.getGroups().remove(this);
        }
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