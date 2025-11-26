# TGMS Auth & User Service API Documentation

## Overview
Production-ready RESTful API service for authentication and user management in the Tour Guide Management System (TGMS).

**Base URL:** `http://localhost:8080`  
**Version:** 1.0.0  
**Framework:** Spring Boot 4.0.0 with Java 22

---

## Authentication

All endpoints except those listed as "Public" require JWT authentication.

**Header Format:**
```
Authorization: Bearer <JWT_TOKEN>
```

---

## API Endpoints

### Authentication APIs

#### 1. Register New User
**POST** `/api/auth/register`

Register a new user account.

**Access:** Public

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "name": "John Doe",
  "phone": "+1234567890",
  "role": "ADMIN"
}
```

**Validation Rules:**
- Email: Valid format, unique
- Password: 8-100 chars, must contain uppercase, lowercase, and digit
- Name: 2-200 chars
- Phone: 10-30 chars
- Role: TOURIST, GUIDE, or ADMIN

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "name": "John Doe",
    "phone": "+1234567890",
    "role": "TOURIST",
    "createdAt": "2024-01-15T10:30:00Z",
    "lastLogin": null
  }
}
```

---

#### 2. Login
**POST** `/api/auth/login`

Authenticate user and receive JWT token.

**Access:** Public

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "email": "user@example.com",
    "name": "John Doe",
    "phone": "+1234567890",
    "role": "TOURIST",
    "createdAt": "2024-01-15T10:30:00Z",
    "lastLogin": "2024-01-16T09:15:00Z"
  }
}
```

---

#### 3. Health Check
**GET** `/api/auth/health`

Check API health status.

**Access:** Public

**Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2024-01-16T10:30:00Z"
}
```

---

### User Profile APIs

#### 4. Get Current User Profile
**GET** `/api/users/me`

Get authenticated user's profile.

**Access:** Authenticated users

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
  "phone": "+1234567890",
  "role": "TOURIST",
  "createdAt": "2024-01-15T10:30:00Z",
  "lastLogin": "2024-01-16T09:15:00Z"
}
```

---

#### 5. Update Profile
**PUT** `/api/users/me`

Update authenticated user's profile.

**Access:** Authenticated users

**Request Body:**
```json
{
  "name": "John Updated Doe",
  "phone": "+9876543210"
}
```

**Validation Rules:**
- Name: 2-200 chars (optional)
- Phone: 10-30 chars (optional)

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Updated Doe",
  "phone": "+9876543210",
  "role": "TOURIST",
  "createdAt": "2024-01-15T10:30:00Z",
  "lastLogin": "2024-01-16T09:15:00Z"
}
```

---

#### 6. Change Password
**POST** `/api/users/change-password`

Change authenticated user's password.

**Access:** Authenticated users

**Request Body:**
```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewPassword456",
  "confirmPassword": "NewPassword456"
}
```

**Validation Rules:**
- Current password: Required
- New password: 8-100 chars, must contain uppercase, lowercase, and digit
- Passwords must match
- New password must be different from current

**Response (200 OK):**
```json
{
  "message": "Password changed successfully"
}
```

---

#### 7. Delete Account
**DELETE** `/api/users/me`

Delete authenticated user's account.

**Access:** Authenticated users

**Response (200 OK):**
```json
{
  "message": "Account deleted successfully"
}
```

⚠️ **Warning:** This action is permanent and cannot be undone.

---

### Password Recovery APIs

#### 8. Forgot Password
**POST** `/api/users/forgot-password`

Initiate password reset process.

**Access:** Public

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "message": "Password reset instructions have been sent to your email",
  "email": "user@example.com"
}
```

📧 **Note:** In production, a reset link will be sent via email. The token expires in 1 hour.

---

#### 9. Reset Password
**POST** `/api/users/reset-password`

Reset password using token from email.

**Access:** Public

**Request Body:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "newPassword": "NewPassword123",
  "confirmPassword": "NewPassword123"
}
```

**Validation Rules:**
- Token: Valid, not expired (1 hour validity)
- New password: 8-100 chars, must contain uppercase, lowercase, and digit
- Passwords must match

**Response (200 OK):**
```json
{
  "message": "Password reset successfully"
}
```

---

### User Information APIs

#### 10. Get Public Profile
**GET** `/api/users/{id}/public-profile`

Get limited public information about a user.

**Access:** Public

**Path Parameters:**
- `id` (UUID): User ID

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "John Doe",
  "role": "GUIDE",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

ℹ️ **Note:** Email and phone are not included in public profiles for privacy.

---

#### 11. Check Email Availability
**GET** `/api/users/check-email?email={email}`

Check if an email address is already registered.

**Access:** Public/required auth token

