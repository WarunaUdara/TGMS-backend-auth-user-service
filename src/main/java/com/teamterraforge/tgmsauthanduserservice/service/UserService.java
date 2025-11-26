package com.teamterraforge.tgmsauthanduserservice.service;

import com.teamterraforge.tgmsauthanduserservice.dto.*;
import com.teamterraforge.tgmsauthanduserservice.entity.User;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import com.teamterraforge.tgmsauthanduserservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        log.debug("Fetching user with ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        return mapToUserResponse(user);
    }

    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        return mapToUserResponse(user);
    }

    /**
     * Check if email exists
     */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.debug("Updating profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        boolean updated = false;
        
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
            updated = true;
            log.debug("Updated name for user: {}", userId);
        }
        
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
            updated = true;
            log.debug("Updated phone for user: {}", userId);
        }
        
        if (updated) {
            userRepository.save(user);
            log.info("Profile updated successfully for user: {}", userId);
        } else {
            log.debug("No profile changes for user: {}", userId);
        }
        
        return mapToUserResponse(user);
    }

    /**
     * Change user password
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.debug("Changing password for user ID: {}", userId);
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Initiate forgot password process
     * Generates a JWT token for password reset (valid for 1 hour)
     */
    @Transactional(readOnly = true)
    public PasswordResetResponse forgotPassword(ForgotPasswordRequest request) {
        log.debug("Forgot password request for email: {}", request.getEmail());
        
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + request.getEmail()));
        
        // Generate a password reset token (JWT with 1 hour expiration)
        String resetToken = generatePasswordResetToken(user);
        
        // In production, you would send this token via email
        // For now, we'll return it in the response (NOT RECOMMENDED FOR PRODUCTION)
        log.info("Password reset token generated for user: {}", user.getId());
        log.warn("DEVELOPMENT MODE: Reset token returned in response. In production, send via email only.");
        
        return PasswordResetResponse.builder()
                .message("Password reset instructions have been sent to your email")
                .email(user.getEmail())
                .build();
    }

    /**
     * Reset password using token
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Resetting password with token");
        
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }
        
        // Validate and extract user email from token
        String email;
        try {
            email = jwtService.extractUsername(request.getToken());
            if (!jwtService.isTokenValid(request.getToken(), email)) {
                throw new IllegalArgumentException("Invalid or expired reset token");
            }
        } catch (Exception e) {
            log.error("Invalid reset token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired reset token");
        }
        
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password reset successfully for user: {}", user.getId());
    }

    /**
     * Delete user account (soft delete by setting role to null would require schema change)
     * For now, we'll actually delete the user. In production, use a status field.
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        log.debug("Deleting account for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        userRepository.delete(user);
        log.info("Account deleted successfully for user: {}", userId);
    }

    /**
     * Get public profile (limited information)
     */
    @Transactional(readOnly = true)
    public UserResponse getPublicProfile(UUID userId) {
        log.debug("Fetching public profile for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        // Return limited information for public profile
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Get all users (admin only) with pagination
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination - page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<User> userPage = userRepository.findAll(pageable);
        
        PageResponse<UserResponse> response = PageResponse.<UserResponse>builder()
                .content(userPage.getContent().stream()
                        .map(this::mapToUserResponse)
                        .toList())
                .pageNumber(userPage.getNumber())
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .empty(userPage.isEmpty())
                .build();
        
        log.debug("Returning {} users out of {} total", 
                response.getContent().size(), response.getTotalElements());
        
        return response;
    }

    /**
     * Get user entity by ID (internal use)
     */
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Generate password reset token (JWT valid for 1 hour)
     */
    private String generatePasswordResetToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "password_reset");
        claims.put("userId", user.getId().toString());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 3600000); // 1 hour
        
        return io.jsonwebtoken.Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(jwtService.getSigningKey())
                .compact();
    }

    /**
     * Map User entity to UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
