# TGMS Auth & User Service

## Team TerraForge - Tour Guide Management System

### Overview
This is the Authentication and User Management microservice for the TGMS platform. It provides JWT-based authentication, user registration, and user management capabilities.

### Tech Stack
- **Framework**: Spring Boot 4.0.0
- **Language**: Java 22
- **Database**: PostgreSQL 15
- **Security**: Spring Security 6 + JWT
- **Migration**: Flyway
- **Build Tool**: Maven

### Features
- ✅ User Registration with validation
- ✅ JWT-based authentication
- ✅ Role-based access control (ADMIN, TOURIST, GUIDE)
- ✅ Password encryption with BCrypt
- ✅ Database schema versioning with Flyway
- ✅ Global exception handling
- ✅ Production-ready logging
- ✅ Health check endpoints

### Prerequisites
- Java 22 or higher
- Docker & Docker Compose
- Maven 3.8+

### Quick Start

#### 1. Start PostgreSQL with Docker
```powershell
docker-compose up -d
```

This will start PostgreSQL on `localhost:5432` with:
- Database: `tgms_auth`
- Username: `tgms_user`
- Password: `tgms_password`

#### 2. Build the application
```powershell
./mvnw clean package -DskipTests
```

#### 3. Run the application
```powershell
./mvnw spring-boot:run
```

Or run from IDE: `AuthUserServiceApplication.java`

The service will start on **http://localhost:8080**

### API Endpoints

#### Public Endpoints (No Authentication Required)

**Register User**
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123",
  "name": "John Doe",
  "phone": "+94771234567",
  "role": "TOURIST"
}
```

**Login**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "name": "John Doe",
    "role": "TOURIST"
  }
}
```

#### Protected Endpoints (Require Authentication)

**Get Current User Profile**
```http
GET /api/users/me
Authorization: Bearer <your-jwt-token>
```

**Get User by ID (Admin Only)**
```http
GET /api/users/{userId}
Authorization: Bearer <admin-jwt-token>
```

**Check Email Availability**
```http
GET /api/users/check-email?email=test@example.com
```

#### Health Check
```http
GET /actuator/health
```

### Testing with cURL

**Register:**
```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"tourist@tgms.com\",\"password\":\"Test1234\",\"name\":\"Test Tourist\",\"phone\":\"+94771234567\",\"role\":\"TOURIST\"}'
```

**Login:**
```powershell
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"tourist@tgms.com\",\"password\":\"Test1234\"}'
```

**Get Profile:**
```powershell
curl -X GET http://localhost:8080/api/users/me `
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Configuration

#### Environment Variables (Production)
```bash
DB_URL=jdbc:postgresql://your-db-host:5432/tgms_auth
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your-super-secret-256-bit-key
JWT_EXPIRATION_MS=3600000
SERVER_PORT=8080
```

### Database Schema
The service uses Flyway for database migrations. The initial schema (`V1__init_schema.sql`) includes:
- Users table with role-based access
- Guides, Areas, Licenses tables
- Bookings, Reviews, Notifications
- Audit logging
- Outbox pattern for event sourcing

### Security
- Passwords are hashed using BCrypt
- JWT tokens expire after 1 hour (configurable)
- CORS is disabled by default (configure for production)
- CSRF is disabled (stateless JWT authentication)
- Role-based access control with Spring Security

### Project Structure
```
src/main/java/com/teamterraforge/tgmsauthanduserservice/
├── config/              # Configuration classes
├── controller/          # REST controllers
├── dto/                 # Data Transfer Objects
├── entity/              # JPA entities
│   └── enums/          # Enum types
├── exception/           # Exception handlers
├── repository/          # Spring Data repositories
├── security/            # Security components (JWT, filters)
└── service/             # Business logic
```

### Docker Commands

**Start PostgreSQL:**
```powershell
docker-compose up -d
```

**Stop PostgreSQL:**
```powershell
docker-compose down
```

**View PostgreSQL logs:**
```powershell
docker-compose logs -f postgres
```

**Connect to PostgreSQL:**
```powershell
docker exec -it tgms-postgres psql -U tgms_user -d tgms_auth
```

### Troubleshooting

**Port 5432 already in use:**
```powershell
# Stop local PostgreSQL service
net stop postgresql-x64-15

# Or change port in docker-compose.yml
ports:
  - "5433:5432"
```

**Application fails to start:**
1. Ensure PostgreSQL is running: `docker-compose ps`
2. Check database connection in `application.yml`
3. Check logs for detailed error messages

### Development Notes
- Use `dev` profile for development (default)
- Use `prod` profile for production deployment
- Flyway migrations run automatically on startup
- JPA validation mode is set to `validate` (no auto schema changes)

### Next Steps for Production
1. Set up proper JWT secret (minimum 256 bits)
2. Configure CORS for frontend domain
3. Set up HTTPS/TLS
4. Configure rate limiting
5. Set up monitoring and alerting
6. Deploy to EC2 with systemd service
7. Use AWS RDS for PostgreSQL
8. Store secrets in AWS Secrets Manager

### Team
- **Team Name**: TeamTerraForge
- **Project**: Tour Guide Management System (TGMS)
- **Service**: Auth & User Service
- **Assignment**: University Project

---
© 2025 TeamTerraForge. All rights reserved.
