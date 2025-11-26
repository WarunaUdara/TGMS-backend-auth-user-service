package com.teamterraforge.tgmsauthanduserservice.security;

import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JwtService
 * Tests JWT token generation, validation, and extraction
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails testUserDetails;
    private UUID testUserId;
    private String secretKey;
    private long expirationMs;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Set test values using reflection (production values from application.yml)
        secretKey = "test-secret-key-that-is-at-least-512-bits-long-for-hmac-sha512-algorithm-security-requirements";
        expirationMs = 86400000L; // 24 hours
        
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "expirationMs", expirationMs);
        
        // Initialize the service
        ReflectionTestUtils.invokeMethod(jwtService, "init");
        
        // Create test user
        testUserId = UUID.randomUUID();
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + UserRole.TOURIST.name())
        );
        testUserDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .authorities(authorities)
                .build();
    }

    @Test
    void generateToken_WithValidUserDetails_ShouldGenerateToken() {
        // When
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT format: header.payload.signature
    }

    @Test
    void extractUsername_FromValidToken_ShouldReturnUsername() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void extractUserId_FromValidToken_ShouldReturnUserId() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // When
        UUID extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(testUserId);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // When
        boolean isValid = jwtService.validateToken(token, testUserDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_WithWrongUsername_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);
        
        UserDetails differentUser = User.builder()
                .username("different@example.com")
                .password("password")
                .authorities(testUserDetails.getAuthorities())
                .build();

        // When
        boolean isValid = jwtService.validateToken(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_WithExpiredToken_ShouldThrowException() {
        // Given - Create an expired token manually
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", testUserId.toString())
                .claim("roles", Collections.singletonList("ROLE_TOURIST"))
                .issuedAt(new Date(System.currentTimeMillis() - 86400000L * 2)) // 2 days ago
                .expiration(new Date(System.currentTimeMillis() - 86400000L)) // expired 1 day ago
                .signWith(key)
                .compact();

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken, testUserDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generateToken_WithAdminRole_ShouldIncludeAdminAuthority() {
        // Given
        Collection<GrantedAuthority> adminAuthorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + UserRole.ADMIN.name())
        );
        UserDetails adminUser = User.builder()
                .username("admin@example.com")
                .password("password")
                .authorities(adminAuthorities)
                .build();

        // When
        String token = jwtService.generateToken(adminUser, testUserId);
        Claims claims = extractAllClaims(token);

        // Then
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("roles").toString()).contains("ROLE_ADMIN");
    }

    @Test
    void generateToken_WithGuideRole_ShouldIncludeGuideAuthority() {
        // Given
        Collection<GrantedAuthority> guideAuthorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + UserRole.GUIDE.name())
        );
        UserDetails guideUser = User.builder()
                .username("guide@example.com")
                .password("password")
                .authorities(guideAuthorities)
                .build();

        // When
        String token = jwtService.generateToken(guideUser, testUserId);
        Claims claims = extractAllClaims(token);

        // Then
        assertThat(claims.get("roles")).isNotNull();
        assertThat(claims.get("roles").toString()).contains("ROLE_GUIDE");
    }

    @Test
    void extractExpiration_FromValidToken_ShouldReturnFutureDate() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // When
        Date expiration = extractAllClaims(token).getExpiration();

        // Then
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void isTokenExpired_WithFreshToken_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateToken(testUserDetails, testUserId);

        // When
        Boolean isExpired = (Boolean) ReflectionTestUtils.invokeMethod(jwtService, "isTokenExpired", token);

        // Then
        assertThat(isExpired).isFalse();
    }

    @Test
    void generateToken_ShouldIncludeIssuedAtClaim() {
        // Given & When
        String token = jwtService.generateToken(testUserDetails, testUserId);
        Claims claims = extractAllClaims(token);

        // Then
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getIssuedAt()).isBefore(new Date());
    }

    @Test
    void generateToken_ShouldIncludeUserIdClaim() {
        // Given & When
        String token = jwtService.generateToken(testUserDetails, testUserId);
        Claims claims = extractAllClaims(token);

        // Then
        assertThat(claims.get("userId")).isNotNull();
        assertThat(claims.get("userId").toString()).isEqualTo(testUserId.toString());
    }

    @Test
    void generateToken_WithMultipleRoles_ShouldIncludeAllRoles() {
        // Given - User with multiple authorities
        Collection<GrantedAuthority> multipleAuthorities = java.util.Arrays.asList(
                new SimpleGrantedAuthority("ROLE_TOURIST"),
                new SimpleGrantedAuthority("ROLE_GUIDE")
        );
        UserDetails multiRoleUser = User.builder()
                .username("multi@example.com")
                .password("password")
                .authorities(multipleAuthorities)
                .build();

        // When
        String token = jwtService.generateToken(multiRoleUser, testUserId);
        Claims claims = extractAllClaims(token);

        // Then
        assertThat(claims.get("roles")).isNotNull();
        String rolesString = claims.get("roles").toString();
        assertThat(rolesString).contains("ROLE_TOURIST");
        assertThat(rolesString).contains("ROLE_GUIDE");
    }

    @Test
    void extractUsername_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractUserId_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.extractUserId(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "malformed.token";

        // When & Then
        assertThatThrownBy(() -> jwtService.validateToken(malformedToken, testUserDetails))
                .isInstanceOf(Exception.class);
    }

    /**
     * Helper method to extract all claims from a token for testing
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
