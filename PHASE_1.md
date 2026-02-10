# Phase 1: Foundation & Identity

> **Goal**: Set up project infrastructure, authentication, user management, and device security.
> **Estimated Duration**: Weeks 1–3

---

## Phase 1 Overview

| Step | Title                      | Scope                                         | Status        |
| ---- | -------------------------- | --------------------------------------------- | ------------- |
| 1.1  | Project Setup              | pom.xml, package structure, profiles, configs | 🟡 Discussion |
| 1.2  | User & Identity Management | User entity, registration, login              | ⬜ Pending    |
| 1.3  | JWT Authentication         | Token generation, refresh, validation         | ⬜ Pending    |
| 1.4  | Device Binding & Security  | Trusted devices, fingerprinting               | ⬜ Pending    |
| 1.5  | KYC Verification           | Document upload, admin review                 | ⬜ Pending    |

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

1. **YAML or Properties** for configuration files?
2. **H2 in-memory** or **PostgreSQL via Docker** for local development?
3. **Strict hexagonal** (separate JPA entities from domain entities) or **pragmatic** (JPA annotations on domain classes)?
4. **Error codes**: string (`VALIDATION_FAILED`) or numeric (`1001`)?
5. **Arabic error messages** from the start, or add later?
6. **Package naming**: `identity` vs `auth` vs `user`?
7. **Any additional dependencies** you want (MapStruct, SpringDoc/Swagger, etc.)?
