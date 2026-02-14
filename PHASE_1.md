# Phase 1: Foundation & Identity

> **Goal**: Set up project infrastructure, authentication, user management, and device security.
> **Estimated Duration**: Weeks 1–3

---

## Phase 1 Overview

| Step | Title                      | Scope                                         | Status  |
| ---- | -------------------------- | --------------------------------------------- | ------- |
| 1.1  | Project Setup              | pom.xml, package structure, profiles, configs | ✅ Done |
| 1.2  | User & Identity Management | User entity, registration, login              | ✅ Done |
| 1.3  | JWT Authentication         | Token generation, refresh, validation         | ✅ Done |
| 1.4  | Device Binding & Security  | Trusted devices, fingerprinting               | ✅ Done |
| 1.5  | KYC Verification           | Document upload, admin review                 | ✅ Done |

---

## 1.1 Project Setup — Discussion

### What We Already Have

The Spring Initializr-generated project is already in place with:

- ✅ Spring Boot **3.2.3** parent POM
- ✅ `EwalletApplication.java` entry point
- ✅ `EwalletApplicationTests.java` basic test
- ✅ Maven Wrapper (`mvnw.cmd`)
- ✅ Build compiles successfully (Java 17)

### What Phase 1.1 Will Deliver

This step sets up everything we need _before_ writing business code.

---

### 1.1.1 — `pom.xml` Adjustments

Our `pom.xml` already includes core dependencies. Here's what's **already configured** vs. **what we might need to add**:

| Dependency                     | Status     | Purpose                   |
| ------------------------------ | ---------- | ------------------------- |
| spring-boot-starter-web        | ✅ Present | REST API                  |
| spring-boot-starter-data-jpa   | ✅ Present | Database access           |
| spring-boot-starter-security   | ✅ Present | Security framework        |
| spring-boot-starter-validation | ✅ Present | Bean validation (@Valid)  |
| spring-boot-starter-actuator   | ✅ Present | Health checks, metrics    |
| jjwt-api / impl / jackson      | ✅ Present | JWT token handling        |
| flyway-core                    | ✅ Present | DB migrations             |
| flyway-database-postgresql     | ✅ Present | Flyway PostgreSQL dialect |
| postgresql driver              | ✅ Present | PostgreSQL connectivity   |
| h2                             | ✅ Present | In-memory testing DB      |
| micrometer-registry-prometheus | ✅ Present | Prometheus metrics        |
| lombok                         | ✅ Present | Boilerplate reduction     |
| spring-boot-devtools           | ✅ Present | Hot reload                |
| spring-boot-starter-test       | ✅ Present | Testing                   |
| spring-security-test           | ✅ Present | Security testing          |

> [!IMPORTANT]
> **Discussion Point**: The `pom.xml` looks complete for Phase 1. Do you want to add any additional dependencies (e.g., MapStruct for DTO mapping, SpringDoc for API docs)?

---

### 1.1.2 — Application Profiles & Configuration

Spring Boot uses **profiles** to separate configuration per environment. We need to set up:

| Profile   | File                   | Purpose                                        |
| --------- | ---------------------- | ---------------------------------------------- |
| `default` | `application.yml`      | Common settings shared across all environments |
| `dev`     | `application-dev.yml`  | Local development with H2 in-memory DB         |
| `test`    | `application-test.yml` | Testing with isolated H2 DB                    |
| `prod`    | `application-prod.yml` | Production with PostgreSQL (placeholder)       |

> [!IMPORTANT]
> **Discussion Points**:
>
> 1. **YAML vs Properties**: Should we use `application.yml` (structured, readable) or `application.properties` (flat, simpler)? I recommend **YAML** for readability since we'll have nested config.
> 2. **Default Profile**: Should the default profile fall back to `dev` or stay neutral?
> 3. **Database for dev**: Use **H2 in-memory** for quick local development, or prefer **PostgreSQL Docker** for realistic testing?

#### Proposed `application.yml` (common settings):

```yaml
spring:
  application:
    name: fintech-ewallet

server:
  port: 8080

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:default-dev-secret-change-in-production}
  expiration: 3600000 # 1 hour in milliseconds
  refresh-expiration: 604800000 # 7 days in milliseconds

# Actuator (health checks)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
```

#### Proposed `application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:ewallet_dev
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true # Access at /h2-console
  jpa:
    hibernate:
      ddl-auto: validate # Flyway manages schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    com.fintech.ewallet: DEBUG
    org.springframework.security: DEBUG
