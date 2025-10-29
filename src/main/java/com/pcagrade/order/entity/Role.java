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
    ROLE_GRADER("Grader", "Grades Pokemon cards"),

    /**
     * Employee responsible for certifying graded cards
     * Verifies grading accuracy and authenticates cards
     */
    ROLE_CERTIFIER("Certifier", "Certifies graded cards"),

    /**
     * Employee responsible for scanning cards
     * Digitizes card images for records
     */
    ROLE_SCANNER("Scanner", "Scans cards for digital records"),

    /**
     * Employee responsible for preparing orders for shipment
     * Packages and prepares orders for delivery
     */
    ROLE_PREPARER("Preparer", "Prepares orders for shipment");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
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
     * Check if this is an admin role
     * Can be extended in the future for admin roles
     */
    public boolean isAdminRole() {
        return false; // Can add ROLE_ADMIN later if needed
    }
}