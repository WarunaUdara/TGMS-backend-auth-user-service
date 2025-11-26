package com.teamterraforge.tgmsauthanduserservice.dto;

import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private Instant createdAt;
    private Instant lastLogin;
}
