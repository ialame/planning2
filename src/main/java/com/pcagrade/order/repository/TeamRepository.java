package com.pcagrade.order.repository;

import com.pcagrade.order.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Team Repository
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByName(String name);

    List<Team> findByActiveTrue();

    @Query("SELECT t FROM Team t WHERE UPPER(t.name) LIKE UPPER(CONCAT('%', :searchTerm, '%'))")
    List<Team> searchByName(String searchTerm);

    // For synchronization - ULID advantage!
    List<Team> findByIdGreaterThan(UUID lastSyncId);

    Optional<Team> findTopByOrderByIdDesc();
}