```

---

### 1.1.3 — Package Structure

We'll create the **skeleton directory structure** following Hexagonal Architecture with Feature Modules. No code yet — just the empty packages so the architecture is visible from day one.

```
src/main/java/com/fintech/ewallet/
├── EwalletApplication.java           # ← Already exists
│
├── shared/                           # Cross-cutting concerns
│   ├── config/                       # Security, JWT, Async configs
│   ├── domain/                       # Shared value objects (Money, Currency)
│   ├── exception/                    # Global exception handling
│   └── util/                         # Helpers (ID generation, formatting)
│
├── identity/                         # User & Auth module
│   ├── domain/                       # User entity, UserRepository port
│   ├── application/                  # Use cases (Register, Login)
│   │   └── dto/                      # Request/Response DTOs
│   ├── infrastructure/               # JPA adapters, JWT provider
│   │   ├── persistence/
│   │   └── security/
│   └── api/                          # REST controllers
│
├── device/                           # Device binding module
│   ├── domain/
│   ├── application/
│   │   └── dto/
│   ├── infrastructure/
│   │   └── persistence/
│   └── api/
│
└── kyc/                              # KYC verification module
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── api/
```

> [!NOTE]
> For Phase 1 we only create folders for **identity**, **device**, **kyc**, and **shared**. Other modules (wallet, ledger, transfer, exchange, etc.) will be added in later phases.

> [!IMPORTANT]
> **Discussion Points**:
>
> 1. **Hexagonal purity**: Should `domain/` package contain pure Java classes with zero Spring annotations, or is it OK to use `@Entity` directly? Strict hexagonal separates JPA entities into `infrastructure/persistence/` and keeps domain clean. Trade-off: more boilerplate but cleaner boundaries.
> 2. **DTO location**: DTOs in `application/dto/` or in `api/` alongside controllers?
> 3. **Package naming**: `identity` vs `auth` vs `user` — which name do you prefer for the authentication/user module?

---

### 1.1.4 — Initial Flyway Migration

Flyway manages database schema changes via numbered SQL files. We'll create:

```
src/main/resources/db/migration/
└── V1__create_initial_schema.sql
```

This file will be **empty initially** (or contain just a comment), ready for Phase 1.2 when we add the `users` table.

> [!IMPORTANT]
> **Discussion Point**: Should the initial migration be empty (placeholder), or should we include the `users` table schema right away?

---

### 1.1.5 — Global Exception Handler

A `@RestControllerAdvice` class that standardizes all API error responses.

**Proposed standard error response format:**

```json
{
  "timestamp": "2024-02-10T20:30:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "details": [
    { "field": "phoneNumber", "message": "Phone number is required" }
  ],
  "path": "/api/v1/auth/register"
}
```

> [!IMPORTANT]
> **Discussion Points**:
>
> 1. **Error code format**: String codes like `VALIDATION_FAILED` or numeric codes like `1001`?
> 2. **Localization**: Should error messages be English-only for now, or support Arabic from the start?
> 3. **Stack traces**: Never expose in prod, but show in dev?

---

### 1.1.6 — Security Config (Permit-All Starter)

For Phase 1.1, we'll create a `SecurityConfig` that **permits all requests**. We'll lock it down in Phase 1.3 when JWT is implemented.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // API-only, no CSRF needed
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()           // Open for now
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);        // 12-round BCrypt
    }
}
```

> [!NOTE]
> This is intentionally wide-open. Phase 1.3 will add JWT filter chain and restrict endpoints.

---

## Summary — What Phase 1.1 Will Produce

| Deliverable                                            | Type      |
| ------------------------------------------------------ | --------- |
| Updated `pom.xml` (if needed)                          | Config    |
| `application.yml` (common settings)                    | Config    |
| `application-dev.yml` (H2, debug logging)              | Config    |
| `application-test.yml` (test isolation)                | Config    |
| Empty package skeleton (shared, identity, device, kyc) | Structure |
| `V1__create_initial_schema.sql` (placeholder)          | Migration |
| `GlobalExceptionHandler.java`                          | Code      |
| `ApiErrorResponse.java`                                | Code      |
| `SecurityConfig.java` (permit-all)                     | Code      |
| `PasswordEncoder` bean                                 | Code      |

---

## Questions for You

Before I implement anything, I'd like your input on these decisions:

1. **YAML or Properties** for configuration files? → **YAML** ✅
2. **H2 in-memory** or **PostgreSQL via Docker** for local development? → **PostgreSQL Docker** ✅
3. **Strict hexagonal** (separate JPA entities from domain entities) or **pragmatic** (JPA annotations on domain classes)? → **Strict hexagonal** ✅
4. **Error codes**: string (`VALIDATION_FAILED`) or numeric (`1001`)? → **String** ✅
5. **Arabic error messages** from the start, or add later? → **Add later** ✅
6. **Package naming**: `identity` vs `auth` vs `user`? → **`identity`** ✅
7. **Any additional dependencies**? → **MapStruct, SpringDoc, Testcontainers, Docker Compose** ✅

---

## 1.2 User & Identity Management — Discussion

> [!NOTE]
> Phase 1.2 focuses on the **User domain entity**, **registration**, and **login**. JWT tokens are generated here but the full JWT security filter chain is Phase 1.3.

### Architecture Layers (Strict Hexagonal)

Since you chose **strict hexagonal**, every entity will have this separation:

```
┌─────────────────────────────────────────────────────────────┐
│  DOMAIN LAYER (pure Java — NO Spring annotations)          │
│  ┌─────────────┐  ┌──────────────────┐                     │
│  │ User.java   │  │ UserRepository   │ ← interface (port)  │
│  │ (plain POJO)│  │ (interface only) │                     │
│  └─────────────┘  └──────────────────┘                     │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION LAYER (orchestration — use cases)             │
│  ┌────────────────────┐  ┌─────────────────┐               │
│  │ RegisterUserUseCase│  │ LoginUseCase    │               │
│  │ (@Service)         │  │ (@Service)      │               │
│  └────────────────────┘  └─────────────────┘               │
│  ┌────────────────────┐  ┌─────────────────┐               │
│  │ RegisterRequest    │  │ LoginResponse   │ ← DTOs        │
│  │ (record)           │  │ (record)        │               │
│  └────────────────────┘  └─────────────────┘               │
├─────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE LAYER (framework — adapters)               │
│  ┌────────────────────┐  ┌─────────────────────┐           │
│  │ UserJpaEntity.java │  │ UserJpaRepository   │           │
│  │ (@Entity, @Table)  │  │ (Spring Data JPA)   │           │
│  └────────────────────┘  └─────────────────────┘           │
│  ┌────────────────────┐                                    │
│  │ UserMapper.java    │ ← MapStruct (domain ↔ JPA entity) │
│  └────────────────────┘                                    │
├─────────────────────────────────────────────────────────────┤
│  API LAYER (REST controllers)                              │
│  ┌────────────────────┐                                    │
│  │ AuthController.java│ → calls use cases, returns DTOs    │
│  └────────────────────┘                                    │
└─────────────────────────────────────────────────────────────┘
```

