// ========== ENGLISH VERSION: EmployeeRepository.java ==========
// src/main/java/com/pcagrade/order/repository/EmployeeRepository.java

package com.pcagrade.order.repository;

import com.pcagrade.order.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Employee Repository
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmail(String email);

    List<Employee> findByActiveTrue();

    List<Employee> findByActiveAndRolesContaining(Boolean active, String role);

    @Query("SELECT e FROM Employee e JOIN e.teams t WHERE t.id = :teamId AND e.active = true")
    List<Employee> findActiveByTeamId(UUID teamId);

    @Query("SELECT e FROM Employee e WHERE e.active = true AND :role MEMBER OF e.roles")
    List<Employee> findActiveEmployeesByRole(String role);

    // For synchronization - ULID advantage!
    List<Employee> findByIdGreaterThan(UUID lastSyncId);

    Optional<Employee> findTopByOrderByIdDesc();
}

