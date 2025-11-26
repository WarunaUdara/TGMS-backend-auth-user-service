package com.teamterraforge.tgmsauthanduserservice.repository;

import com.teamterraforge.tgmsauthanduserservice.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Area entity operations
 */
@Repository
public interface AreaRepository extends JpaRepository<Area, UUID> {

    /**
     * Find area by country, region, and city
     */
    Optional<Area> findByCountryCodeAndRegionCodeAndCity(
            String countryCode, 
            String regionCode, 
            String city
    );
}
