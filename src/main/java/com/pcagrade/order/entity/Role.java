package com.pcagrade.order.entity;

/**
 * Enumeration of employee roles in the Pokemon card grading system
 * Each role represents a specific function in the card processing workflow
 */
public enum Role {
    /**
     * Employee responsible for grading Pokemon cards
     * Evaluates card condition and assigns grades
     */
    ROLE_GRADER("Grader", "Grades Pokemon cards", false),

    /**
     * Employee responsible for certifying graded cards
     * Verifies grading accuracy and authenticates cards
     */
    ROLE_CERTIFIER("Certifier", "Certifies graded cards", false),

    /**
     * Employee responsible for scanning cards
     * Digitizes card images for records
     */
    ROLE_SCANNER("Scanner", "Scans cards for digital records", false),

    /**
     * Employee responsible for preparing orders for shipment
     * Packages and prepares orders for delivery
     */
    ROLE_PREPARER("Preparer", "Prepares orders for shipment", false),

    /**
     * Manager with planning and operational permissions
     * Can generate planning, manage employees, and oversee operations
     */
    ROLE_MANAGER("Manager", "Manages operations and planning", true),

    /**
     * Administrator with full system access
     * Can perform all operations including system configuration
     */
    ROLE_ADMIN("Admin", "Full system administration", true);

    private final String displayName;
    private final String description;
    private final boolean isManagement;

    Role(String displayName, String description, boolean isManagement) {
        this.displayName = displayName;
        this.description = description;
        this.isManagement = isManagement;
    }

    /**
     * Get human-readable name for the role
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get description of what this role does
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if this is an admin role (ADMIN)
     */
    public boolean isAdminRole() {
        return this == ROLE_ADMIN;
    }

    /**
     * Check if this is a management role (MANAGER or ADMIN)
     */
    public boolean isManagementRole() {
        return isManagement;
    }

    /**
     * Check if this role can generate planning
     */
    public boolean canGeneratePlanning() {
        return this == ROLE_MANAGER || this == ROLE_ADMIN;
    }

    /**
     * Check if this role can manage employees
     */
    public boolean canManageEmployees() {
        return this == ROLE_MANAGER || this == ROLE_ADMIN;
    }

    /**
     * Check if this role is an operational role (GRADER, CERTIFIER, SCANNER, PREPARER)
     */
    public boolean isOperationalRole() {
        return !isManagement;
    }

    /**
     * Get role from string name (e.g., "ROLE_GRADER")
     */
    public static Role fromString(String roleName) {
        for (Role role : Role.values()) {
            if (role.name().equals(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleName);
    }

    /**
     * Check if a string represents a valid role
     */
    public static boolean isValidRole(String roleName) {
        try {
            fromString(roleName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}