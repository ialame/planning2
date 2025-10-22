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

    /**
     * Find employee by email (used for authentication)
     */
    Optional<Employee> findByEmail(String email);


    List<Employee> findByActiveTrue();

    @Query("SELECT DISTINCT e FROM Employee e " +
            "JOIN e.teams t " +
            "WHERE e.active = :active AND t.name = :teamName")
    List<Employee> findByActiveAndRolesContaining(@Param("active") Boolean active,
                                                  @Param("teamName") String teamName);

    @Query("SELECT e FROM Employee e JOIN e.teams t WHERE t.id = :teamId AND e.active = true")
    List<Employee> findActiveByTeamId(UUID teamId);

    @Query("SELECT e FROM Employee e JOIN e.teams t WHERE e.active = true AND t.name = :roleName")
    List<Employee> findActiveEmployeesByRole(String role);

    // For synchronization - ULID advantage!
    List<Employee> findByIdGreaterThan(UUID lastSyncId);

    Optional<Employee> findTopByOrderByIdDesc();
}

