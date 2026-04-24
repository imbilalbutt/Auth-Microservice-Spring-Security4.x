# High-Level Software Architecture: Spring Auth Dev

## 1. System Overview

**Spring Auth Dev** is a comprehensive authentication and authorization service built with Spring Boot 4.0.1, providing dual authentication mechanisms (JWT and Redis Session-based) for both REST APIs and traditional web applications.

---

## 2. Architecture Pattern

**Layered Architecture (N-Tier)** with the following layers:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (Controllers: REST API + Web MVC + Thymeleaf Views)    │
├─────────────────────────────────────────────────────────┤
│                    Security Layer                        │
│        (Filters, Security Config, Authentication)        │
├─────────────────────────────────────────────────────────┤
│                    Service Layer                         │
│         (Business Logic, Use Cases, Validators)          │
├─────────────────────────────────────────────────────────┤
│                 Persistence Layer                        │
│        (JPA Repositories, Entity Management)             │
├─────────────────────────────────────────────────────────┤
│                    Infrastructure Layer                  │
│      (Redis, PostgreSQL, External Integrations)          │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Core Components

### 3.1 Presentation Layer

**Controllers:**

| Controller | Path | Type | Purpose |
|------------|------|------|---------|
| `ApiAuthController` | `/api/v1/auth` | REST | JWT-based authentication endpoints |
| `SessionController` | `/api/v2/session` | REST | Redis session management endpoints |
| `WebUIServletController` | `/ui/auth/servlet` | MVC | Servlet session-based web UI |
| `WebUIRedisController` | `/ui/auth/redis` | MVC | Redis session-based web UI |

**Request/Response DTOs:**
- `RegisterRequest` - User registration data
- `AuthenticationRequest` - Login credentials
- `AuthenticationResponse` - Authentication result with token/session
- `UserDTO` - User data transfer object (placeholder)
- `SessionInfo` - Session metadata

---

### 3.2 Security Layer

**Security Configuration (`SecurityConfig.java`):**

Two security filter chains with ordered precedence:

1. **API Security Chain (Order 1)**
   - Path: `/api/**`
   - Session Policy: STATELESS
   - Authentication: JWT Token
   - Filter: `JwtAuthenticationFilter`

2. **UI Security Chain (Order 2)**
   - Paths: `/ui/**`, `/`, `/home`, `/login`, `/register`
   - Session Policy: ALWAYS (max 1 concurrent session)
   - Authentication: Redis Session
   - Filter: `SessionAuthenticationFilter`
   - Form Login/Logout configured

**Security Filters:**

| Filter | Type | Responsibility |
|--------|------|----------------|
| `JwtAuthenticationFilter` | `OncePerRequestFilter` | Validates JWT tokens from Authorization header |
| `SessionAuthenticationFilter` | `OncePerRequestFilter` | Validates Redis sessions from SESSION_ID cookie |

**Authentication Infrastructure (`ApplicationConfig.java`):**
- `UserDetailsService` - Loads users from database by email
- `AuthenticationProvider` (DaoAuthenticationProvider) - Username/password authentication
- `AuthenticationManager` - Orchestrates authentication process
- `PasswordEncoder` (BCrypt) - Password hashing

---

### 3.3 Service Layer

**Service Interfaces & Implementations:**

| Service | Implementation | Authentication Type |
|---------|---------------|---------------------|
| `ApiUserService` | `ApiUserServiceImpl` | JWT-based |
| `WebUserService` | `WebUserServiceImpl` | Redis Session-based |

**Service Responsibilities:**
- User registration with validation
- User authentication
- JWT token generation and validation
- Redis session registration and management
- User account creation

**Domain Services:**
- `JwtService` - JWT token generation, validation, and claim extraction

---

### 3.4 Persistence Layer

**Entity:**
- `User` - JPA entity implementing `UserDetails` and `Principal`
  - Fields: id, firstname, lastname, email, password, role, enabled, locked, createdDate, lastModifiedDate, dateOfBirth
  - Location: Table `_user`

**Repository:**
- `UserRepository` - Spring Data JPA repository
  - Methods: `findByEmail()`, `existsByEmail()`, CRUD operations

**Enums:**
- `Role` - USER, ADMIN

---

### 3.5 Infrastructure Layer

**Data Stores:**

| Store | Technology | Purpose | Configuration |
|-------|-----------|---------|---------------|
| Primary DB | PostgreSQL 9.3 | User data persistence | Port: 5433 (host), 5432 (container) |
| Cache/Session | Redis 7 | Session storage | Port: 6379 |

**Redis Configuration (`RedisConfiguration.java`):**
- Connection: LettuceConnectionFactory
- Template: `RedisTemplate<String, Object>`
- Session Storage: Key-value (sessionId → username)

