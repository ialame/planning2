package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Invoice Entity - READ-ONLY
 *
 * ⚠️ CRITICAL: This table is shared with the financial system
 *
 * DO NOT MODIFY:
 * - This table is managed by an external financial/accounting system
 * - This entity is for READ OPERATIONS ONLY
 * - All write operations will fail or be ignored
 * - Data is synced from the dev database
 *
 * Purpose: Access invoice/billing data for reporting and reference
 * Owner: Financial system
 * Access: Read-only
 */
@Entity
@Table(name = "invoice")
@Immutable  // ← CRITICAL: This marks the entity as read-only in Hibernate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Invoice extends AbstractUlidEntity {

    /**
     * Invoice number (unique identifier)
     * Format: INV-YYYYMMDD-XXXX
     */
    @Column(name = "invoice_number", insertable = false, updatable = false)
    private String invoiceNumber;

    /**
     * Order ID this invoice is for
     * Reference to order table (no FK constraint - read-only)
     */
    @Column(name = "order_id", insertable = false, updatable = false, columnDefinition = "BINARY(16)")
    private byte[] orderIdBytes;

    /**
     * Customer/client name
     */
    @Column(name = "customer_name", insertable = false, updatable = false)
    private String customerName;

    /**
     * Customer email
     */
    @Column(name = "customer_email", insertable = false, updatable = false)
    private String customerEmail;

    /**
     * Total amount (in currency)
     */
    @Column(name = "amount", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code (EUR, USD, etc.)
     */
    @Column(name = "currency", insertable = false, updatable = false, length = 3)
    private String currency;

    /**
     * Invoice status
     * Values: DRAFT, SENT, PAID, OVERDUE, CANCELLED
     */
    @Column(name = "status", insertable = false, updatable = false, length = 20)
    private String status;

    /**
     * Date invoice was issued
     */
    @Column(name = "issue_date", insertable = false, updatable = false)
    private LocalDateTime issueDate;

    /**
     * Due date for payment
     */
    @Column(name = "due_date", insertable = false, updatable = false)
    private LocalDateTime dueDate;

    /**
     * Date payment was received (if paid)
     */
    @Column(name = "payment_date", insertable = false, updatable = false)
    private LocalDateTime paymentDate;

    /**
     * Additional notes or comments
     */
    @Column(name = "notes", insertable = false, updatable = false, columnDefinition = "TEXT")
    private String notes;

    /**
     * Creation date
     */
    @Column(name = "creation_date", insertable = false, updatable = false)
    private LocalDateTime creationDate;

    /**
     * Last modification date
     */
    @Column(name = "modification_date", insertable = false, updatable = false)
    private LocalDateTime modificationDate;

    // ========== HELPER METHODS ==========

    /**
     * Check if invoice is paid
     * @return true if status is PAID
     */
    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    /**
     * Check if invoice is overdue
     * @return true if status is OVERDUE
     */
    public boolean isOverdue() {
        return "OVERDUE".equalsIgnoreCase(status);
    }

    /**
     * Check if payment is pending
     * @return true if sent but not yet paid
     */
    public boolean isPaymentPending() {
        return "SENT".equalsIgnoreCase(status);
    }

    /**
     * Get formatted amount with currency
     * @return formatted string like "€125.50"
     */
    public String getFormattedAmount() {
        if (amount == null) {
            return "N/A";
        }

        String symbol = switch (currency != null ? currency.toUpperCase() : "EUR") {
            case "EUR" -> "€";
            case "USD" -> "$";
            case "GBP" -> "£";
            default -> currency + " ";
        };

        return symbol + amount.toString();
    }

    /**
     * Get order ID as UUID string
     * Converts BINARY(16) to UUID format
     * @return UUID string or null
     */
    public String getOrderIdAsString() {
        if (orderIdBytes == null || orderIdBytes.length != 16) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderIdBytes.length; i++) {
            sb.append(String.format("%02x", orderIdBytes[i]));
            if (i == 3 || i == 5 || i == 7 || i == 9) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    // ========== IMPORTANT NOTES ==========

    /*
     * READ-ONLY ENTITY GUIDELINES:
     *
     * 1. @Immutable annotation prevents Hibernate from tracking changes
     * 2. insertable=false, updatable=false on all columns
     * 3. No setter methods should be used in business logic
     * 4. Repository should extend ReadOnlyRepository or restrict to find* methods
     * 5. Any attempt to save/update will be ignored or throw exception
     *
     * WHAT YOU CAN DO:
     * ✅ findById()
     * ✅ findAll()
     * ✅ findByStatus()
     * ✅ Custom read queries
     *
     * WHAT YOU CANNOT DO:
     * ❌ save()
     * ❌ saveAll()
     * ❌ delete()
     * ❌ update()
     *
     * DATA SOURCE:
     * This data is synced from the dev database
     * Sync frequency: Daily or on-demand
     *
     * IF YOU NEED TO MODIFY INVOICE DATA:
     * → Use the financial system's interface
     * → Changes will sync to this table automatically
     */
}