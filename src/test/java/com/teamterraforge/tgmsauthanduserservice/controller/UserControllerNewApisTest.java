// package com.teamterraforge.tgmsauthanduserservice.controller;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.teamterraforge.tgmsauthanduserservice.dto.*;
// import com.teamterraforge.tgmsauthanduserservice.entity.enums.UserRole;
// import com.teamterraforge.tgmsauthanduserservice.security.CustomUserDetailsService;
// import com.teamterraforge.tgmsauthanduserservice.security.JwtAuthenticationEntryPoint;
// import com.teamterraforge.tgmsauthanduserservice.security.JwtService;
// import com.teamterraforge.tgmsauthanduserservice.service.UserService;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.http.MediaType;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.context.bean.override.mockito.MockitoBean;
// import org.springframework.test.web.servlet.MockMvc;

// import java.time.Instant;
// import java.util.Arrays;
// import java.util.UUID;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.*;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /**
//  * Integration tests for UserController - New API endpoints
//  */
// @WebMvcTest(UserController.class)
// @AutoConfigureMockMvc(addFilters = false)
// @DisplayName("UserController - New APIs Integration Tests")
// class UserControllerNewApisTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Autowired
//     private ObjectMapper objectMapper;

//     @MockitoBean
//     private UserService userService;

//     @MockitoBean
//     private JwtService jwtService;

//     @MockitoBean
//     private CustomUserDetailsService customUserDetailsService;

//     @MockitoBean
//     private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

//     private UUID testUserId = UUID.randomUUID();

//     // ===== UPDATE PROFILE TESTS =====

//     @Test
//     @WithMockUser(username = "test@example.com", roles = {"TOURIST"})
//     @DisplayName("PUT /api/users/me - Should update profile successfully")
//     void shouldUpdateProfileSuccessfully() throws Exception {
//         // Given
//         UpdateProfileRequest request = UpdateProfileRequest.builder()
//                 .name("Updated Name")
//                 .phone("+1234567890")
//                 .build();

//         UserResponse response = UserResponse.builder()
//                 .id(testUserId)
//                 .email("test@example.com")
//                 .name("Updated Name")
//                 .phone("+1234567890")
//                 .role(UserRole.TOURIST)
//                 .createdAt(Instant.now())
//                 .build();

//         when(userService.getUserByEmail("test@example.com"))
//                 .thenReturn(response);
//         when(userService.updateProfile(eq(testUserId), any(UpdateProfileRequest.class)))
//                 .thenReturn(response);

//         // When & Then
//         mockMvc.perform(put("/api/users/me")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.name").value("Updated Name"))
//                 .andExpect(jsonPath("$.phone").value("+1234567890"));

//         verify(userService).updateProfile(eq(testUserId), any(UpdateProfileRequest.class));
//     }

//     @Test
//     @WithMockUser(username = "test@example.com", roles = {"TOURIST"})
//     @DisplayName("PUT /api/users/me - Should reject invalid phone number")
//     void shouldRejectInvalidPhoneNumber() throws Exception {
//         // Given
//         UpdateProfileRequest request = UpdateProfileRequest.builder()
//                 .name("Test User")
//                 .phone("123") // Too short
//                 .build();

//         // When & Then
//         mockMvc.perform(put("/api/users/me")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isBadRequest());
//     }

//     // ===== CHANGE PASSWORD TESTS =====

//     @Test
//     @WithMockUser(username = "test@example.com", roles = {"TOURIST"})
//     @DisplayName("POST /api/users/change-password - Should change password successfully")
//     void shouldChangePasswordSuccessfully() throws Exception {
//         // Given
//         ChangePasswordRequest request = ChangePasswordRequest.builder()
//                 .currentPassword("OldPassword123")
//                 .newPassword("NewPassword123")
//                 .confirmPassword("NewPassword123")
//                 .build();

//         UserResponse userResponse = UserResponse.builder()
//                 .id(testUserId)
//                 .email("test@example.com")
//                 .name("Test User")
//                 .role(UserRole.TOURIST)
//                 .build();

//         when(userService.getUserByEmail("test@example.com")).thenReturn(userResponse);
//         doNothing().when(userService).changePassword(eq(testUserId), any(ChangePasswordRequest.class));

//         // When & Then
//         mockMvc.perform(post("/api/users/change-password")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.message").value("Password changed successfully"));

//         verify(userService).changePassword(eq(testUserId), any(ChangePasswordRequest.class));
//     }

//     @Test
//     @WithMockUser(username = "test@example.com", roles = {"TOURIST"})
//     @DisplayName("POST /api/users/change-password - Should reject weak password")
//     void shouldRejectWeakPassword() throws Exception {
//         // Given
//         ChangePasswordRequest request = ChangePasswordRequest.builder()
//                 .currentPassword("OldPassword123")
//                 .newPassword("weak") // No uppercase, no digit
//                 .confirmPassword("weak")
//                 .build();

//         // When & Then
//         mockMvc.perform(post("/api/users/change-password")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isBadRequest());
//     }

//     // ===== FORGOT PASSWORD TESTS =====

//     @Test
//     @DisplayName("POST /api/users/forgot-password - Should send reset email")
//     void shouldSendPasswordResetEmail() throws Exception {
//         // Given
//         ForgotPasswordRequest request = ForgotPasswordRequest.builder()
//                 .email("test@example.com")
//                 .build();

//         PasswordResetResponse response = PasswordResetResponse.builder()
//                 .message("Password reset instructions have been sent to your email")
//                 .email("test@example.com")
//                 .build();

//         when(userService.forgotPassword(any(ForgotPasswordRequest.class)))
//                 .thenReturn(response);