**Session Registry (`SessionRegistry.java`):**
- Generates unique session IDs (Base64-encoded UUID)
- Stores sessions in Redis with 24-hour TTL
- Fallback to in-memory HashMap if Redis fails
- Session refresh and invalidation support

---

## 4. Authentication Flows

### 4.1 JWT Authentication Flow (API)

```
┌──────┐              ┌─────────────┐              ┌──────────┐
│Client│              │ApiAuthCtrl  │              │ApiUserService│
└──┬───┘              └──────┬──────┘              └────┬─────┘
   │                         │                          │
   │ POST /api/v1/auth/register ──────────────────────► │
   │                         │                          │
   │                         │ 1. Check user exists     │
   │                         │ 2. Create user (BCrypt)  │
   │                         │ 3. Generate JWT token    │
   │                         │                          │
   │◄───────────────────────────────────────────────────│
   │  { token, tokenType, email, firstname, role }      │
   │                         │                          │
   │                         │                          │
   │ Future Request         │                          │
   │ Authorization: Bearer <token> ────────────────────►│
   │                         │                          │
   │                         │ JwtAuthenticationFilter  │
   │                         │ 1. Extract token         │
   │                         │ 2. Validate with JwtService│
   │                         │ 3. Load user details     │
   │                         │ 4. Set SecurityContext   │
   └─────────────────────────┴──────────────────────────┘
```

### 4.2 Redis Session Authentication Flow (Web UI)

```
┌──────┐         ┌──────────────┐         ┌─────────────┐        ┌───────┐
│Client│         │WebUIRedisCtrl│         │WebUserService│        │ Redis │
└──┬───┘         └──────┬───────┘         └──────┬──────┘        └───┬───┘
   │                    │                        │                   │
   │ POST /ui/auth/redis/login ────────────────►│                   │
   │ { email, password }│                        │                   │
   │                    │ 1. Authenticate (AuthenticationManager)    │
   │                    │                        │                   │
   │                    │ 2. Create session ────►│                   │
   │                    │                        │ registerSession() │
   │                    │                        │──────────────────►│
   │                    │                        │    sessionId      │
   │                    │                        │◄──────────────────│
   │                    │                        │                   │
   │◄───────────────────│                        │                   │
   │ Set-Cookie: SESSION_ID=<sessionId>          │                   │
   │                    │                        │                   │
   │ Subsequent Requests│                        │                   │
   │ Cookie: SESSION_ID │                        │                   │
   │───────────────────►│                        │                   │
   │                    │ SessionAuthenticationFilter               │
   │                    │ 1. Extract sessionId from cookie           │
   │                    │ 2. Validate in Redis ─────────────────────►│
   │                    │    username                                │
   │                    │◄───────────────────────────────────────────│
   │                    │ 3. Load user details                       │
   │                    │ 4. Set SecurityContext                     │
   │                    │ 5. Refresh session TTL                     │
   └────────────────────┴────────────────────────────────────────────┘
```

---

## 5. Data Models

### 5.1 User Entity

```
User (Principal, UserDetails)
├── Integer id (PK, auto-generated)
├── String firstname
├── String lastname
├── LocalDate dateOfBirth
├── String email (unique)
├── String password (BCrypt encoded)
├── boolean enabled
├── boolean locked
├── LocalDateTime createdDate (auto)
├── LocalDateTime lastModifiedDate (auto)
└── Role role (enum: USER, ADMIN)
```

### 5.2 SessionInfo

```
SessionInfo
├── String username
├── String displayName
├── String role
├── LocalDateTime createdAt
└── LocalDateTime lastAccessedAt
```

---

## 6. API Endpoints

### 6.1 REST API (JWT)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/authenticate` | Public | Login and get JWT token |
| POST | `/api/v1/auth/create-account` | Public | Alternative account creation |
| POST | `/api/v2/session/login` | Public | Session-based login |
| POST | `/api/v2/session/logout` | Session | Invalidate session |
| POST | `/api/v2/session/refresh` | Session | Refresh session TTL |

### 6.2 Web UI (Redis Session)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/ui/auth/redis/register` | Public | Show registration form |
| POST | `/ui/auth/redis/register` | Public | Process registration |
| GET | `/ui/auth/redis/login` | Public | Show login form |
| POST | `/ui/auth/redis/login` | Public | Process login |
| GET | `/ui/auth/redis/dashboard` | Session | Dashboard page |
| GET | `/ui/auth/redis/profile` | Session | Profile page |
| GET | `/ui/auth/redis/logout` | Session | Logout |

