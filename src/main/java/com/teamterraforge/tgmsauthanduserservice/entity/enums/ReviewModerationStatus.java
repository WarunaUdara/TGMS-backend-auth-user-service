package com.teamterraforge.tgmsauthanduserservice.entity.enums;

/**
 * Moderation status for reviews
 * Corresponds to review_moderation_status enum in PostgreSQL
 */
public enum ReviewModerationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    HIDDEN
}
