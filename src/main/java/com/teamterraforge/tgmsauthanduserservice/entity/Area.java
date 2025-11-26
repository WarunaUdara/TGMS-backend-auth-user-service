package com.teamterraforge.tgmsauthanduserservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Area entity representing geographical locations
 * Maps to the 'areas' table in PostgreSQL
 */
@Entity
@Table(name = "areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "region_code", length = 50)
    private String regionCode;

    @Column(length = 200)
    private String city;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(columnDefinition = "TEXT")
    private String geometry;
}
