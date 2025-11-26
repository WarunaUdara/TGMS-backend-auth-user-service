package com.teamterraforge.tgmsauthanduserservice.service;

import com.teamterraforge.tgmsauthanduserservice.dto.*;
import com.teamterraforge.tgmsauthanduserservice.entity.User;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import com.teamterraforge.tgmsauthanduserservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService - New API endpoints
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - New APIs Tests")
class UserServiceNewApisTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .name("Test User")
                .phone("+1234567890")
                .passwordHash("hashedPassword")
                .role(UserRole.TOURIST)
                .createdAt(Instant.now())
                .lastLogin(Instant.now())
                .build();
    }

    // ===== UPDATE PROFILE TESTS =====

    @Test
    @DisplayName("Should update user profile with name and phone")
    void shouldUpdateProfileSuccessfully() {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("Updated Name")
                .phone("+9876543210")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Name");
        assertThat(response.getPhone()).isEqualTo("+9876543210");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should update only name when phone is null")
    void shouldUpdateOnlyName() {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("New Name")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateProfile(testUserId, request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found for profile update")
    void shouldThrowExceptionWhenUserNotFoundForUpdate() {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("New Name")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ===== CHANGE PASSWORD TESTS =====

    @Test
    @DisplayName("Should change password successfully")
    void shouldChangePasswordSuccessfully() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", testUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.changePassword(testUserId, request);

        // Then
        verify(passwordEncoder).matches("oldPassword", testUser.getPasswordHash());
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception when passwords don't match")
    void shouldThrowExceptionWhenPasswordsDontMatch() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword")
                .newPassword("NewPassword123")
                .confirmPassword("DifferentPassword123")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void shouldThrowExceptionWhenCurrentPasswordIncorrect() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("Should throw exception when new password same as current")
    void shouldThrowExceptionWhenNewPasswordSameAsCurrent() {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("samePassword")
                .newPassword("samePassword")
                .confirmPassword("samePassword")
                .build();

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("samePassword", testUser.getPasswordHash())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(testUserId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be different");
    }

    // ===== FORGOT PASSWORD TESTS =====

    @Test
    @DisplayName("Should generate password reset token")
    void shouldGeneratePasswordResetToken() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("test@example.com")
                .build();

        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(testUser));

        // When
        PasswordResetResponse response = userService.forgotPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getMessage()).contains("reset instructions");
    }

    @Test
    @DisplayName("Should throw exception for forgot password with invalid email")
    void shouldThrowExceptionForInvalidEmail() {
        // Given
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent@example.com")
                .build();

        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.forgotPassword(request))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ===== RESET PASSWORD TESTS =====

    @Test
    @DisplayName("Should reset password with valid token")
    void shouldResetPasswordWithValidToken() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("validToken")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(jwtService.extractUsername("validToken")).thenReturn("test@example.com");
        when(jwtService.isTokenValid("validToken", "test@example.com")).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("test@example.com"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.resetPassword(request);

        // Then
        verify(jwtService).extractUsername("validToken");
        verify(jwtService).isTokenValid("validToken", "test@example.com");
        verify(passwordEncoder).encode("NewPassword123");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw exception for reset password with mismatched passwords")
    void shouldThrowExceptionForMismatchedPasswords() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("validToken")
                .newPassword("Password123")
                .confirmPassword("DifferentPassword123")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("Should throw exception for invalid reset token")
    void shouldThrowExceptionForInvalidToken() {
        // Given
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("invalidToken")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(jwtService.extractUsername("invalidToken")).thenThrow(new RuntimeException("Invalid token"));

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired");
    }

    // ===== DELETE ACCOUNT TESTS =====

    @Test
    @DisplayName("Should delete user account")
    void shouldDeleteAccount() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        userService.deleteAccount(testUserId);

        // Then
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("Should throw exception when user not found for deletion")
    void shouldThrowExceptionWhenUserNotFoundForDeletion() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.deleteAccount(testUserId))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ===== PUBLIC PROFILE TESTS =====

    @Test
    @DisplayName("Should get public profile with limited information")
    void shouldGetPublicProfile() {
        // Given
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getPublicProfile(testUserId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(testUserId);
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getRole()).isEqualTo(UserRole.TOURIST);
        assertThat(response.getEmail()).isNull(); // Email should not be in public profile
        assertThat(response.getPhone()).isNull(); // Phone should not be in public profile
    }

    // ===== PAGINATION TESTS =====

    @Test
    @DisplayName("Should get all users with pagination")
    void shouldGetAllUsersWithPagination() {
        // Given
        User user1 = User.builder().id(UUID.randomUUID()).email("user1@test.com")
                .name("User 1").role(UserRole.TOURIST).createdAt(Instant.now()).build();
        User user2 = User.builder().id(UUID.randomUUID()).email("user2@test.com")
                .name("User 2").role(UserRole.GUIDE).createdAt(Instant.now()).build();

        Page<User> userPage = new PageImpl<>(Arrays.asList(user1, user2), 
                PageRequest.of(0, 20), 2);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // When
        PageResponse<UserResponse> response = userService.getAllUsers(PageRequest.of(0, 20));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should handle empty page")
    void shouldHandleEmptyPage() {
        // Given
        Page<User> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When
        PageResponse<UserResponse> response = userService.getAllUsers(PageRequest.of(0, 20));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0);
        assertThat(response.isEmpty()).isTrue();
    }
}