**Query Parameters:**
- `email` (string): Email address to check

**Response (200 OK):**
```json
true
```
Returns `true` if email exists, `false` if available.

---

### Admin APIs

#### 12. Get User by ID
**GET** `/api/users/{id}`

Get full user information by ID.

**Access:** Admin only

**Path Parameters:**
- `id` (UUID): User ID

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com",
  "name": "John Doe",
  "phone": "+1234567890",
  "role": "TOURIST",
  "createdAt": "2024-01-15T10:30:00Z",
  "lastLogin": "2024-01-16T09:15:00Z"
}
```

---

#### 13. Get All Users (Paginated)
**GET** `/api/users/admin/all`

Get paginated list of all users with sorting.

**Access:** Admin only

**Query Parameters:**
- `page` (int, default: 0): Page number (0-indexed)
- `size` (int, default: 20): Page size
- `sortBy` (string, default: "createdAt"): Sort field (createdAt, name, email, role)
- `sortDirection` (string, default: "DESC"): Sort direction (ASC or DESC)

**Example Request:**
```
GET /api/users/admin/all?page=0&size=20&sortBy=createdAt&sortDirection=DESC
```

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "email": "user1@example.com",
      "name": "User One",
      "phone": "+1234567890",
      "role": "TOURIST",
      "createdAt": "2024-01-16T10:30:00Z",
      "lastLogin": "2024-01-16T11:00:00Z"
    },
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "email": "user2@example.com",
      "name": "User Two",
      "phone": "+1234567891",
      "role": "GUIDE",
      "createdAt": "2024-01-15T09:20:00Z",
      "lastLogin": "2024-01-16T08:45:00Z"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 50,
  "totalPages": 3,
  "first": true,
  "last": false,
  "empty": false
}
```

---

## Error Responses

All errors follow a consistent format:

### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2024-01-16T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "validationErrors": {
    "email": "Email must be valid",
    "password": "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
  },
  "path": "/api/auth/register"
}
```

### Unauthorized (401)
```json
{
  "timestamp": "2024-01-16T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

### Forbidden (403)
```json
{
  "timestamp": "2024-01-16T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to access this resource",
  "path": "/api/users/admin/all"
}
```

### Not Found (404)
```json
{
  "timestamp": "2024-01-16T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 123e4567-e89b-12d3-a456-426614174000",
  "path": "/api/users/123e4567-e89b-12d3-a456-426614174000"
}
```

### Internal Server Error (500)
```json
{
  "timestamp": "2024-01-16T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/users/me"
}
```

---

## Security Best Practices

### JWT Token Management
- **Expiration:** Tokens expire after 24 hours
- **Storage:** Store tokens securely (HttpOnly cookies or secure storage)
- **Transmission:** Always use HTTPS in production
- **Refresh:** Re-authenticate when token expires

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one digit (0-9)
- Maximum 100 characters

### Rate Limiting
Consider implementing rate limiting for:
- Login attempts: 5 attempts per 15 minutes
- Password reset requests: 3 attempts per hour
- Registration: 3 attempts per hour per IP

---

## Testing

### cURL Examples

#### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123",
    "name": "Test User",
    "phone": "+1234567890",
    "role": "TOURIST"
  }'
```

#### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Password123"
  }'
```

#### Get Current User (with JWT)
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Update Profile
```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name",
    "phone": "+9876543210"
  }'
```

#### Change Password
```bash
curl -X POST http://localhost:8080/api/users/change-password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Password123",
    "newPassword": "NewPassword456",
    "confirmPassword": "NewPassword456"
  }'
```

---

## Production Checklist

- ✅ All endpoints use proper HTTP methods (GET, POST, PUT, DELETE)
- ✅ Input validation on all request bodies
- ✅ Consistent error responses
- ✅ JWT authentication with secure signing
- ✅ Role-based access control (RBAC)
- ✅ Password hashing with BCrypt
- ✅ SQL injection prevention (JPA/Hibernate)
- ✅ CORS configuration
- ✅ Comprehensive logging
- ✅ Transaction management
- ✅ Pagination for list endpoints
- ✅ Unit and integration tests

### Additional Production Recommendations
1. **Email Service:** Integrate email service for password reset
2. **Rate Limiting:** Add rate limiting middleware
3. **Monitoring:** Set up APM (Application Performance Monitoring)
4. **Logging:** Centralized logging (ELK stack, CloudWatch)
5. **HTTPS:** Enforce HTTPS in production
6. **Database:** Connection pooling and optimization
7. **Caching:** Redis for session management
8. **CI/CD:** Automated testing and deployment pipeline

---

## Contact & Support

For issues or questions, contact the TGMS development team.

**Version:** 1.0.0  
**Last Updated:** January 2024