//         // When & Then
//         mockMvc.perform(post("/api/users/forgot-password")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.message").exists())
//                 .andExpect(jsonPath("$.email").value("test@example.com"));
//     }

//     @Test
//     @DisplayName("POST /api/users/forgot-password - Should reject invalid email")
//     void shouldRejectInvalidEmailForForgotPassword() throws Exception {
//         // Given
//         ForgotPasswordRequest request = ForgotPasswordRequest.builder()
//                 .email("invalid-email")
//                 .build();

//         // When & Then
//         mockMvc.perform(post("/api/users/forgot-password")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isBadRequest());
//     }

//     // ===== RESET PASSWORD TESTS =====

//     @Test
//     @DisplayName("POST /api/users/reset-password - Should reset password with valid token")
//     void shouldResetPasswordWithValidToken() throws Exception {
//         // Given
//         ResetPasswordRequest request = ResetPasswordRequest.builder()
//                 .token("validToken123")
//                 .newPassword("NewPassword123")
//                 .confirmPassword("NewPassword123")
//                 .build();

//         doNothing().when(userService).resetPassword(any(ResetPasswordRequest.class));

//         // When & Then
//         mockMvc.perform(post("/api/users/reset-password")
//                         .with(csrf())
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.message").value("Password reset successfully"));
//     }

//     // ===== DELETE ACCOUNT TESTS =====

//     @Test
//     @WithMockUser(username = "test@example.com", roles = {"TOURIST"})
//     @DisplayName("DELETE /api/users/me - Should delete account successfully")
//     void shouldDeleteAccountSuccessfully() throws Exception {
//         // Given
//         UserResponse userResponse = UserResponse.builder()
//                 .id(testUserId)
//                 .email("test@example.com")
//                 .name("Test User")
//                 .role(UserRole.TOURIST)
//                 .build();

//         when(userService.getUserByEmail("test@example.com")).thenReturn(userResponse);
//         doNothing().when(userService).deleteAccount(testUserId);

//         // When & Then
//         mockMvc.perform(delete("/api/users/me")
//                         .with(csrf()))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.message").value("Account deleted successfully"));

//         verify(userService).deleteAccount(testUserId);
//     }

//     // ===== PUBLIC PROFILE TESTS =====

//     @Test
//     @DisplayName("GET /api/users/{id}/public-profile - Should get public profile")
//     void shouldGetPublicProfile() throws Exception {
//         // Given
//         UUID userId = UUID.randomUUID();
//         UserResponse response = UserResponse.builder()
//                 .id(userId)
//                 .name("John Doe")
//                 .role(UserRole.GUIDE)
//                 .createdAt(Instant.now())
//                 .build();

//         when(userService.getPublicProfile(userId)).thenReturn(response);

//         // When & Then
//         mockMvc.perform(get("/api/users/{id}/public-profile", userId))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.id").value(userId.toString()))
//                 .andExpect(jsonPath("$.name").value("John Doe"))
//                 .andExpect(jsonPath("$.role").value("GUIDE"))
//                 .andExpect(jsonPath("$.email").doesNotExist())
//                 .andExpect(jsonPath("$.phone").doesNotExist());
//     }

//     // ===== ADMIN - GET ALL USERS TESTS =====

//     @Test
//     @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
//     @DisplayName("GET /api/users/admin/all - Should get paginated users list")
//     void shouldGetPaginatedUsersList() throws Exception {
//         // Given
//         UserResponse user1 = UserResponse.builder()
//                 .id(UUID.randomUUID())
//                 .email("user1@test.com")
//                 .name("User 1")
//                 .role(UserRole.TOURIST)
//                 .createdAt(Instant.now())
//                 .build();

//         UserResponse user2 = UserResponse.builder()
//                 .id(UUID.randomUUID())
//                 .email("user2@test.com")
//                 .name("User 2")
//                 .role(UserRole.GUIDE)
//                 .createdAt(Instant.now())
//                 .build();

//         PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder()
//                 .content(Arrays.asList(user1, user2))
//                 .pageNumber(0)
//                 .pageSize(20)
//                 .totalElements(2)
//                 .totalPages(1)
//                 .first(true)
//                 .last(true)
//                 .empty(false)
//                 .build();

//         when(userService.getAllUsers(any(PageRequest.class))).thenReturn(pageResponse);

//         // When & Then
//         mockMvc.perform(get("/api/users/admin/all")
//                         .param("page", "0")
//                         .param("size", "20")
//                         .param("sortBy", "createdAt")
//                         .param("sortDirection", "DESC"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.content").isArray())
//                 .andExpect(jsonPath("$.content.length()").value(2))
//                 .andExpect(jsonPath("$.totalElements").value(2))
//                 .andExpect(jsonPath("$.totalPages").value(1))
//                 .andExpect(jsonPath("$.pageNumber").value(0))
//                 .andExpect(jsonPath("$.pageSize").value(20));
//     }

//     @Test
//     @WithMockUser(username = "user@example.com", roles = {"TOURIST"})
//     @DisplayName("GET /api/users/admin/all - Should deny access for non-admin")
//     void shouldDenyAccessForNonAdmin() throws Exception {
//         // When & Then
//         mockMvc.perform(get("/api/users/admin/all"))
//                 .andExpect(status().isForbidden());
//     }

//     @Test
//     @DisplayName("GET /api/users/admin/all - Should deny access for unauthenticated user")
//     void shouldDenyAccessForUnauthenticated() throws Exception {
//         // When & Then
//         mockMvc.perform(get("/api/users/admin/all"))
//                 .andExpect(status().isUnauthorized());
//     }
// }
