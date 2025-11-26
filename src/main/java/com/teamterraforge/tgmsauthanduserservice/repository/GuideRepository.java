package com.teamterraforge.tgmsauthanduserservice.repository;

import com.teamterraforge.tgmsauthanduserservice.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Guide entity operations
 */
@Repository
public interface GuideRepository extends JpaRepository<Guide, UUID> {

    /**
     * Find guide by user ID
     */
    Optional<Guide> findByUserId(UUID userId);

    /**
     * Check if guide exists for user
     */
    boolean existsByUserId(UUID userId);
}