---

### 1.2.1 — Domain Entity: `User.java`

Pure Java POJO — no JPA, no Spring, no Lombok. Business logic lives here.

**Proposed fields:**

| Field           | Type                 | Purpose                                   |
| --------------- | -------------------- | ----------------------------------------- |
| `id`            | `UUID`               | Primary key (generated)                   |
| `phoneNumber`   | `String`             | International format (+967...)            |
| `countryCode`   | `String`             | ISO 3166-1 ("YE", "SA")                   |
| `fullName`      | `String`             | User's display name                       |
| `passwordHash`  | `String`             | BCrypt hash (never plain text)            |
| `email`         | `String`             | Optional, for recovery                    |
| `kycStatus`     | `KycStatus` enum     | `NONE`, `PENDING`, `VERIFIED`, `REJECTED` |
| `accountStatus` | `AccountStatus` enum | `ACTIVE`, `SUSPENDED`, `CLOSED`           |
| `language`      | `String`             | `"ar"` or `"en"` (default `"ar"`)         |
| `referralCode`  | `String`             | Unique code for referral program          |
| `createdAt`     | `Instant`            | Registration timestamp                    |
| `updatedAt`     | `Instant`            | Last profile update                       |

> [!IMPORTANT]
> **Discussion Points:**
>
> 1. **Phone number format**: Store as `+967XXXXXXXXX` (E.164 international)? Or separate `countryCode` + `localNumber`?
> 2. **Email**: Required or optional? Some Yemeni users may not have email.
> 3. **Default language**: `"ar"` (Arabic) or let the client send it?
> 4. **Referral code**: Generate on registration, or add in a later phase?

---

### 1.2.2 — JPA Entity: `UserJpaEntity.java`

This lives in `infrastructure/persistence/` and has all the JPA annotations. MapStruct maps between `User` ↔ `UserJpaEntity`.

**Proposed database table:**

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    phone_number    VARCHAR(20) NOT NULL UNIQUE,
    country_code    VARCHAR(3)  NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    kyc_status      VARCHAR(20) NOT NULL DEFAULT 'NONE',
    account_status  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    language        VARCHAR(5)  NOT NULL DEFAULT 'ar',
    referral_code   VARCHAR(20) UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_referral ON users(referral_code);
```

> [!IMPORTANT]
> **Discussion Points:** 5. **Enum storage**: Store enums as `VARCHAR` (readable in DB) or `SMALLINT` (compact)? 6. **Soft delete**: Should we add a `deleted_at` column for soft deletes, or hard-delete users?

---

### 1.2.3 — Repository Port: `UserRepository.java`

Domain interface (port) — no Spring annotations.

```java
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
```

The **adapter** (`UserRepositoryAdapter.java`) in infrastructure will implement this using Spring Data JPA + MapStruct.

---

### 1.2.4 — Registration Use Case

**Flow:**

```
Client POST /api/v1/auth/register
        │
        ▼
┌─ AuthController ─────────────────────────┐
│  Validates @Valid RegisterRequest        │
│  Calls RegisterUserUseCase.execute()     │
└──────────│───────────────────────────────┘
           ▼
┌─ RegisterUserUseCase ────────────────────┐
│  1. Check phone not already registered   │
│  2. Validate phone format                │
│  3. Hash password with BCrypt(12)        │
│  4. Generate referral code               │
│  5. Create User domain object            │
│  6. Save via UserRepository port         │
│  7. Return RegisterResponse (no token)   │
└──────────────────────────────────────────┘
```

**Register Request DTO:**

```java
public record RegisterRequest(
    @NotBlank String phoneNumber,
    @NotBlank @Size(min = 8, max = 50) String password,
    @NotBlank String fullName,
    String countryCode,  // defaults to "YE"
    String email,        // optional
    String language      // defaults to "ar"
) {}
```

**Register Response DTO:**

```java
public record RegisterResponse(
    UUID userId,
    String phoneNumber,
    String fullName,
    String referralCode,
    String message
) {}
```

> [!NOTE]
> Registration does NOT return a JWT token. The user must login separately after registering. This is a deliberate security choice.

---

### 1.2.5 — Login Use Case

**Flow:**

```
Client POST /api/v1/auth/login
        │
        ▼
┌─ AuthController ─────────────────────────┐
│  Validates @Valid LoginRequest           │
│  Calls LoginUseCase.execute()            │
└──────────│───────────────────────────────┘
           ▼
