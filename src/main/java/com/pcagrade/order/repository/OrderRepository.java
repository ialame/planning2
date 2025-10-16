package com.pcagrade.order.repository;

import com.pcagrade.order.entity.AssignmentStatus;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.entity.WorkAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Order Repository
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.deliveryDeadline ASC")
    List<Order> findByStatusOrderedByDeadline(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.deliveryDeadline < CURRENT_TIMESTAMP AND o.status NOT IN ('COMPLETED', 'DELIVERED')")
    List<Order> findOverdueOrders();

    List<Order> findByCustomerEmailOrderByOrderDateDesc(String customerEmail);

    // For synchronization - ULID advantage!
    // Simple ID comparison instead of complex timestamp queries
    List<Order> findByIdGreaterThan(UUID lastSyncId);

    // Range queries using ULID's chronological ordering
    @Query("SELECT o FROM Order o WHERE o.id >= :fromId AND o.id <= :toId")
    List<Order> findByIdBetween(UUID fromId, UUID toId);

    Optional<Order> findTopByOrderByIdDesc();

    // Count orders created in a time range (using ULID)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.id >= :fromId AND o.id <= :toId")
    Long countByIdBetween(UUID fromId, UUID toId);
}

