package com.teamterraforge.tgmsauthanduserservice.service;

import com.teamterraforge.tgmsauthanduserservice.dto.UserResponse;
import com.teamterraforge.tgmsauthanduserservice.entity.User;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests user retrieval and validation business logic
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getUserById_WithExistingUser_ShouldReturnUser() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("Test User");
        assertThat(result.getRole()).isEqualTo(UserRole.TOURIST);

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WithNonExistingUser_ShouldThrowException() {
        // Given
        UUID nonExistingId = UUID.randomUUID();
        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(nonExistingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(nonExistingId);
    }

    @Test
    void getUserByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(mockUser));

        // When
        UserResponse result = userService.getUserByEmail(email);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualToIgnoringCase(email);
        assertThat(result.getId()).isEqualTo(userId);

        verify(userRepository).findByEmailIgnoreCase(email);
    }

    @Test
    void getUserByEmail_WithNonExistingEmail_ShouldThrowException() {
        // Given
        String nonExistingEmail = "nonexistent@example.com";
        when(userRepository.findByEmailIgnoreCase(nonExistingEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail(nonExistingEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmailIgnoreCase(nonExistingEmail);
    }

    @Test
    void getUserByEmail_WithCaseInsensitiveEmail_ShouldReturnUser() {
        // Given
        String emailUpperCase = "TEST@EXAMPLE.COM";
        when(userRepository.findByEmailIgnoreCase(emailUpperCase)).thenReturn(Optional.of(mockUser));

        // When
        UserResponse result = userService.getUserByEmail(emailUpperCase);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualToIgnoringCase(emailUpperCase);

        verify(userRepository).findByEmailIgnoreCase(emailUpperCase);
    }

    @Test
    void emailExists_WithExistingEmail_ShouldReturnTrue() {
        // Given
        String email = "test@example.com";
        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(true);

        // When
        boolean result = userService.emailExists(email);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmailIgnoreCase(email);
    }

    @Test
    void emailExists_WithNonExistingEmail_ShouldReturnFalse() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.existsByEmailIgnoreCase(email)).thenReturn(false);

        // When
        boolean result = userService.emailExists(email);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmailIgnoreCase(email);
    }

    @Test
    void emailExists_WithCaseInsensitiveEmail_ShouldReturnTrue() {
        // Given
        String emailUpperCase = "TEST@EXAMPLE.COM";
        when(userRepository.existsByEmailIgnoreCase(emailUpperCase)).thenReturn(true);

        // When
        boolean result = userService.emailExists(emailUpperCase);

        // Then
        assertThat(result).isTrue();
        verify(userRepository).existsByEmailIgnoreCase(emailUpperCase);
    }

    @Test
    void getUserById_WithNullId_ShouldThrowException() {
        // Given
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(null);
    }

    @Test
    void getUserByEmail_WithNullEmail_ShouldThrowException() {
        // Given
        when(userRepository.findByEmailIgnoreCase(null)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmailIgnoreCase(null);
    }

    @Test
    void getUserByEmail_WithEmptyEmail_ShouldThrowException() {
        // Given
        when(userRepository.findByEmailIgnoreCase("")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByEmail(""))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmailIgnoreCase("");
    }

    @Test
    void emailExists_WithNullEmail_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByEmailIgnoreCase(null)).thenReturn(false);

        // When
        boolean result = userService.emailExists(null);

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmailIgnoreCase(null);
    }

    @Test
    void emailExists_WithEmptyEmail_ShouldReturnFalse() {
        // Given
        when(userRepository.existsByEmailIgnoreCase("")).thenReturn(false);

        // When
        boolean result = userService.emailExists("");

        // Then
        assertThat(result).isFalse();
        verify(userRepository).existsByEmailIgnoreCase("");
    }

    @Test
    void getUserById_WithAdminUser_ShouldReturnAdminRole() {
        // Given
        User adminUser = User.builder()
                .id(userId)
                .email("admin@example.com")
                .passwordHash("hashedPassword")
                .name("Admin User")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_WithGuideUser_ShouldReturnGuideRole() {
        // Given
        User guideUser = User.builder()
                .id(userId)
                .email("guide@example.com")
                .passwordHash("hashedPassword")
                .name("Guide User")
                .role(UserRole.GUIDE)
                .createdAt(Instant.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(guideUser));

        // When
        UserResponse result = userService.getUserById(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(UserRole.GUIDE);
        verify(userRepository).findById(userId);
    }
}
