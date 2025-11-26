package com.teamterraforge.tgmsauthanduserservice.repository;

import com.teamterraforge.tgmsauthanduserservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity operations
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by resource type and ID
     */
    List<AuditLog> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, 
            UUID resourceId
    );

    /**
     * Find audit logs by actor
     */
    List<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId);
}