┌─ LoginUseCase ───────────────────────────┐
│  1. Find user by phone number            │
│  2. Verify password with BCrypt.matches  │
│  3. Check account is ACTIVE              │
│  4. Generate JWT access token (1hr)      │
│  5. Generate JWT refresh token (7 days)  │
│  6. Return LoginResponse with tokens     │
└──────────────────────────────────────────┘
```

**Login Request DTO:**

```java
public record LoginRequest(
    @NotBlank String phoneNumber,
    @NotBlank String password
) {}
```

**Login Response DTO:**

```java
public record LoginResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        UUID id,
        String fullName,
        String phoneNumber,
        String kycStatus
    ) {}
}
```

---

### 1.2.6 — API Endpoints

| Endpoint                | Method | Auth Required | Description           |
| ----------------------- | ------ | ------------- | --------------------- |
| `/api/v1/auth/register` | POST   | No            | Register new user     |
| `/api/v1/auth/login`    | POST   | No            | Login, get JWT tokens |

> [!NOTE]
> Refresh token and logout endpoints will be added in **Phase 1.3** (JWT Authentication) since they need the full JWT filter chain.

---

### 1.2.7 — Files to Create

| File                              | Layer          | Package                               |
| --------------------------------- | -------------- | ------------------------------------- |
| `User.java`                       | Domain         | `identity.domain`                     |
| `KycStatus.java` (enum)           | Domain         | `identity.domain`                     |
| `AccountStatus.java` (enum)       | Domain         | `identity.domain`                     |
| `UserRepository.java` (interface) | Domain         | `identity.domain`                     |
| `RegisterUserUseCase.java`        | Application    | `identity.application`                |
| `LoginUseCase.java`               | Application    | `identity.application`                |
| `RegisterRequest.java` (record)   | Application    | `identity.application.dto`            |
| `RegisterResponse.java` (record)  | Application    | `identity.application.dto`            |
| `LoginRequest.java` (record)      | Application    | `identity.application.dto`            |
| `LoginResponse.java` (record)     | Application    | `identity.application.dto`            |
| `UserJpaEntity.java`              | Infrastructure | `identity.infrastructure.persistence` |
| `UserJpaRepository.java`          | Infrastructure | `identity.infrastructure.persistence` |
| `UserRepositoryAdapter.java`      | Infrastructure | `identity.infrastructure.persistence` |
| `UserMapper.java` (MapStruct)     | Infrastructure | `identity.infrastructure.persistence` |
| `JwtTokenProvider.java`           | Infrastructure | `identity.infrastructure.security`    |
| `AuthController.java`             | API            | `identity.api`                        |
| `V2__create_users_table.sql`      | Migration      | `db/migration`                        |

---

## Questions for You (Phase 1.2)

1. **Phone format**: Store as `+967XXXXXXXXX` (single field) or separate `countryCode` + `localNumber`?
2. **Email**: Required or optional at registration?
3. **Default language**: `"ar"` (Arabic) as default?
4. **Referral code**: Generate now or later?
5. **Enum storage in DB**: `VARCHAR` (readable) or integer codes?
6. **Soft delete**: Add `deleted_at` column, or hard-delete?
7. **Registration → auto-login?** Return tokens on register, or require separate login?
8. **Anything else** you want on the User entity?

---

## 1.3 JWT Authentication — Discussion

> [!NOTE]
> Phase 1.2 created `JwtTokenProvider` which **generates** tokens during login. Phase 1.3 adds the **filter chain** to **validate** tokens and **protect** endpoints.

### Current State

Right now, our API is **completely open**:

- `SecurityConfig` has `permitAll()` — anyone can call any endpoint.
- Login returns JWT tokens, but **nothing validates them**.
- A user could call protected endpoints without logging in.

**Phase 1.3 Goal:** Lock down the API so only authenticated users with valid tokens can access protected resources.

---

### 1.3.1 — JWT Authentication Filter

**What it does:**

1. **Intercepts** every HTTP request.
2. **Extracts** the JWT token from the `Authorization: Bearer <token>` header.
3. **Validates** the token (signature, expiration, format).
4. **Extracts** the User ID from the token.
5. **Sets** Spring Security's `Authentication` object so Spring knows "this user is logged in".

**Flow Diagram:**

```
Client Request
    │
    ▼
┌─────────────────────────────────────────┐
│ JwtAuthenticationFilter                 │
│  1. Extract token from header           │
│  2. Call JwtTokenProvider.parseToken()  │
│  3. Check token is not blacklisted      │
│  4. Extract userId from claims          │
│  5. Create Authentication object        │
│  6. Set SecurityContext                 │
└────────────│────────────────────────────┘
             ▼
        Controller
             │
             ▼
        Use Cases (can access userId via @AuthenticationPrincipal)
```

---

### 1.3.2 — Refresh Token Endpoint

**Why we need it:**

Access tokens expire quickly (1 hour). Instead of forcing the user to re-enter their password every hour, we use a **refresh token** (valid for 7 days) to get a new access token.

**Flow:**

```
POST /api/v1/auth/refresh
Headers:
  Authorization: Bearer <refresh_token>

