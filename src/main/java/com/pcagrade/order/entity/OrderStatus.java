package com.pcagrade.order.entity;

/**
 * Order status enumeration
 */
public enum OrderStatus {
    PENDING,
    IN_PROGRESS,
    GRADING,
    CERTIFYING,
    SCANNING,
    PACKAGING,
    COMPLETED,
    DELIVERED
}