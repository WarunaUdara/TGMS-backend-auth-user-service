package com.teamterraforge.tgmsauthanduserservice.entity;

import com.teamterraforge.tgmsauthanduserservice.entity.enums.LicenseStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * License entity representing guide licenses
 * Maps to the 'licenses' table in PostgreSQL
 */
@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "guide_id", nullable = false)
    private UUID guideId;

    @Column(name = "license_type", nullable = false, length = 100)
    private String licenseType;

    @Column(name = "issuing_region_code", length = 50)
    private String issuingRegionCode;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "license_status")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Builder.Default
    private LicenseStatus status = LicenseStatus.PENDING;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "doc_url", nullable = false, columnDefinition = "TEXT")
    private String docUrl;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
