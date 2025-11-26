package com.teamterraforge.tgmsauthanduserservice.entity.enums;

/**
 * Booking lifecycle status
 * Corresponds to booking_status enum in PostgreSQL
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
