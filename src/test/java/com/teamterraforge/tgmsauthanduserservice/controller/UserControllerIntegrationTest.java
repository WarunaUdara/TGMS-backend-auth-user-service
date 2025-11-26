package com.teamterraforge.tgmsauthanduserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamterraforge.tgmsauthanduserservice.dto.RegisterRequest;
import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
import com.teamterraforge.tgmsauthanduserservice.repository.UserRepository;
import com.teamterraforge.tgmsauthanduserservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController
 * Tests protected endpoints with JWT authentication
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String touristToken;
    private String adminToken;
    private UUID touristId;
    private UUID adminId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();

        // Create tourist user
        RegisterRequest touristRequest = RegisterRequest.builder()
                .email("tourist@test.com")
                .password("Test1234")
                .name("Tourist User")
                .phone("+94771234567")
                .role(UserRole.TOURIST)
                .build();

        MvcResult touristResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(touristRequest)))
                .andReturn();

        String touristResponse = touristResult.getResponse().getContentAsString();
        touristToken = objectMapper.readTree(touristResponse).get("accessToken").asText();
        touristId = UUID.fromString(objectMapper.readTree(touristResponse).get("user").get("id").asText());

        // Create admin user
        RegisterRequest adminRequest = RegisterRequest.builder()
                .email("admin@test.com")
                .password("Admin1234")
                .name("Admin User")
                .phone("+94777654321")
                .role(UserRole.ADMIN)
                .build();

        MvcResult adminResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRequest)))
                .andReturn();

        String adminResponse = adminResult.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminResponse).get("accessToken").asText();
        adminId = UUID.fromString(objectMapper.readTree(adminResponse).get("user").get("id").asText());
    }

    @Test
    void getCurrentUser_WithValidToken_ShouldReturnUserDetails() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + touristToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("tourist@test.com")))
                .andExpect(jsonPath("$.name", is("Tourist User")))
                .andExpect(jsonPath("$.role", is("TOURIST")))
                .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void getCurrentUser_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUser_WithInvalidToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_AsAdmin_ShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/" + touristId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("tourist@test.com")))
                .andExpect(jsonPath("$.name", is("Tourist User")));
    }

    @Test
    void getUserById_AsTourist_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + touristToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_WithNonExistentId_ShouldReturn404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/users/" + nonExistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users/" + touristId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkEmail_WithExistingEmail_ShouldReturnTrue() throws Exception {
        mockMvc.perform(get("/api/users/check-email")
                        .param("email", "tourist@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    void checkEmail_WithNonExistentEmail_ShouldReturnFalse() throws Exception {
        mockMvc.perform(get("/api/users/check-email")
                        .param("email", "nonexistent@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(false)));
    }

    @Test
    void checkEmail_CaseInsensitive_ShouldWork() throws Exception {
        mockMvc.perform(get("/api/users/check-email")
                        .param("email", "TOURIST@TEST.COM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    void jwtToken_ShouldContainCorrectClaims() throws Exception {
        // Verify token contains user ID and role
        UUID extractedUserId = jwtService.extractUserId(touristToken);
        String username = jwtService.extractUsername(touristToken);

        assertThat(extractedUserId).isEqualTo(touristId);
        assertThat(username).isEqualTo("tourist@test.com");
    }

    @Test
    void expiredToken_ShouldBeRejected() throws Exception {
        // This test verifies the token expiration mechanism
        // In a real scenario, you'd generate an expired token
        // For now, we just verify that invalid tokens are rejected
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"))
                .andExpect(status().isUnauthorized());
    }
}
