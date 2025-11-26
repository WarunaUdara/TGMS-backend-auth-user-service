package com.teamterraforge.tgmsauthanduserservice.entity.enums;

/**
 * Status of guide licenses
 * Corresponds to license_status enum in PostgreSQL
 */
public enum LicenseStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    EXPIRED
}
