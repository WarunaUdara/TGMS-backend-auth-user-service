package com.teamterraforge.tgmsauthanduserservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Guide entity representing tour guide profiles
 * Maps to the 'guides' table in PostgreSQL
 */
@Entity
@Table(name = "guides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String languages;

    @Column(name = "per_day_rate", precision = 10, scale = 2)
    private BigDecimal perDayRate;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "profile_complete_score", nullable = false)
    @Builder.Default
    private Integer profileCompleteScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(columnDefinition = "TEXT")
    private String photos;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_active_at")
    private Instant lastActiveAt;
}
