package com.teamterraforge.tgmsauthanduserservice.service;

import com.teamterraforge.tgmsauthanduserservice.dto.LoginRequest;
import com.teamterraforge.tgmsauthanduserservice.dto.RegisterRequest;
import com.teamterraforge.tgmsauthanduserservice.entity.User;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import com.teamterraforge.tgmsauthanduserservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private User mockUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Test1234")
                .name("Test User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .name("Test User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .createdAt(Instant.now())
                .build();

        // Create mock UserDetails with authorities
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + UserRole.TOURIST.name())
        );
        mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("hashedPassword")
                .authorities(authorities)
                .build();
    }

    @Test
    void register_WithValidData_ShouldCreateUser() {
        // Given
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn("jwt.token.here");

        // When
        var response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(passwordEncoder).encode("Test1234");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(UserDetails.class), any(UUID.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository).existsByEmailIgnoreCase("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnToken() {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Test1234")
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn("jwt.token.here");

        // When
        var response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(userRepository).save(mockUser); // Save for lastLogin update
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowException() {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void register_WithGuideRole_ShouldCreateGuideUser() {
        // Given
        registerRequest.setRole(UserRole.GUIDE);
        User guideUser = User.builder()
                .id(UUID.randomUUID())
                .email("guide@example.com")
                .passwordHash("hashedPassword")
                .name("Guide User")
                .role(UserRole.GUIDE)
                .build();

        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(guideUser);
        when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn("jwt.token.here");

        // When
        var response = authService.register(registerRequest);

        // Then
        assertThat(response.getUser().getRole()).isEqualTo(UserRole.GUIDE);
        verify(jwtService).generateToken(any(UserDetails.class), any(UUID.class));
    }

    @Test
    void register_ShouldEncodePassword() {
        // Given
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("Test1234")).thenReturn("encoded_Test1234");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(any(UserDetails.class), any(UUID.class))).thenReturn("jwt.token.here");

        // When
        authService.register(registerRequest);

        // Then
        verify(passwordEncoder).encode("Test1234");
    }
}
