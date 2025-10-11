// ===================================================================
// OrderRepository.java
// ===================================================================
package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity
 * Handles database operations for Pokemon card orders
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by order number (unique identifier from Symfony)
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find order by Symfony order ID
     */
    Optional<Order> findBySymfonyOrderId(Long symfonyOrderId);

    /**
     * Check if order exists by order number
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(String status);

    /**
     * Find orders with delivery date before given date (overdue orders)
     */
//     @Query("SELECT o FROM Order o WHERE o.deliveryDate < :date AND o.status != 'COMPLETED'")
//     List<Order> findOverdueOrders(@Param("date") LocalDate date);

    /**
     * Find orders to be delivered soon (within next N days)
     */
//     @Query("SELECT o FROM Order o WHERE o.deliveryDate BETWEEN :start AND :end ORDER BY o.deliveryDate ASC")
//     List<Order> findOrdersDueSoon(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /**
     * Find orders ordered by priority (earliest delivery date first)
     */
//     @Query("SELECT o FROM Order o WHERE o.status != 'COMPLETED' ORDER BY o.deliveryDate ASC, o.priority ASC")
//     List<Order> findAllOrderedByPriority();

    /**
     * Find orders for a specific customer
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    /**
     * Find orders by delivery date
     */
    List<Order> findByDeliveryDate(LocalDate deliveryDate);

    /**
     * Count orders by status
     */
    long countByStatus(String status);

    /**
     * Find pending orders (not yet completed)
     */
//    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'IN_PROGRESS') ORDER BY o.deliveryDate ASC")
//    List<Order> findPendingOrders();

    /**
     * Get total cards count across all orders
     */
    @Query("SELECT SUM(o.totalCards) FROM Order o")
    Long getTotalCardsCount();

    /**
     * Get completed cards count across all orders
     */
//     @Query("SELECT SUM(o.completedCards) FROM Order o")
//     Long getCompletedCardsCount();
}