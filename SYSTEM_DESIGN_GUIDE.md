# 🎯 System Design Interview Guide — E-Wallet Project

> **Purpose**: Every system design concept in this project, explained for interview prep.
> Each topic maps to real code you wrote — so you can explain **with examples**.

---

## Table of Contents

1. [Architecture Patterns](#1-architecture-patterns)
2. [Authentication & Authorization](#2-authentication--authorization)
3. [Security Design](#3-security-design)
4. [Database Design](#4-database-design)
5. [Caching & In-Memory Stores](#5-caching--in-memory-stores)
6. [API Design](#6-api-design)
7. [Data Consistency & Transactions](#7-data-consistency--transactions)
8. [Rate Limiting](#8-rate-limiting)
9. [Scalability Patterns](#9-scalability-patterns)
10. [DevOps & Infrastructure](#10-devops--infrastructure)
11. [Common Interview Questions](#11-common-interview-questions)

---

## 1. Architecture Patterns

### 1.1 Hexagonal Architecture (Ports & Adapters)

**Interview Question**: _"How do you structure a backend application?"_

```
┌──────────────────────────────────────────┐
│                  API Layer               │  ← Controllers (REST endpoints)
│              (Driving Adapter)           │
├──────────────────────────────────────────┤
│             Application Layer            │  ← Use Cases (business logic)
│         (Orchestration / Use Cases)      │
├──────────────────────────────────────────┤
│               Domain Layer               │  ← Entities + Repository Interfaces
│          (Pure Business Logic)           │
├──────────────────────────────────────────┤
│           Infrastructure Layer           │  ← JPA, Redis, JWT (implementations)
│             (Driven Adapters)            │
└──────────────────────────────────────────┘
```

**How our project implements this**:

| Layer              | Package                 | Example Files                                                    |
| ------------------ | ----------------------- | ---------------------------------------------------------------- |
| **Domain**         | `device.domain`         | `TrustedDevice.java`, `TrustedDeviceRepository.java` (interface) |
| **Application**    | `device.application`    | `VerifyOtpUseCase.java`, `ListDevicesUseCase.java`               |
| **Infrastructure** | `device.infrastructure` | `TrustedDeviceRepositoryAdapter.java`, `RedisOtpService.java`    |
| **API**            | `device.api`            | `DeviceController.java`                                          |

**Why this matters in interviews**:

- **Testability**: You can test business logic without databases (mock the port interfaces)
- **Swappability**: Replace PostgreSQL with MongoDB by only changing the adapter
- **Dependency Rule**: Inner layers NEVER know about outer layers

**What to say in an interview**:

> "We use hexagonal architecture. The domain layer defines contracts (interfaces like `TrustedDeviceRepository`), and infrastructure provides implementations (like `TrustedDeviceRepositoryAdapter` using JPA). This means we can swap PostgreSQL for DynamoDB without touching business logic."

---

### 1.2 Domain-Driven Design (DDD)

**Interview Question**: _"How do you organize modules in a large system?"_

Our project uses **bounded contexts**:

```
com.fintech.ewallet
├── identity/          ← Bounded Context: User Identity & Auth
│   ├── domain/        (User entity, UserRepository interface)
│   ├── application/   (LoginUseCase, RegisterUserUseCase)
│   ├── infrastructure/(UserJpaEntity, JwtTokenProvider)
│   └── api/           (AuthController)
│
├── device/            ← Bounded Context: Device Management
│   ├── domain/        (TrustedDevice, OtpService interface)
│   ├── application/   (VerifyOtpUseCase, RevokeDeviceUseCase)
│   ├── infrastructure/(RedisOtpService, DeviceFingerprintServiceImpl)
│   └── api/           (DeviceController)
│
├── wallet/            ← Bounded Context: Financial Operations (Phase 2)
├── kyc/               ← Bounded Context: User Verification (Phase 1.5)
└── shared/            ← Cross-cutting concerns
    ├── config/        (SecurityConfig)
    ├── exception/     (DomainException hierarchy)
    └── security/      (TokenBlacklistService interface)
```

**What to say in an interview**:

> "Each module is a bounded context with its own domain, application, and infrastructure layers. The `identity` context owns the User entity and authentication. The `device` context manages trusted devices. They communicate through well-defined interfaces, not direct database access."

---

### 1.3 Use Case Pattern (Clean Architecture)

**Interview Question**: _"How do you organize business logic?"_

Each business operation is an explicit **Use Case** class:

```java
// LoginUseCase.java — One class, one responsibility
@Service
public class LoginUseCase {
    public LoginResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
        // 1. Find user
        // 2. Check if locked
        // 3. Verify password
        // 4. Register device
        // 5. Generate tokens
        // 6. Return response
    }
}
```

**Benefits**:

- Each use case is independently testable
- Clear separation: `LoginUseCase`, `LogoutUseCase`, `RefreshTokenUseCase`
- Easy to trace: "Where does login happen?" → `LoginUseCase.java`

---

## 2. Authentication & Authorization

### 2.1 JWT (JSON Web Tokens)

**Interview Question**: _"How would you design an authentication system?"_

#### How JWT works in our system:

```
Login Flow:
──────────

Client                          Server                      Redis
  │                                │                           │
  │  POST /login                   │                           │
  │  {phone, password, deviceId}   │                           │
  │ ──────────────────────────────▶│                           │
  │                                │                           │
  │                                │ 1. Verify credentials     │
  │                                │ 2. Register/update device │
  │                                │ 3. Generate tokens:       │
  │                                │    - Access Token (1hr)   │
  │                                │    - Refresh Token (7d)   │
  │                                │      with deviceId claim  │
  │                                │                           │
  │  {accessToken, refreshToken}   │                           │
  │ ◀──────────────────────────────│                           │
  │                                │                           │

API Request:                       │                           │
  │  GET /api/v1/devices           │                           │
  │  Authorization: Bearer <JWT>   │                           │
  │ ──────────────────────────────▶│                           │
  │                                │ 1. Extract JWT            │
  │                                │ 2. Verify signature       │
  │                                │ 3. Check blacklist ──────▶│ Is blacklisted?
  │                                │                    ◀──────│ No
  │                                │ 4. Set SecurityContext     │
  │                                │ 5. Process request         │
  │  Response                      │                           │
  │ ◀──────────────────────────────│                           │
```

#### Token Structure

```
Access Token Claims:
{
  "jti": "unique-id-for-blacklisting",      ← Token ID
  "sub": "user-uuid",                        ← Subject (User ID)
  "phone": "+967XXXXXXXXX",
  "name": "Mustafa",
  "kyc": "PENDING",
  "type": "access",                          ← Token type
  "iat": 1708000000,                         ← Issued at
  "exp": 1708003600                          ← Expires (1 hour)
}

Refresh Token Claims:
{
  "jti": "unique-id-for-blacklisting",
  "sub": "user-uuid",
  "deviceId": "client-generated-uuid",       ← NEW in Phase 1.4
  "type": "refresh",
  "iat": 1708000000,
  "exp": 1708604800                          ← Expires (7 days)
}
```

**What to say in an interview**:

> "We use JWTs with asymmetric signing. Access tokens are short-lived (1 hour), stateless, and contain user claims. Refresh tokens are long-lived (7 days), device-specific, and support rotation. We use the `jti` claim for token blacklisting on logout."

---

### 2.2 Refresh Token Rotation

**Interview Question**: _"How do you handle token refresh securely?"_

```
Token Rotation Flow:
────────────────────

Client                         Server                        Redis
  │                               │                             │
  │  POST /refresh                │                             │
  │  Authorization: Bearer <RT1>  │                             │
  │ ─────────────────────────────▶│                             │
  │                               │ 1. Parse RT1                │
  │                               │ 2. Check blacklist ────────▶│ Is RT1 blacklisted?
  │                               │                     ◀──────│ No
  │                               │ 3. RT1 is valid             │
  │                               │ 4. Blacklist RT1 ──────────▶│ SET rt1 blacklisted TTL 7d
  │                               │ 5. Generate new AT2 + RT2   │
  │                               │                             │
  │  {accessToken: AT2,           │                             │
  │   refreshToken: RT2}          │                             │
  │ ◀─────────────────────────────│                             │

If attacker steals RT1 AFTER rotation:
  │  POST /refresh                │                             │
  │  Authorization: Bearer <RT1>  │                             │
  │ ─────────────────────────────▶│                             │
  │                               │ Check blacklist ───────────▶│ Is RT1 blacklisted?
  │                               │                     ◀──────│ YES! 🚫
  │  401 Unauthorized             │                             │
  │ ◀─────────────────────────────│                             │
```

**Why rotation matters**:

- If an attacker steals a refresh token, it becomes invalid after the legitimate user refreshes
- Single-use tokens = damage is limited

---

### 2.3 Token Blacklisting (Logout)

**Interview Question**: _"JWTs are stateless. How do you implement logout?"_

**The problem**: JWTs can't be "invalidated" because they're stateless. Once issued, they're valid until expiry.

**Our solution**: Redis blacklist with TTL matching token expiry.

```java
// RedisTokenBlacklistService.java
public void blacklistToken(String tokenId, Duration ttl) {
    String key = "blacklist:token:" + tokenId;
    redisTemplate.opsForValue().set(key, "blacklisted", ttl);
    // TTL auto-removes key when token would have expired anyway
}

public boolean isBlacklisted(String tokenId) {
    return Boolean.TRUE.equals(
        redisTemplate.hasKey("blacklist:token:" + tokenId)
    );
}
```

**Why Redis?**

- **O(1) lookup** — every request checks the blacklist
- **TTL** — keys auto-expire, no cleanup needed
- **In-memory** — microsecond response times
- **Scalable** — Redis Cluster for high availability

**What to say in an interview**:

> "We blacklist the token's unique ID (`jti` claim) in Redis with a TTL matching the token's remaining lifetime. This way, Redis auto-cleans expired entries. Every request checks the blacklist in the JWT filter — O(1) lookup since Redis is in-memory."

---

## 3. Security Design

### 3.1 Device Fingerprinting

**Interview Question**: _"How do you detect suspicious logins?"_

```java
// DeviceFingerprintServiceImpl.java
public String calculateFingerprint(HttpServletRequest request, String deviceId) {
    String userAgent = request.getHeader("User-Agent");
    String acceptLanguage = request.getHeader("Accept-Language");

    String raw = deviceId + "|" + userAgent + "|" + acceptLanguage;
    return SHA256(raw);  // Deterministic hash
}
```

**Design decisions**:
| Factor | Included? | Why |
|--------|-----------|-----|
| User-Agent | ✅ Yes | Stable, identifies browser + OS |
| Accept-Language | ✅ Yes | Rarely changes |
| IP Address | ❌ No | Changes on mobile networks, VPNs |
| Screen Resolution | ❌ No | Not available in server-side headers |

**What to say in an interview**:

> "We calculate a SHA-256 fingerprint from the User-Agent, Accept-Language, and a client-side device ID. We deliberately exclude IP addresses because mobile users switch networks frequently. The fingerprint helps us recognize returning devices without forcing re-authentication."

---

### 3.2 OTP (One-Time Password) Verification

**Interview Question**: _"How do you verify user identity on new devices?"_

```
New Device Flow:
────────────────

User logs in from Phone (1st device):
  → Password verified ✅
  → Device auto-trusted (isPrimary = true)
  → No OTP needed

User logs in from Laptop (2nd device):
  → Password verified ✅
  → New device detected → OTP required
  → OTP sent to phone (console in dev, SMS in prod)
  → User enters OTP → Device trusted
```

**OTP Storage** (Redis):

```
Key: "otp:code:+967XXXXXXXXX"     Value: "042387"     TTL: 5 minutes
Key: "otp:ratelimit:+967XXXXXXXXX" Value: "3"           TTL: 1 hour
```

**Security features**:

- OTP expires in 5 minutes (TTL)
- Max 5 OTP requests per hour (rate limiting)
- OTP deleted after successful verification (single-use)

---

### 3.3 Password Security

**Interview Question**: _"How do you store passwords?"_

```java
// BCrypt with 12 rounds (~0.3 seconds per hash)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

**Why BCrypt?**

- **Salted** — Each password gets a unique random salt
- **Adaptive** — 12 rounds = ~300ms to hash (too slow for brute-force)
- **One-way** — Can't reverse the hash

**Account Lockout** (implemented in `User.java`):

```
Failed attempts: 0 → 1 → 2 → 3 → 4 → 5 → LOCKED! 🔒
After 5 failed attempts, account is locked until admin/user unlocks
```

---

### 3.4 Stateless Session Management

**Interview Question**: _"How do you handle sessions in a distributed system?"_

```java
// SecurityConfig.java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

**Stateless vs Stateful**:

| Aspect          | Stateful (Sessions)                          | Stateless (JWT) — Our Choice      |
| --------------- | -------------------------------------------- | --------------------------------- |
| Storage         | Server memory/DB                             | Client-side (token)               |
| Scaling         | Need sticky sessions or shared session store | Any server can handle any request |
| Logout          | Delete session from server                   | Blacklist token in Redis          |
| Performance     | DB lookup per request                        | Cryptographic verification        |
| Mobile-friendly | Cookie-based (problematic)                   | Header-based (easy)               |

**What to say in an interview**:

> "We chose stateless JWT authentication because it scales horizontally. Any server instance can verify a token without querying a central session store. The tradeoff is that logout requires a blacklist (Redis), but this is faster than session lookups."

---

## 4. Database Design

### 4.1 Schema Design

**Interview Question**: _"Design the database for an e-wallet system."_

```sql
-- Users table (Phase 1.2)
CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    phone_number          VARCHAR(15) UNIQUE NOT NULL,     -- +967XXXXXXXXX
    email                 VARCHAR(255),
    password_hash         VARCHAR(255) NOT NULL,           -- BCrypt
    full_name             VARCHAR(100) NOT NULL,
    kyc_status            VARCHAR(20) DEFAULT 'PENDING',   -- VARCHAR, not ENUM
    status                VARCHAR(20) DEFAULT 'ACTIVE',
    preferred_language    VARCHAR(10) DEFAULT 'ar',
    failed_login_attempts INTEGER DEFAULT 0,
    locked_until          TIMESTAMP,
    last_login_at         TIMESTAMP,
    created_at            TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP NOT NULL,
    deleted_at            TIMESTAMP                        -- Soft delete!
);

-- Trusted devices (Phase 1.4)
CREATE TABLE trusted_devices (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    device_id       VARCHAR(255) NOT NULL,
    fingerprint     VARCHAR(255) NOT NULL,
    device_name     VARCHAR(100),                          -- "Chrome on Windows"
    user_agent      TEXT,
    last_ip_address VARCHAR(45),                           -- IPv6 ready
    is_primary      BOOLEAN DEFAULT FALSE,
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL,
    UNIQUE(user_id, device_id)
);

CREATE INDEX idx_trusted_devices_user ON trusted_devices(user_id);
CREATE INDEX idx_trusted_devices_fingerprint ON trusted_devices(fingerprint);
```

### 4.2 Key Design Decisions

**UUID vs Auto-increment IDs**:

```
Auto-increment: 1, 2, 3, 4, 5...
  ❌ Predictable: attacker can guess /users/1, /users/2
  ❌ Merge conflicts in distributed systems

UUID: 550e8400-e29b-41d4-a716-446655440000
  ✅ Unguessable
  ✅ Generated client-side or server-side
  ✅ Works across distributed systems
```

**VARCHAR vs ENUM for status fields**:

```java
// We use VARCHAR, not PostgreSQL ENUM
// Why?
// 1. Adding new status = ALTER TYPE + ALTER TABLE (downtime!)
// 2. VARCHAR just needs new Java enum value
// 3. Easier migrations
kyc_status VARCHAR(20) DEFAULT 'PENDING'
```

**Soft Delete** (never lose user data):

```sql
-- Instead of:  DELETE FROM users WHERE id = ?
-- We do:       UPDATE users SET deleted_at = NOW() WHERE id = ?

-- Queries filter deleted records:
SELECT * FROM users WHERE deleted_at IS NULL
```

**What to say in an interview**:

> "We use UUIDs for security and distributed-system compatibility. Status fields are VARCHAR instead of database ENUMs to avoid schema migrations when adding new statuses. We implement soft deletes using a `deleted_at` timestamp — financial regulations often require data retention, and this makes recovery trivial."

---

### 4.3 Database Migrations (Flyway)

**Interview Question**: _"How do you manage database schema changes?"_

```
src/main/resources/db/migration/
├── V1__create_users_table.sql         ← Phase 1.1
├── V2__add_users_fields.sql           ← Phase 1.2
└── V3__create_trusted_devices.sql     ← Phase 1.4
```

**Rules**:

- Migration files are **immutable** — never edit an applied migration
- Naming: `V{number}__{description}.sql`
- Flyway tracks applied migrations in `flyway_schema_history` table
- Rollback = new migration that reverses changes

**What to say in an interview**:

> "We use Flyway for versioned, repeatable migrations. Each migration is immutable once applied. This gives us full audit trail of schema changes and ensures every environment (dev, staging, prod) has identical schemas."

---

## 5. Caching & In-Memory Stores

### 5.1 Redis Usage Patterns

**Interview Question**: _"When and how would you use Redis?"_

We use Redis for three distinct purposes:

```
Redis Usage in Our System:
──────────────────────────

1. TOKEN BLACKLIST (Phase 1.3)
   Key Pattern:  "blacklist:token:{jti}"
   Value:        "blacklisted"
   TTL:          Remaining token lifetime
   Purpose:      Invalidate JWTs on logout
   Access:       Every authenticated request (O(1) lookup)

2. OTP STORAGE (Phase 1.4)
   Key Pattern:  "otp:code:{phoneNumber}"
   Value:        "042387" (6-digit code)
   TTL:          5 minutes
   Purpose:      Store verification codes for new devices
   Access:       On OTP generate/verify only

3. OTP RATE LIMITING (Phase 1.4)
   Key Pattern:  "otp:ratelimit:{phoneNumber}"
   Value:        Counter (incremented with INCR)
   TTL:          1 hour
   Purpose:      Limit OTP requests to 5/hour
   Access:       On each OTP request
```

**Why Redis and not database?**
| Factor | PostgreSQL | Redis |
|--------|-----------|-------|
| Latency | ~1-10ms | ~0.1ms |
| TTL support | Manual cleanup | Built-in auto-expiry |
| Throughput | ~10K ops/sec | ~100K ops/sec |
| Use case | Persistent data | Ephemeral/temporary data |

**What to say in an interview**:

> "We use Redis for 3 things: token blacklisting (checked on every request, needs sub-millisecond latency), OTP codes (ephemeral, 5-min TTL), and rate limiting (atomic INCR with TTL). All three are temporary data that benefit from Redis's TTL feature — zero cleanup needed."

---

## 6. API Design

### 6.1 RESTful API Design

**Interview Question**: _"How do you design APIs?"_

```
Our API Endpoints:
──────────────────

Authentication (Public):
  POST   /api/v1/auth/register          ← Create account
  POST   /api/v1/auth/login             ← Login + device binding
  POST   /api/v1/auth/refresh           ← Rotate tokens
  POST   /api/v1/auth/logout            ← Blacklist tokens

Device Management (Mixed):
  GET    /api/v1/devices                ← List user's devices (Protected)
  DELETE /api/v1/devices/{id}           ← Revoke device (Protected)
  POST   /api/v1/devices/request-otp   ← Request OTP (Public)
  POST   /api/v1/devices/verify-otp    ← Verify & bind device (Public)
```

**Design principles**:

- **Versioning**: `/api/v1/` prefix for backwards compatibility
- **Nouns, not verbs**: `/devices` not `/getDevices`
- **HTTP methods**: GET (read), POST (create/action), DELETE (remove)
- **Status codes**: 200 (OK), 201 (Created), 204 (No Content), 401 (Unauthorized), 429 (Rate Limited)

### 6.2 Request/Response Design

```json
// POST /api/v1/auth/login
// Request:
{
  "phoneNumber": "+967777123456",
  "password": "MySecurePass123!",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": null  // Optional, auto-generated
}

// Response (200 OK):
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600000,
  "user": {
    "id": "user-uuid",
    "fullName": "Mustafa",
    "phoneNumber": "+967777123456",
    "kycStatus": "PENDING"
  }
}
```

---

## 7. Data Consistency & Transactions

### 7.1 Transactional Boundaries

**Interview Question**: _"How do you ensure data consistency?"_

```java
@Transactional  // All-or-nothing
public LoginResponse execute(LoginRequest request, HttpServletRequest httpRequest) {
    // 1. Verify password            → READ user
    // 2. Reset failed attempts      → WRITE user
    // 3. Register device            → WRITE device
    // 4. Generate tokens            → Pure computation (no DB)

    // If step 3 fails → step 2 is rolled back automatically
}
```

**Key principle**: The `@Transactional` annotation ensures that **all database writes either ALL succeed or ALL fail**. If device registration fails, the login attempt counter reset is also rolled back.

---

## 8. Rate Limiting

**Interview Question**: _"How do you prevent API abuse?"_

```
OTP Rate Limiting Implementation:
─────────────────────────────────

Request 1: POST /devices/request-otp
  Redis: INCR "otp:ratelimit:+967..." → 1
  Redis: EXPIRE "otp:ratelimit:+967..." 3600
  → OTP sent ✅

Request 2-5: Same...
  Redis: INCR → 2, 3, 4, 5
  → OTP sent ✅

Request 6:
  Redis: GET "otp:ratelimit:+967..." → 5
  5 >= MAX_REQUESTS_PER_HOUR (5)
  → 429 Too Many Requests 🚫
```

**Scaling rate limiting**:

- **Single server**: Uses Redis to track per-user counters
- **Distributed**: Redis is shared across all server instances
- **Sliding window**: Could upgrade to Redis sorted sets for more precision

---

## 9. Scalability Patterns

### 9.1 Horizontal Scaling Strategy

**Interview Question**: _"How would you scale this system to 10M users?"_

```
Current Architecture (Monolith):
────────────────────────────────

┌─────────┐    ┌─────────────────┐    ┌──────────┐    ┌───────────┐
│  Client  │──▶│  Spring Boot    │──▶│PostgreSQL│    │   Redis   │
│  (App)   │    │  (Single JAR)   │    │ (Users,   │    │(Blacklist,│
│          │    │                 │    │  Devices) │    │ OTP)      │
└─────────┘    └─────────────────┘    └──────────┘    └───────────┘


Scaled Architecture (Future):
─────────────────────────────

                    ┌─────────────┐
                    │ Load Balancer│
                    └─────┬───────┘
              ┌───────────┼───────────┐
              ▼           ▼           ▼
         ┌─────────┐ ┌─────────┐ ┌─────────┐
         │Server 1 │ │Server 2 │ │Server 3 │     ← Stateless (JWT!)
         └────┬────┘ └────┬────┘ └────┬────┘
              │           │           │
         ┌────┴───────────┴───────────┴────┐
         │        PostgreSQL (Primary)      │     ← Read replicas possible
         │        Redis Cluster             │     ← Shared across instances
         └─────────────────────────────────┘
```

**Why our design scales**:

1. **Stateless auth (JWT)** → Any server can handle any request
2. **Redis for shared state** → Token blacklist works across all instances
3. **No sticky sessions needed** → Simple round-robin load balancing
4. **Bounded contexts** → Each module could become a microservice

### 9.2 Database Scaling

```
Read-Heavy Operations (user profile, device list):
  → Add PostgreSQL read replicas

Write-Heavy Operations (login tracking, transactions):
  → Partition by user_id
  → Sharding strategy: hash(user_id) % shard_count
```

---

## 10. DevOps & Infrastructure

### 10.1 Container Orchestration

**Interview Question**: _"How do you deploy your services?"_

```yaml
# compose.yaml — Docker Compose for local dev
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ewallet_dev
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
```

### 10.2 Environment Configuration

```
Profile-based configuration:
  application.yml          ← Shared defaults
  application-dev.yml      ← Development (H2/Postgres, console OTP)
  application-staging.yml  ← Staging (real DB, test SMS)
  application-prod.yml     ← Production (secure secrets, real SMS, no Swagger)
```

**Sensitive config**:

```yaml
# NEVER hardcode secrets
jwt:
  secret: ${JWT_SECRET} # From environment variable
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000
```

---

## 11. Common Interview Questions

### ❓ "Design a login system"

**Your answer using this project**:

> 1. User submits phone + password + deviceId
> 2. Server finds user by phone, checks if account is locked
> 3. BCrypt compares password hash (12 rounds)
> 4. On 5 failed attempts → lock account
> 5. On success → register/update device, calculate fingerprint
> 6. Generate JWT access token (1hr) + refresh token (7d, device-specific)
> 7. Return tokens to client
> 8. Every subsequent request includes JWT in `Authorization` header
> 9. JWT filter validates signature, checks Redis blacklist, sets security context

---

### ❓ "How do you handle logout with JWTs?"

> JWTs are stateless and can't be revoked natively. We store the token's unique `jti` claim in Redis with a TTL matching its remaining lifetime. On every request, the JWT filter checks Redis before accepting the token. Redis auto-deletes expired entries, so no cleanup is needed.

---

### ❓ "How would you handle multi-device login?"

> Each device gets a unique `deviceId` stored in the `trusted_devices` table. The refresh token contains a `deviceId` claim, so each device has independent sessions. Users can view all devices via `GET /devices` and revoke any device via `DELETE /devices/{id}`. This lets "logout from phone" without affecting "laptop session."

---

### ❓ "How do you prevent brute-force attacks?"

> Three layers of protection:
>
> 1. **BCrypt** — 12-round hashing makes each password check ~300ms (too slow for brute force)
> 2. **Account lockout** — 5 failed attempts → account locked
> 3. **OTP rate limiting** — Max 5 OTP requests per hour per phone number (Redis counter with TTL)

---

### ❓ "Why hexagonal architecture over layered architecture?"

> Layered architecture creates tight coupling — if I change my database, I might need to change my service layer. Hexagonal architecture defines interfaces (ports) in the domain layer, and implementations (adapters) in the infrastructure layer. I can swap PostgreSQL for DynamoDB by only changing the adapter — zero changes to business logic.

---

### ❓ "How do you ensure data consistency in a distributed system?"

> For local consistency: Spring's `@Transactional` ensures all-or-nothing writes within a single service.
> For distributed consistency (future): Event-driven architecture with eventual consistency — e.g., after a money transfer, publish an event that updates both wallets asynchronously with compensation logic for failures.

---

### ❓ "What's the difference between authentication and authorization?"

> **Authentication** = "Who are you?" → Login with phone + password → Get JWT
> **Authorization** = "What can you do?" → JWT filter checks token claims. We have `SecurityConfig` that defines which endpoints are public vs protected. Future: role-based access (ADMIN, USER, MERCHANT).

---

### ❓ "How would you scale Redis?"

> 1. **Redis Sentinel** — Automatic failover (leader election if primary Redis dies)
> 2. **Redis Cluster** — Sharding across multiple nodes (data partitioned by key hash)
> 3. **Read replicas** — For read-heavy operations like blacklist checks

---

### ❓ "What happens if Redis goes down?"

> **Graceful degradation**:
>
> - Token blacklist: Fail-open (allow requests) or fail-closed (reject all). Choice depends on security requirements. For a fintech app, fail-closed is safer — reject all requests until Redis recovers.
> - OTP: Users can't receive OTPs → show "Service temporarily unavailable" message
> - Rate limiting: Temporarily disable → accept higher risk of abuse

---

## 📚 Key Vocabulary Cheat Sheet

| Term                | Definition                                    | Our Example                        |
| ------------------- | --------------------------------------------- | ---------------------------------- |
| **JWT**             | JSON Web Token — self-contained auth token    | Access & refresh tokens            |
| **JTI**             | JWT ID — unique token identifier              | Used for blacklisting              |
| **BCrypt**          | Password hashing algorithm                    | 12-round password storage          |
| **TTL**             | Time To Live — auto-expiry                    | Redis keys for OTP, blacklist      |
| **HMAC**            | Hash-based Message Authentication Code        | JWT signature verification         |
| **OTP**             | One-Time Password                             | 6-digit SMS code                   |
| **Fingerprint**     | Device identifier hash                        | SHA-256(UA + Lang + deviceId)      |
| **Idempotent**      | Same request = same result                    | GET endpoints, token refresh       |
| **ACID**            | Atomicity, Consistency, Isolation, Durability | @Transactional                     |
| **Bounded Context** | Self-contained domain module                  | identity/, device/, wallet/        |
| **Port**            | Interface defining a capability               | `TrustedDeviceRepository`          |
| **Adapter**         | Implementation of a port                      | `TrustedDeviceRepositoryAdapter`   |
| **Use Case**        | Single business operation                     | `LoginUseCase`, `VerifyOtpUseCase` |
| **Soft Delete**     | Mark as deleted, don't remove                 | `deleted_at` timestamp             |

---

> 💡 **Pro Tip**: In interviews, always connect concepts to **tradeoffs**. Don't just say "we use Redis" — say "we chose Redis over database-stored sessions because we need sub-millisecond blacklist lookups on every request, and Redis's TTL feature eliminates the need for cleanup jobs."