Response:
{
  "accessToken": "new_jwt_...",
  "refreshToken": "new_refresh_...", // Optional: rotate refresh tokens
  "expiresIn": 3600000
}
```

**Security considerations:**

- Should we **rotate** refresh tokens? (Issue a new refresh token each time, invalidate the old one)
- Should refresh tokens be **tied to a device**? (Different refresh token per phone/laptop)

---

### 1.3.3 — Logout Endpoint

**The Problem:** JWTs are **stateless**. Once issued, they're valid until they expire. You can't "delete" them from the server.

**Solution: Token Blacklist**

When a user logs out:

1. Add the JWT's unique ID (`jti` claim) to a **blacklist**.
2. The filter checks the blacklist before validating the token.
3. If the token is blacklisted → reject it, even if it's not expired yet.

**Where to store the blacklist?**

| Option                            | Pros                                                   | Cons                          |
| --------------------------------- | ------------------------------------------------------ | ----------------------------- |
| **Redis**                         | Fast (in-memory), auto-expiry (TTL = token expiration) | Requires Redis setup          |
| **Database**                      | Simple, no extra infrastructure                        | Slower, needs cleanup job     |
| **In-Memory (ConcurrentHashMap)** | Zero dependencies                                      | Lost on restart, not scalable |

---

### 1.3.4 — Protecting Endpoints

**Update `SecurityConfig` to:**

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()         // Public endpoints
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
        .anyRequest().authenticated()                           // Everything else requires JWT
    )
    .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No cookies
    )
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

---

### 1.3.5 — Files to Create/Modify

| File                                 | Action | Purpose                                                           |
| ------------------------------------ | ------ | ----------------------------------------------------------------- |
| `JwtAuthenticationFilter.java`       | NEW    | Extract + validate token, set authentication                      |
| `TokenBlacklistService.java`         | NEW    | Blacklist interface (port)                                        |
| `InMemoryTokenBlacklistService.java` | NEW    | Simple in-memory implementation                                   |
| `RefreshTokenUseCase.java`           | NEW    | Application layer: validate refresh token, issue new access token |
| `AuthController.java`                | MODIFY | Add `/refresh` and `/logout` endpoints                            |
| `SecurityConfig.java`                | MODIFY | Add filter, lock down endpoints                                   |

---

### 1.3.6 — Extracting User from Context

Once the filter sets the authentication, controllers can access the logged-in user like this:

```java
@GetMapping("/me")
public UserProfile getProfile(@AuthenticationPrincipal UUID userId) {
    // userId is automatically extracted from the JWT
}
```

Or create a custom `@CurrentUserId` annotation for cleaner code.

---

## Questions for You (Phase 1.3)

1. **Token Blacklist Storage**: Redis (requires setup), Database (simpler but slower), or In-Memory (not production-ready)?
2. **Refresh Token Rotation**: Should we issue a new refresh token on each `/refresh` call and invalidate the old one? (More secure but complex)
3. **Device Binding**: Should refresh tokens be tied to a specific device? (One device logout = only that device's refresh token is invalidated)
4. **Token Expiration Times**: Keep 1 hour access + 7 days refresh, or change?
5. **Public Endpoints**: Which endpoints should remain public after Phase 1.3?
   - `/api/v1/auth/register` ✅
   - `/api/v1/auth/login` ✅
   - `/actuator/health` ✅
   - Swagger UI (`/swagger-ui/**`) — public or require auth?

---

## 1.4 Device Binding & Security — Discussion

> [!NOTE]
> Phase 1.4 adds **trusted device management**. This enables multi-device login while maintaining security. In Phase 1.3, we added device binding to refresh tokens conceptually — now we build the full infrastructure.

### Why Device Binding Matters

**Problem**: Without device binding:

- User logs in on Phone A → gets refresh token
- Hacker steals the refresh token → uses it from Phone B
- User can't revoke just the hacker's session

  **Solution**: Tie each refresh token to a **device fingerprint**:

- Each device gets its own refresh token
- Logout from one device doesn't affect others
- User can see all active devices and revoke suspicious ones

---

### 1.4.1 — Device Fingerprinting

**What is a device fingerprint?**

A unique identifier calculated from browser/device characteristics:

```
DeviceFingerprint = hash(
    UserAgent,      // "Mozilla/5.0 (iPhone; CPU...)"
    ScreenResolution,
    TimeZone,
    Language,
    Platform
)
```

**Why not just use IP address?**

- IP changes (mobile networks, VPNs)
- Multiple users on the same Wi-Fi share an IP

**Why not rely on client-sent device ID?**

- Clients can lie (send fake ID)
- Fingerprint is harder to spoof

**Hybrid Approach** (Recommended):

- Client sends a `deviceId` (they generate on first install)
- Server calculates fingerprint from headers
- Both are stored and checked

---

### 1.4.2 — Trusted Device Entity

**Domain Model:**

```java
class TrustedDevice {
    UUID id;
    UUID userId;
    String deviceId;            // Client-generated UUID
    String fingerprint;         // Server-calculated hash
    String deviceName;          // "Mustafa's iPhone 12"
    String userAgent;
    String lastIpAddress;
    boolean isPrimary;          // First device registered
    Instant lastUsedAt;
    Instant createdAt;
}
```

**Database Table:**

```sql
CREATE TABLE trusted_devices (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    device_id       VARCHAR(255) NOT NULL,
    fingerprint     VARCHAR(255) NOT NULL,
    device_name     VARCHAR(100),
    user_agent      TEXT,
    last_ip_address VARCHAR(45),
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    last_used_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_trusted_devices_user ON trusted_devices(user_id);
CREATE INDEX idx_trusted_devices_fingerprint ON trusted_devices(fingerprint);
```

---

### 1.4.3 — Device Binding Flow

**1. First Login (New Device)**

```
Client POST /api/v1/auth/login
{
  "phoneNumber": "+967...",
  "password": "...",
  "deviceId": "abc-123",     // Generated by client
  "deviceName": "iPhone 12"
}

Server:
1. Verify credentials
2. Calculate fingerprint from headers (UserAgent, IP, etc.)
3. Check: Is this device already trusted?
   - NO → Create new TrustedDevice
   - YES → Update lastUsedAt
4. Generate RefreshToken tied to deviceId
5. Return tokens + "device registered" flag
```

**2. Subsequent Login (Same Device)**

```
Server recognizes fingerprint → Skip OTP, log in directly
```

**3. Login from New Device (OTP Challenge)**

```
Server sees unknown fingerprint → Send OTP to user's phone
User enters OTP → Device is added to trusted list
```

> [!IMPORTANT]
> **Discussion Point**: Should we **always** require OTP for new devices, or only if:
>
> - User has enabled 2FA?
> - User has high-value transactions in KYC?
> - It's the 2nd+ device (first device is auto-trusted)?

---

### 1.4.4 — OTP (One-Time Password)

**Use Cases:**

1. New device login
2. Sensitive operations (large transfers, password change)
3. Account recovery

**OTP Generation:**

```java
String otp = String.format("%06d", new Random().nextInt(1000000)); // "042387"
```

**Storage:**

| Storage      | Pros                            | Cons              |
| ------------ | ------------------------------- | ----------------- |
| **Redis**    | Fast, auto-expiry (TTL = 5 min) | Requires Redis    |
| **Database** | Simple                          | Needs cleanup job |

**Rate Limiting:**

- Max 5 OTP requests per hour per phone number
- Store in Redis: `otp:ratelimit:{phone}` → count, TTL = 1 hour

**Sending OTP:**

- Phase 1: Log to console (for testing)
- Phase 2: Integrate SMS provider (Twilio, AWS SNS)

---

### 1.4.5 — API Endpoints

| Endpoint                          | Method | Auth | Description                                    |
| --------------------------------- | ------ | ---- | ---------------------------------------------- |
| `GET /api/v1/devices`             | GET    | ✅   | List user's trusted devices                    |
| `POST /api/v1/devices/verify-otp` | POST   | ❌   | Verify OTP and bind new device                 |
| `DELETE /api/v1/devices/{id}`     | DELETE | ✅   | Revoke a device (blacklists its refresh token) |
| `PUT /api/v1/devices/{id}/rename` | PUT    | ✅   | Rename a device                                |

---

### 1.4.6 — Refresh Token Schema Change

**Current:**
Refresh token just contains `userId`.

**Phase 1.4:**
Refresh token contains `userId` + `deviceId`.

```java
// Generate refresh token
Jwts.builder()
    .subject(user.getId().toString())
    .claim("deviceId", device.getDeviceId())  // NEW
    .claim("type", "refresh")
    ...
```

**RefreshTokenUseCase** changes:

1. Extract `deviceId` from refresh token
2. Verify device still exists and is trusted
3. If device was revoked → reject refresh

---

### 1.4.7 — Files to Create/Modify

| File                                  | Action | Purpose                                |
| ------------------------------------- | ------ | -------------------------------------- |
| `TrustedDevice.java`                  | NEW    | Domain entity                          |
| `TrustedDeviceRepository.java`        | NEW    | Port interface                         |
| `TrustedDeviceJpaEntity.java`         | NEW    | JPA entity                             |
| `TrustedDeviceJpaRepository.java`     | NEW    | Spring Data                            |
| `TrustedDeviceRepositoryAdapter.java` | NEW    | Adapter                                |
| `TrustedDeviceMapper.java`            | NEW    | MapStruct                              |
| `OtpService.java`                     | NEW    | Port interface for OTP                 |
| `RedisOtpService.java`                | NEW    | Redis implementation                   |
| `DeviceFingerprintService.java`       | NEW    | Calculate fingerprint from request     |
| `VerifyOtpUseCase.java`               | NEW    | Verify OTP, bind device                |
| `ListDevicesUseCase.java`             | NEW    | Get user's devices                     |
| `RevokeDeviceUseCase.java`            | NEW    | Delete device, blacklist tokens        |
| `DeviceController.java`               | NEW    | REST API                               |
| `JwtTokenProvider.java`               | MODIFY | Add `deviceId` to refresh token claims |
| `RefreshTokenUseCase.java`            | MODIFY | Verify device is still trusted         |
| `LoginUseCase.java`                   | MODIFY | Register/update device on login        |
| `V3__create_trusted_devices.sql`      | NEW    | Flyway migration                       |

---

## Questions for You (Phase 1.4)

1. **OTP Provider**: Log to console (testing only), or integrate SMS provider now (Twilio, AWS SNS)?
2. **OTP Always Required?**: Should we **always** send OTP for new devices, or only if:
   - 2FA is enabled by the user?
   - It's the 2nd+ device (first device is auto-trusted)?
3. **Device Limit**: Should we limit users to N devices (e.g., 5 max)?
4. **Fingerprint Calculation**: Which headers should we use?
   - User-Agent ✅
   - IP Address (changes often on mobile)
   - Accept-Language
   - Screen Resolution (from client)
5. **Device Naming**: Auto-generate from User-Agent ("Chrome on Windows") or require user input?
6. **Primary Device**: Should the first device be "primary" and receive special treatment (e.g., can't be revoked without re-authentication)?


---

## 1.5 KYC Verification  Discussion

> [!NOTE]
> Phase 1.5 implements **Know Your Customer (KYC)** verification  a legal requirement for fintech apps. Users must upload identity documents (passport, national ID) before they can create wallets or transfer money.

### Why KYC Matters

**The Regulatory Reality:**

Every country with anti-money laundering (AML) laws requires fintech companies to:
1. **Verify user identities** before allowing financial transactions
2. **Keep records** of identity documents for 5-10 years
3. **Report suspicious activity** to financial authorities
4. **Block** users who fail verification

**Without KYC:**
- Your app can be **shut down** by regulators
- You face **fines** (millions of dollars in some countries)
- Banks won't partner with you for cash-out services
- Payment processors (Stripe, PayPal) will reject you

**With KYC:**
-  Compliant with regulations
-  Can operate legally in most countries
-  Partner with banks and payment processors
-  Build user trust (legitimate users only)
-  Reduce fraud (harder to use stolen accounts)

---

### 1.5.1  The KYC Journey

**User Perspective:**

1. **Registration**  User creates account (email, phone, password)
2. **Try to create wallet**  Blocked! ""Please complete KYC verification""
3. **Upload document**  Takes photo of passport or national ID
4. **Wait for review**  Admin reviews document (manual or AI)
5. **Approved**  Can now create wallet and transfer money
6. **OR Rejected**  ""Your passport image is blurry, please re-upload""

**Key Insight:** KYC is a **gate**  users can register and login, but can't do financial operations until verified.

---

### 1.5.2  Document Types

We accept 4 types of government-issued ID:

| Document Type | Why It's Accepted | Why It's NOT Accepted |
|---------------|-------------------|----------------------|
| **Passport** | Universal, hard to forge, has photo | Expensive to obtain |
| **National ID** | Common in most countries | Some countries don't issue them |
| **Driver's License** | Widely available | Easier to forge than passport |
| **Residence Permit** | For non-citizens | Not accepted as primary ID in some countries |

**Design Decision:** Accept all 4, but **prefer passport** as it's the most trusted globally.

**Rejected Documents:**
-  Student ID (not government-issued)
-  Work badge (can be faked)
-  Birth certificate (no photo)
-  Utility bill (used for address verification, not ID)

---

### 1.5.3  File Storage: Local vs Cloud

**The Problem:** KYC documents are:
- **Large** (photos are 2-5 MB each)
- **Sensitive** (contain personal information)
- **Long-lived** (must keep for 5-10 years by law)

**Development:** Store locally in ./uploads/kyc/{userId}/
-  Fast for testing
-  No AWS bill
-  Files lost if server crashes
-  Not scalable (disk fills up)

**Production:** Use cloud storage (S3, Azure Blob, GCS)
-  Durable (99.999999999% durability)
-  Scalable (unlimited storage)
-  Encrypted at rest
-  Access logging for compliance
-  Costs money (.023 per GB/month on S3)

**Why Abstract Storage?** We use a FileStorageService interface:
- Development  LocalFileStorageService saves to disk
- Production  S3FileStorageService saves to AWS S3
- Domain code doesn't care which implementation is used

This is **Hexagonal Architecture**  infrastructure is pluggable.

---

### 1.5.4  Security: What Could Go Wrong?

**Threat 1: Malicious File Upload**

Attacker uploads a virus disguised as passport.jpg:
-  **Mitigation:** Check MIME type (only allow image/jpeg, image/png, application/pdf)
-  **Mitigation:** Limit file size to 5MB (prevent DoS)
-  **TODO:** Integrate virus scanner (ClamAV) in production

**Threat 2: Stolen Document**

Hacker steals someone's passport scan, uploads it to create fake account:
-  **Mitigation:** Require selfie verification (future phase)
-  **Mitigation:** Admin compares selfie to passport photo
-  **TODO:** Integrate AI verification (Onfido, Jumio)

**Threat 3: Data Breach**

Database hacked, attacker downloads all passport scans:
-  **Mitigation:** Encrypt files at rest (S3 SSE, AES-256)
-  **Mitigation:** Encrypt database connections (SSL/TLS)
-  **Mitigation:** Don't expose file URLs publicly (use pre-signed URLs)
-  **Mitigation:** Log all file access for audit trail

---

### 1.5.5  The KYC Status State Machine

``
NONE  User hasn't uploaded documents yet
  
PENDING  User uploaded, waiting for admin review
  
VERIFIED  Admin approved 
REJECTED  Admin rejected  (can re-upload)
``

**State Transitions:**

| From | To | Trigger | Who Can Do It |
|------|-----|---------|---------------|
| NONE | PENDING | Upload document | User |
| PENDING | VERIFIED | Approve | Admin only |
| PENDING | REJECTED | Reject | Admin only |
| REJECTED | PENDING | Re-upload | User |
| VERIFIED | REJECTED | Fraudulent doc discovered | Admin only |

**Business Rule:** Once VERIFIED, users can't go back to NONE. Even if rejected later, they stay in the system (for audit trail).

---

### 1.5.6  Admin Review: Manual vs Automated

**Option 1: Manual Review (What We Built)**

Admin logs into dashboard, sees list of pending documents:
- Views passport scan
- Checks if photo is clear
- Checks if name matches registration
- Clicks ""Approve"" or ""Reject""

**Pros:**
-  100% accurate (human review)
-  No integration costs
-  Can handle edge cases (damaged passports, etc.)

**Cons:**
-  Slow (24-48 hours)
-  Doesn't scale (need more admins as users grow)
-  Human error (tired admin approves fake passport)

**Option 2: Automated AI Review (Future Phase)**

Integrate with **Onfido**, **Jumio**, or **Stripe Identity**:
- User uploads document  Sent to AI service
- AI extracts text (OCR)
- AI checks if document is real (not photoshopped)
- AI matches selfie to passport photo
- Auto-approves in 30 seconds

**Pros:**
-  Fast (instant or < 1 minute)
-  Scales infinitely
-  Catches sophisticated fakes

**Cons:**
-  Expensive (-3 per verification)
-  Not 100% accurate (sometimes rejects real passports)
-  Vendor lock-in

**Our Decision:** Start with manual, upgrade to AI when we have > 1000 users/month.

---

### 1.5.7  KYC-Gated Operations

**What Operations Require KYC?**

 **Require KYC (kyc_status = VERIFIED):**
- Create wallet
- Send money
- Withdraw to bank account
- Cash out to mobile money

 **Don't Require KYC:**
- Register account
- Login
- View profile
- Read transaction history (if wallet exists)

**Why This Split?**

Regulations say ""verify users before they handle money"". Reading data is okay, moving money is not.

**Implementation:**

Before any money-moving operation, we check:
``
if (user.kycStatus != VERIFIED) {
    throw KycRequiredException(""Complete KYC to continue"");
}
``

This gate is enforced in:
- CreateWalletUseCase
- SendMoneyUseCase
- WithdrawUseCase

---

### 1.5.8  File Upload: Multipart vs Base64

**Option 1: Multipart Form Data (What We Built)**

Client sends file as multipart/form-data:
``
POST /api/v1/kyc/upload
Content-Type: multipart/form-data

--boundary--
Content-Disposition: form-data; name=""documentType""
PASSPORT
--boundary--
Content-Disposition: form-data; name=""file""; filename=""passport.jpg""
Content-Type: image/jpeg

<binary image data>
--boundary--
``

**Pros:**
-  Standard for file uploads (all browsers support it)
-  Efficient (no base64 encoding overhead)
-  Easy to test with Postman/cURL

**Cons:**
-  Harder to use from React Native (needs special library)

**Option 2: Base64 in JSON**

Client encodes file as base64 string:
``json
{
  ""documentType"": ""PASSPORT"",
  ""fileName"": ""passport.jpg"",
  ""fileData"": ""iVBORw0KGgoAAAANSUhEUgAA...""
}
``

**Pros:**
-  Easy to use from any client (just JSON)
  
**Cons:**
-  33% larger payload (base64 encoding overhead)
-  Can't stream large files (entire file in memory)

**Our Decision:** Multipart wins. React Native has libraries (expo-image-picker) that handle it.

---

### 1.5.9  Document Expiry & Re-verification

**The Problem:**

Passports expire every 10 years. If user uploaded passport in 2026, by 2036 it's expired. Should we require re-upload?

**Option 1: No Expiry Tracking**
- Once verified, stay verified forever
-  Simple
-  User might have changed appearance (aging)
-  Document might be revoked (stolen passport reported)

**Option 2: Track Expiry, Require Re-verification**
- Store document_expiry_date field
- Cron job checks for expired documents
- Send email: ""Your passport expired, please re-upload""
-  More secure
-  Annoying for users

**Option 3: Risk-Based Re-verification**
- Auto-trigger re-verification if:
  - User suddenly sends large amounts (suspicious)
  - User changes phone number or email
  - 5 years since last verification
-  Balance security and UX

**Our Decision (Future Phase):** Implement Option 3. For now, no expiry tracking.

---

### Design Decisions (Phase 1.5)

> [!IMPORTANT]
> **Design decisions finalized on 2026-02-14**

#### 1. ? Selfie Verification  **YES**
**Decision:** Users must upload a selfie alongside their ID document.

**Rationale:**
- Prevents use of stolen documents (face match required)
- Industry standard for fintech KYC
- Minor friction acceptable for security gain

**Implementation:** Phase 2 will add:
- Selfie upload field in KYC form
- Admin can compare selfie to passport photo
- Future: AI face-matching integration

---

#### 2.  Multiple Documents  **NO**
**Decision:** Only 1 government-issued ID required (no utility bill).

**Rationale:**
- Address verification can be added later if needed
- Reduces user friction (easier onboarding)
- Many users don't have utility bills in their name
- Passport/National ID already verifies identity sufficiently

---

#### 3.  Third-Party KYC  **NOT NOW**
**Decision:** Manual admin review for now. No Onfido/Jumio integration yet.

**Rationale:**
- Costs $1-3 per verification (expensive for MVP)
- Manual review is fine for low volume (< 1,000 users/month)
- Can integrate later when we have revenue

**Revisit when:** Monthly KYC submissions exceed 500

---

#### 4.  Document Retention  **5 YEARS**
**Decision:** Keep uploaded documents for 5 years after account creation.

**Rationale:**
- Meets minimum regulatory compliance
- Balances legal requirements with privacy concerns
- Auto-delete after 5 years (cron job)

**Implementation:**
- Add document_expiry_date field
- Monthly cron job deletes expired documents
- Log deletions for audit trail

---

#### 5.  Admin Dashboard  **BUILD IN PHASE 2**
**Decision:** Build proper admin dashboard in Phase 2.

**Rationale:**
- Better UX for admin reviewers
- Reduces errors
- Can show document preview
- Scalable

**Phase 2 Dashboard Features:**
- List pending KYC documents
- View uploaded document images
- Approve/Reject with reason
- Search by user name/phone

---
---

**Ready for Phase 2: Wallet & Ledger!** 
