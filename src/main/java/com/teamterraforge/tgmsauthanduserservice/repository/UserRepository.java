package com.teamterraforge.tgmsauthanduserservice.repository;

import com.teamterraforge.tgmsauthanduserservice.entity.User;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find user by email and role
     */
    Optional<User> findByEmailIgnoreCaseAndRole(String email, UserRole role);
}
