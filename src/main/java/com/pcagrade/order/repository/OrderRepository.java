package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findBySymfonyOrderId(String symfonyOrderId);
    
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Find orders by customer name (case insensitive search)
     */
    List<Order> findByCustomerNameContainingIgnoreCase(String customerName);

    List<Order> findByStatus(OrderStatus status);
    
    List<Order> findByDelai(String delai);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status " +
           "ORDER BY CASE o.delai " +
           "  WHEN 'X' THEN 1 " +
           "  WHEN 'F+' THEN 2 " +
           "  WHEN 'F' THEN 3 " +
           "  WHEN 'C' THEN 4 " +
           "  WHEN 'E' THEN 5 " +
           "  ELSE 6 " +
           "END, o.date DESC")
    List<Order> findByStatusOrderedByDeadline(@Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o " +
           "ORDER BY CASE o.delai " +
           "  WHEN 'X' THEN 1 " +
           "  WHEN 'F+' THEN 2 " +
           "  WHEN 'F' THEN 3 " +
           "  WHEN 'C' THEN 4 " +
           "  WHEN 'E' THEN 5 " +
           "  ELSE 6 " +
           "END, o.date DESC")
    List<Order> findPotentiallyOverdueOrders();
    
    @Query("SELECT o FROM Order o WHERE o.delai = :delai " +
           "ORDER BY o.date DESC")
    List<Order> findByDelaiOrderedByDate(@Param("delai") String delai);
    
    @Query("SELECT o FROM Order o WHERE o.delai IN ('X', 'F+', 'F') " +
           "ORDER BY CASE o.delai " +
           "  WHEN 'X' THEN 1 " +
           "  WHEN 'F+' THEN 2 " +
           "  WHEN 'F' THEN 3 " +
           "END, o.date DESC")
    List<Order> findHighPriorityOrders();
    
    @Query("SELECT o FROM Order o " +
           "ORDER BY CASE o.delai " +
           "  WHEN 'X' THEN 1 " +
           "  WHEN 'F+' THEN 2 " +
           "  WHEN 'F' THEN 3 " +
           "  WHEN 'C' THEN 4 " +
           "  WHEN 'E' THEN 5 " +
           "  ELSE 6 " +
           "END, o.date DESC")
    List<Order> findAllByPriority();
    
    long countByStatus(OrderStatus status);
    
    long countByDelai(String delai);
    
    @Query("SELECT COALESCE(SUM(o.totalCards), 0) FROM Order o")
    long getTotalCards();
    
    @Query("SELECT COALESCE(SUM(o.totalCards), 0) FROM Order o WHERE o.status = :status")
    long getTotalCardsByStatus(@Param("status") OrderStatus status);
    
    @Query("SELECT COALESCE(SUM(o.totalCards), 0) FROM Order o WHERE o.delai = :delai")
    long getTotalCardsByDelai(@Param("delai") String delai);
}
