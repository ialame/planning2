package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity
 * Handles database operations for Pokemon card orders
 *
 * FIXED: All parameter types corrected to match Order entity field types
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by order number (unique identifier from Symfony)
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find order by Symfony order ID (ULID hex format)
     * FIXED: Changed from Long to String to match Order.symfonyOrderId field type
     */
    Optional<Order> findBySymfonyOrderId(String symfonyOrderId);

    /**
     * Check if order exists by order number
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find orders by status
     * FIXED: Changed from String to Integer to match Order.status field type
     */
    List<Order> findByStatus(Integer status);

    /**
     * Find orders by delivery date code (delai: X, F+, F, C, E)
     * FIXED: Changed from LocalDate to String - deliveryDate stores delai codes, not dates
     */
    List<Order> findByDeliveryDate(String deliveryDateCode);

    /**
     * Find orders for a specific customer
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Count orders by status
     * FIXED: Changed from String to Integer
     */
    long countByStatus(Integer status);

    /**
     * Get total cards count across all orders
     */
    @Query("SELECT SUM(o.totalCards) FROM Order o")
    Long getTotalCardsCount();
}