### 6.3 Web UI (Servlet Session)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/ui/auth/servlet/register` | Public | Show registration form |
| POST | `/ui/auth/servlet/register` | Public | Process registration |
| GET | `/ui/auth/servlet/login` | Public | Show login form |
| POST | `/ui/auth/servlet/login` | Public | Process login |
| GET | `/ui/auth/servlet/dashboard` | Cookie | Dashboard page |
| GET | `/ui/auth/servlet/profile` | Cookie | Profile page |
| GET | `/ui/auth/servlet/logout` | Cookie | Logout |

---

## 7. Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Network                        │
│                  (shared-internal)                       │
│                                                          │
│  ┌──────────────┐    ┌──────────────┐    ┌────────────┐ │
│  │   app        │───►│     db       │    │   redis    │ │
│  │  (8081)      │    │  (5432)      │    │   (6379)   │ │
│  │              │    │              │    │            │ │
│  │ Spring Boot  │    │  PostgreSQL  │    │   Redis 7  │ │
│  │  Application │    │   9.3        │    │            │ │
│  └──────┬───────┘    └──────────────┘    └────────────┘ │
│         │                                                │
└─────────┼────────────────────────────────────────────────┘
          │
          │ Port 8081
          ▼
┌─────────────────┐
│   Host Machine  │
│  (localhost)    │
└─────────────────┘
```

**Container Configuration:**
- **app**: Spring Boot application (port 8081)
- **db**: PostgreSQL 9.3 (internal port 5432, host port 5433)
- **redis-service**: Redis 7 Alpine (port 6379)

**Environment Variables:**
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/db
SPRING_DATASOURCE_USERNAME: admin_user
SPRING_DATASOURCE_PASSWORD: password
SPRING_DATA_REDIS_HOST: redis-service
SPRING_DATA_REDIS_PORT: 6379
JWT_SECRET: <base64-encoded-secret>
APPLICATION_SECURITY_JWT_EXPIRATION: 86400000 (24 hours)
```

---

## 8. Security Architecture

### 8.1 Authentication Mechanisms

**Dual Authentication Strategy:**

1. **JWT (Stateless)**
   - Used for: REST API endpoints (`/api/**`)
   - Token location: Authorization header (`Bearer <token>`)
   - Expiration: 24 hours
   - Algorithm: HS256
   - Stateless - no server-side storage

2. **Redis Session (Stateful)**
   - Used for: Web UI (`/ui/**`)
   - Session ID location: HttpOnly cookie (`SESSION_ID`)
   - Expiration: 24 hours (refreshable)
   - Server-side session storage in Redis
   - Maximum 1 concurrent session per user

### 8.2 Password Security
- Encoding: BCrypt
- Automatic hashing on user registration

### 8.3 Session Security
- HttpOnly cookies (prevents XSS)
- Secure flag disabled (development mode)
- Session fixation protection enabled
- Concurrent session control (max 1 session)

### 8.4 Authorization
- Role-based access control (RBAC)
- Roles: USER, ADMIN
- Path-based authorization rules:
  - `/api/v1/admin/**` - ADMIN only
  - `/ui/admin/**` - ADMIN only
  - `/api/v1/auth/**` - Public

---

## 9. Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Spring Boot | 4.0.1 |
| Language | Java | 17 |
| Build Tool | Maven | 3.8.4 |
| Web | Spring WebMVC | - |
| Security | Spring Security | - |
| ORM | Spring Data JPA | - |
| Database | PostgreSQL | 9.3 |
| Cache/Session | Redis | 7 |
| Redis Client | Lettuce | - |
| JWT Library | JJWT | 0.11.5 |
| Template Engine | Thymeleaf | - |
| Utilities | Lombok | - |
| Validation | Hibernate Validator | - |
| Containerization | Docker | - |

---

## 10. Package Structure

```
com.imbilalbutt.springauthdev
├── SpringAuthDevApplication.java          # Main entry point
├── AuthService/
│   ├── ApiAuthController.java             # REST API controller
│   ├── ApiUserService.java                # Service interface
│   ├── ApiUserServiceImpl.java            # JWT service implementation
│   ├── WebUserService.java                # Service interface
│   ├── WebUserServiceImpl.java            # Session service implementation
│   ├── WebUIServletController.java        # Servlet session UI controller
│   ├── WebUIRedisController.java          # Redis session UI controller
│   ├── SessionController.java             # Session management REST API
│   ├── User.java                          # JPA entity
│   ├── UserRepository.java                # Data access layer
│   ├── RegisterRequest.java               # DTO
│   ├── AuthenticationRequest.java         # DTO
│   ├── AuthenticationResponse.java        # DTO
│   ├── SessionInfo.java                   # DTO
│   ├── UserDTO.java                       # DTO (placeholder)
│   └── UserConverter.java                 # Converter (placeholder)
├── Config/
│   ├── ApplicationConfig.java             # Security beans configuration
│   ├── JwtService.java                    # JWT token operations
│   └── JwtAuthenticationFilter.java       # JWT validation filter
├── Security/
│   └── SecurityConfig.java                # Security filter chains
├── Session/
│   ├── Redis/
│   │   ├── RedisConfiguration.java        # Redis connection config
│   │   ├── SessionRegistry.java           # Session management
│   │   └── SessionAuthenticationFilter.java # Session validation filter
│   └── Servlet/
│       └── ServletSessionManagement.java  # Servlet session config
└── commons/
    └── Role.java                          # Role enum
```

