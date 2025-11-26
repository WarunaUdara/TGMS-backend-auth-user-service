package com.teamterraforge.tgmsauthanduserservice.repository;

import com.teamterraforge.tgmsauthanduserservice.entity.License;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for License entity operations
 */
@Repository
public interface LicenseRepository extends JpaRepository<License, UUID> {

    /**
     * Find all licenses for a guide
     */
    List<License> findByGuideId(UUID guideId);

    /**
     * Find licenses by guide and status
     */
    List<License> findByGuideIdAndStatus(UUID guideId, LicenseStatus status);
}
