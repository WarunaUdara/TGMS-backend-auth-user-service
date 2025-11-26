package com.teamterraforge.tgmsauthanduserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamterraforge.tgmsauthanduserservice.dto.LoginRequest;
import com.teamterraforge.tgmsauthanduserservice.dto.RegisterRequest;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests the complete authentication flow including database interactions
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @Test
    void register_WithValidData_ShouldReturnJwtToken() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@test.com")
                .password("Test1234")
                .name("John Doe")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresIn", is(86400)))
                .andExpect(jsonPath("$.user.email", is("newuser@test.com")))
                .andExpect(jsonPath("$.user.name", is("John Doe")))
                .andExpect(jsonPath("$.user.role", is("TOURIST")));
    }

    @Test
    void register_WithDuplicateEmail_ShouldReturn400() throws Exception {
        // Given - Register first user
        RegisterRequest firstUser = RegisterRequest.builder()
                .email("duplicate@test.com")
                .password("Test1234")
                .name("First User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)));

        // When & Then - Try to register with same email
        RegisterRequest duplicateUser = RegisterRequest.builder()
                .email("duplicate@test.com")
                .password("Different123")
                .name("Second User")
                .phone("+94777654321")
                .role(UserRole.GUIDE)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void register_WithInvalidEmail_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")
                .password("Test1234")
                .name("John Doe")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()));
    }

    @Test
    void register_WithWeakPassword_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                .password("weak")
                .name("John Doe")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password", notNullValue()));
    }

    @Test
    void register_WithInvalidPhone_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                .password("Test1234")
                .name("John Doe")
                .phone("invalid")
                .role(UserRole.TOURIST)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.phone", notNullValue()));
    }

    @Test
    void register_WithMissingFields_ShouldReturn400() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                // Missing password, name
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors", aMapWithSize(greaterThan(0))));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnJwtToken() throws Exception {
        // Given - Register a user first
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("login@test.com")
                .password("Test1234")
                .name("Login User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When & Then - Login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("login@test.com")
                .password("Test1234")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("login@test.com")));
    }

    @Test
    void login_WithInvalidPassword_ShouldReturn401() throws Exception {
        // Given - Register a user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("user@test.com")
                .password("Test1234")
                .name("Test User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When & Then - Try to login with wrong password
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@test.com")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithNonExistentUser_ShouldReturn401() throws Exception {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent@test.com")
                .password("Test1234")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_WithInvalidEmailFormat_ShouldReturn400() throws Exception {
        // Given
        LoginRequest loginRequest = LoginRequest.builder()
                .email("invalid-email")
                .password("Test1234")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email", notNullValue()));
    }

    @Test
    void health_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Auth Service is running"));
    }

    @Test
    void register_AsGuide_ShouldSucceed() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("guide@test.com")
                .password("Guide1234")
                .name("Tour Guide")
                .phone("+94771234567")
                .role(UserRole.GUIDE)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("GUIDE")));
    }

    @Test
    void register_AsAdmin_ShouldSucceed() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@test.com")
                .password("Admin1234")
                .name("System Admin")
                .phone("+94771234567")
                .role(UserRole.ADMIN)
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role", is("ADMIN")));
    }
}