---

## 11. Key Design Decisions

1. **Dual Authentication Strategy**: Supports both stateless (JWT) and stateful (Redis session) authentication for different use cases

2. **Separation of Concerns**: Clear separation between API and UI authentication paths with dedicated controllers and services

3. **Security Filter Chain Ordering**: Uses `@Order` annotation to prioritize API security over UI security

4. **Redis with Fallback**: Session registry falls back to in-memory storage if Redis is unavailable

5. **Containerized Deployment**: Full Docker Compose setup with isolated network

6. **Role-Based Access Control**: Simple but effective RBAC with USER and ADMIN roles

---

## 12. Potential Improvements

1. **UserConverter & UserDTO**: Currently placeholders - should be implemented for proper DTO mapping

2. **SessionInfo**: Not fully integrated into session management flow

3. **Error Handling**: Centralized exception handling could be added

4. **Testing**: Minimal test coverage (only basic application tests)

5. **Monitoring**: No health checks or metrics endpoints visible

6. **Rate Limiting**: No rate limiting on authentication endpoints

7. **Account Verification**: No email verification flow

8. **Password Reset**: No password recovery mechanism

9. **Multi-Factor Authentication**: Not implemented

10. **Audit Logging**: No audit trail for authentication events

---

## 13. Component Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────────┐  │
│  │  Web Browser│  │  Mobile App │  │  Third-Party Services       │  │
│  └──────┬──────┘  └──────┬──────┘  └─────────────┬───────────────┘  │
└─────────┼────────────────┼────────────────────────┼──────────────────┘
          │                │                        │
          │ HTTP/HTTPS     │ HTTP/HTTPS             │ HTTP/HTTPS
          ▼                ▼                        ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Future)                          │
│                    (Rate Limiting, Load Balancing)                   │
└─────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Spring Auth Dev Application                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Controller Layer                             │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │ │
│  │  │ApiAuthCtrl   │  │SessionCtrl   │  │WebUI Controllers     │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Security Layer                               │ │
│  │  ┌──────────────────┐  ┌────────────────────────────────────┐  │ │
│  │  │SecurityConfig    │  │Authentication Filters              │  │ │
│  │  │(Filter Chains)   │  │(JWT, Session)                      │  │ │
│  │  └──────────────────┘  └────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Service Layer                                │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │ │
│  │  │ApiUserService│  │WebUserService│  │JwtService            │  │ │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                              │                                       │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                   Persistence Layer                             │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │  UserRepository (Spring Data JPA)                        │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
          │                              │
          │ JPA/Hibernate                │ Redis Protocol
          ▼                              ▼
┌──────────────────┐          ┌──────────────────┐
│   PostgreSQL     │          │      Redis       │
│   Database       │          │    Session Store │
│   (User Data)    │          │                  │
└──────────────────┘          └──────────────────┘
```

---

## 14. Sequence Diagram: User Registration (JWT)

```
Client          ApiAuthController      ApiUserServiceImpl     UserRepository     JwtService
  │                      │                      │                    │                │
  │──POST /register────►│                      │                    │                │
  │                      │                      │                    │                │
  │                      │────existsByEmail()──►│                    │                │
  │                      │                      │                    │                │
  │                      │◄───exists (true/false)                    │                │
  │                      │                      │                    │                │
  │                      │────save(user)────────────────────────────►│                │
  │                      │                      │                    │                │
  │                      │◄───savedUser                              │                │
  │                      │                      │                    │                │
  │                      │────generateToken()────────────────────────────────────────►│
  │                      │                      │                    │                │
  │                      │◄───jwtToken                                                 │
  │                      │                      │                    │                │
  │◄──AuthResponse──────│                      │                    │                │
  │                      │                      │                    │                │
```

---

## 15. Deployment Considerations

### Development Environment
- Single instance of each service
- Local PostgreSQL and Redis
- Debug mode enabled
- Hot reloading configured

### Production Environment (Recommended)
- Multiple application instances behind load balancer
- PostgreSQL with replication (primary + replicas)
- Redis Cluster for high availability
- HTTPS/TLS termination at load balancer
- Centralized logging (ELK Stack, Splunk)
- Application monitoring (Prometheus + Grafana)
- Secrets management (Vault, AWS Secrets Manager)

---

*Document generated: April 24, 2026*
