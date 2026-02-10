# 🏗️ Implementation Plan: Fintech E-Wallet System

> **Phased Approach for Building a Production-Ready Digital Wallet**

---

## Overview

This implementation plan breaks down the E-Wallet system into **5 phases**, ordered by dependency and business priority. Each phase builds on the previous one.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         IMPLEMENTATION TIMELINE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  PHASE 1        PHASE 2         PHASE 3         PHASE 4         PHASE 5    │
│  Foundation     Core Wallet     Transfers       Extensions      Production │
│  ───────────    ───────────     ─────────       ──────────      ──────────│
│  2-3 weeks      2-3 weeks       3-4 weeks       2-3 weeks       2 weeks    │
│                                                                             │
│  ┌──────────┐   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌────────┐ │
│  │ Project  │   │ Wallet & │    │ P2P      │    │ Referral │    │ Deploy │ │
│  │ Setup    │──▶│ Ledger   │──▶│ Transfer │──▶│ Privacy  │──▶│ Monitor│ │
│  │ Auth     │   │ Balance  │    │ Exchange │    │ Receive  │    │ Scale  │ │
│  │ User     │   │ System   │    │ Limits   │    │ i18n     │    │        │ │
│  └──────────┘   └──────────┘    └──────────┘    └──────────┘    └────────┘ │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Foundation & Identity (Weeks 1-3)

### Goal

Set up project infrastructure, authentication, user management, and device security.

### 1.1 Project Setup

#### [NEW] `pom.xml` - Maven Configuration

```
Dependencies:
- Spring Boot 3.2.x (Web, Security, Data JPA, Validation)
- Java 21
- PostgreSQL driver
- H2 (test)
- Flyway (migrations)
- JWT (jjwt-api 0.12.x)
- BCrypt
- Micrometer (metrics)
- Lombok (optional, for reducing boilerplate)
```

#### [NEW] Package Structure (Hexagonal Architecture + Feature Modules)

> **Why this structure?** Feature-first organization keeps related code together, enables clean dependency boundaries, and makes future microservice extraction trivial.

```
src/main/java/com/fintech/ewallet/
├── EwalletApplication.java
│
├── shared/                          # ━━━ Cross-cutting concerns ━━━
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtConfig.java
│   │   ├── AsyncConfig.java
│   │   └── LocaleConfig.java
│   ├── domain/                      # Shared value objects
│   │   ├── Money.java
│   │   ├── Currency.java
│   │   └── PhoneNumber.java
│   ├── exception/
│   │   ├── DomainException.java
│   │   └── ErrorCodes.java
│   └── util/
│       ├── IdGenerator.java
│       └── LocalizedFormatter.java
│
├── identity/                        # ━━━ 🟦 User & Auth Module ━━━
│   ├── domain/
│   │   ├── User.java                # Domain entity (no JPA)
│   │   ├── UserService.java         # Domain logic
│   │   └── UserRepository.java      # Port (interface)
│   ├── application/                 # Use cases
│   │   ├── RegisterUserUseCase.java
│   │   ├── LoginUseCase.java
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       └── LoginResponse.java
│   ├── infrastructure/              # Adapters (implementations)
│   │   ├── persistence/
│   │   │   ├── UserJpaRepository.java
│   │   │   └── UserJpaEntity.java   # JPA entity (separate!)
│   │   └── security/
│   │       └── JwtTokenProvider.java
│   └── api/
│       └── AuthController.java
│
├── device/                          # ━━━ 🟩 Device Binding Module ━━━
│   ├── domain/
│   │   ├── TrustedDevice.java
│   │   ├── DeviceService.java
│   │   └── DeviceRepository.java
│   ├── application/
│   │   ├── BindDeviceUseCase.java
│   │   └── dto/
│   ├── infrastructure/
│   │   └── persistence/
│   └── api/
│       └── DeviceController.java
│
├── kyc/                             # ━━━ 🟨 KYC Verification Module ━━━
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── api/
│       └── KycController.java
│
├── wallet/                          # ━━━ 🟧 Wallet & Balance Module ━━━
│   ├── domain/
│   │   ├── Wallet.java
│   │   ├── WalletStatus.java
│   │   ├── WalletService.java
│   │   └── WalletRepository.java
│   ├── application/
│   │   ├── CreateWalletUseCase.java
│   │   ├── GetBalanceUseCase.java
│   │   └── dto/
│   ├── infrastructure/
│   │   └── persistence/
│   └── api/
│       └── WalletController.java
│
├── ledger/                          # ━━━ 🟪 Double-Entry Ledger Module ━━━
│   ├── domain/
│   │   ├── LedgerEntry.java
│   │   ├── EntryType.java
│   │   ├── LedgerService.java
│   │   └── LedgerRepository.java
│   ├── application/
│   │   ├── RecordTransactionUseCase.java
│   │   └── ReconciliationUseCase.java
│   ├── infrastructure/
│   └── api/ (internal only)
│
├── transfer/                        # ━━━ 🟫 P2P Transfer Module ━━━
│   ├── domain/
│   │   ├── Transfer.java
│   │   ├── TransferStatus.java
│   │   ├── TransferService.java
│   │   └── TransferRepository.java
│   ├── application/
│   │   ├── PreviewTransferUseCase.java
│   │   ├── ExecuteTransferUseCase.java
│   │   ├── ReverseTransferUseCase.java
│   │   └── dto/
│   ├── infrastructure/
│   │   ├── persistence/
│   │   └── idempotency/
│   │       ├── IdempotencyFilter.java
│   │       └── IdempotencyStore.java
│   └── api/
│       └── TransferController.java
│
├── exchange/                        # ━━━ 💱 Currency Exchange Module ━━━
│   ├── domain/
│   │   ├── ExchangeRate.java
│   │   ├── ExchangeQuote.java
│   │   └── ExchangeService.java
│   ├── application/
│   │   ├── GetQuoteUseCase.java
│   │   └── ExecuteExchangeUseCase.java
│   ├── infrastructure/
│   │   └── rates/
│   │       └── ExternalRateProvider.java
│   └── api/
│       └── ExchangeController.java
│
├── limits/                          # ━━━ 🚧 Limits Engine Module ━━━
│   ├── domain/
│   │   ├── TransactionLimit.java
│   │   ├── UserTier.java
│   │   └── LimitService.java
│   ├── application/
│   └── infrastructure/
│
├── fee/                             # ━━━ 💰 Fee Calculation Module ━━━
│   ├── domain/
│   │   ├── FeeStructure.java
│   │   └── FeeService.java
│   └── application/
│
├── referral/                        # ━━━ 🎁 Referral Program Module ━━━
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── api/
│       └── ReferralController.java
│
├── privacy/                         # ━━━ 🔒 Privacy Settings Module ━━━
│   ├── domain/
│   │   ├── PrivacySettings.java
│   │   └── NameMaskingService.java
│   └── api/
│       └── PrivacyController.java
│
├── receive/                         # ━━━ 📥 Receive Transfers Module ━━━
│   ├── domain/
│   │   ├── IncomingTransfer.java
│   │   └── ReceiveTransferService.java
│   ├── infrastructure/
│   │   └── partner/
│   │       ├── PartnerNetworkClient.java
│   │       └── LocalAgentAdapter.java
│   └── api/
│       └── ReceiveController.java
│
├── i18n/                            # ━━━ 🌍 Internationalization Module ━━━
│   ├── domain/
│   │   └── LocalizationService.java
│   └── infrastructure/
│       └── MessageBundleLoader.java
│
├── admin/                           # ━━━ 👤 Admin Operations Module ━━━
│   ├── domain/
│   │   ├── Role.java
│   │   └── Permission.java
│   ├── application/
│   └── api/
│       ├── AdminUserController.java
│       └── AdminTransactionController.java
│
├── audit/                           # ━━━ 📋 Audit & Compliance Module ━━━
│   ├── domain/
│   │   └── AuditEntry.java
│   └── infrastructure/
│       ├── AuditInterceptor.java
│       └── AuditRepository.java
│
└── observability/                   # ━━━ 📊 Monitoring Module ━━━
    ├── MetricsConfig.java
    ├── HealthController.java
    └── RequestLoggingFilter.java
```

### Module Dependency Rules

```
┌─────────────────────────────────────────────────────────────────┐
│                    ALLOWED DEPENDENCIES                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   shared/ ◄───────── ALL MODULES CAN IMPORT                     │
│                                                                 │
│   identity/ ◄──────── wallet/, transfer/, admin/                │
│   wallet/ ◄────────── transfer/, exchange/, ledger/             │
│   ledger/ ◄────────── transfer/, exchange/, receive/            │
│   limits/ ◄────────── transfer/, exchange/                      │
│   fee/ ◄───────────── transfer/, exchange/                      │
│                                                                 │
│   ✗ NEVER: api/ → domain/  (controller talks to application)   │
│   ✗ NEVER: domain/ → infrastructure/ (ports don't know adapters)│
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### 1.2 User & Identity Management

#### [NEW] `domain/user/User.kt`

- User entity with UUID, phone, full name, password hash
- Embedded `PrivacySettings` and `KycStatus`
- Language preference field

#### [NEW] `domain/user/UserRepository.kt`

- `findByPhone(phone: String): User?`
- `existsByPhone(phone: String): Boolean`

#### [NEW] `api/v1/AuthController.kt`

| Endpoint                | Method | Description                 |
| ----------------------- | ------ | --------------------------- |
| `/api/v1/auth/register` | POST   | Register new user           |
| `/api/v1/auth/login`    | POST   | Login with phone + password |
| `/api/v1/auth/refresh`  | POST   | Refresh JWT token           |
| `/api/v1/auth/logout`   | POST   | Invalidate tokens           |

#### [NEW] `infrastructure/security/JwtService.kt`

- Generate access token (15 min) + refresh token (7 days)
- Validate and parse tokens
- Token blacklist for logout

---

### 1.3 Device Binding & Security

#### [NEW] `domain/device/TrustedDevice.kt`

- Device ID, fingerprint, user agent, IP
- Trust level, last used, is primary

#### [NEW] `api/v1/DeviceController.kt`

| Endpoint               | Method | Description          |
| ---------------------- | ------ | -------------------- |
| `/api/v1/devices`      | GET    | List trusted devices |
| `/api/v1/devices/{id}` | DELETE | Revoke device        |
| `/api/v1/devices/bind` | POST   | Add new device       |

#### [NEW] `domain/otp/OtpService.kt`

- 6-digit OTP generation
- Rate limiting (5 attempts per hour)
- TTL: 5 minutes

---

### 1.4 KYC Verification

#### [NEW] `domain/kyc/KycVerification.kt`

- ID type (National ID, Passport)
- Document number, issue/expiry dates
- Manual review status

#### [NEW] `api/v1/KycController.kt`

| Endpoint             | Method | Description               |
| -------------------- | ------ | ------------------------- |
| `/api/v1/kyc/submit` | POST   | Submit KYC documents      |
| `/api/v1/kyc/status` | GET    | Check verification status |

---

### 1.5 Database Migrations

#### [NEW] `resources/db/migration/V1__create_users.sql`

#### [NEW] `resources/db/migration/V2__create_devices.sql`

#### [NEW] `resources/db/migration/V3__create_kyc.sql`

---

## Phase 2: Core Wallet System (Weeks 4-6)

### Goal

Implement wallet creation, balance management, and double-entry ledger.

### 2.1 Wallet & Balance

#### [NEW] `domain/wallet/Wallet.kt`

- User wallet with currency code (YER, SAR, USD)
- Cached balance (BigDecimal, precision 19,4)
- Status: ACTIVE, FROZEN, CLOSED

#### [NEW] `domain/wallet/WalletService.kt`

- Auto-create 3 wallets on user registration
- `getBalance(userId, currency): BigDecimal`
- `freezeWallet(walletId)`

---

### 2.2 Double-Entry Ledger

#### [NEW] `domain/ledger/LedgerEntry.kt`

- Debit wallet, Credit wallet
- Amount, currency
- Entry type: TRANSFER, FEE, REFUND, etc.
- Transaction reference

#### [NEW] `domain/ledger/LedgerService.kt`

```kotlin
fun recordTransfer(
    fromWallet: UUID,
    toWallet: UUID,
    amount: BigDecimal,
    transactionId: UUID
): Pair<LedgerEntry, LedgerEntry>
```

- Atomic double-entry creation
- Zero-sum validation

---

### 2.3 System Wallets

#### [NEW] `resources/db/migration/V4__create_system_wallets.sql`

```sql
-- Liquidity wallets (source of all user deposits)
LIQUIDITY_YER, LIQUIDITY_SAR, LIQUIDITY_USD

-- Fee collection wallets
FEES_YER, FEES_SAR, FEES_USD
```

---

### 2.4 Balance Caching & Reconciliation

#### [NEW] `domain/wallet/BalanceReconciliationService.kt`

- Nightly job to compare wallet.balance vs SUM(ledger_entries)
- Alert on discrepancies > threshold

---

## Phase 3: Transfers & Exchange (Weeks 7-10)

### Goal

Implement P2P transfers, currency exchange, limits, and idempotency.

### 3.1 P2P Transfer Engine

#### [NEW] `domain/transfer/P2PTransfer.kt`

- UUID, idempotency key, reference ID
- Sender/Recipient wallet IDs
- Amount, currency, status
- State machine: PENDING → COMPLETED/FAILED/REVERSED

#### [NEW] `domain/transfer/TransferService.kt`

- Preview transfer (validate + show recipient name)
- Execute transfer (atomic, with ledger entries)
- Reverse transfer (compensating entries)

#### [NEW] `api/v1/TransferController.kt`

| Endpoint                    | Method | Description                  |
| --------------------------- | ------ | ---------------------------- |
| `/api/v1/transfers/preview` | POST   | Validate & preview           |
| `/api/v1/transfers/execute` | POST   | Execute with idempotency key |
| `/api/v1/transfers/{id}`    | GET    | Get transfer details         |
| `/api/v1/transfers/history` | GET    | User's transfer history      |

---

### 3.2 Currency Exchange

#### [NEW] `domain/exchange/ExchangeRate.kt`

- Base currency, target currency
- Rate, spread/margin
- Valid from/to timestamps

#### [NEW] `domain/exchange/ExchangeService.kt`

- `getRate(from, to): ExchangeRate`
- `executeExchange(userId, fromCurrency, toCurrency, amount, maxSlippageBps)`
- Slippage protection (reject if rate moved > threshold)

#### [NEW] `api/v1/ExchangeController.kt`

| Endpoint                   | Method | Description        |
| -------------------------- | ------ | ------------------ |
| `/api/v1/exchange/rates`   | GET    | Current rates      |
| `/api/v1/exchange/quote`   | POST   | Get quote with TTL |
| `/api/v1/exchange/execute` | POST   | Execute exchange   |

---

### 3.3 Limits Engine

#### [NEW] `domain/limits/TransactionLimit.kt`

- User tier (BASIC, VERIFIED, PREMIUM)
- Operation type (TRANSFER, EXCHANGE, WITHDRAWAL)
- Daily/Monthly limits
- Per-transaction max

#### [NEW] `domain/limits/LimitService.kt`

- `checkLimit(userId, operation, amount): LimitCheckResult`
- `recordUsage(userId, operation, amount)`
- `getRemainingLimit(userId, operation): RemainingLimit`

---

### 3.4 Fee Calculation

#### [NEW] `domain/fee/FeeStructure.kt`

- Fee type: FLAT, PERCENTAGE, TIERED
- Min/max fee caps
- Currency-specific fees

#### [NEW] `domain/fee/FeeService.kt`

- `calculateFee(operation, amount, currency): FeeResult`

---

### 3.5 Idempotency

#### [NEW] `infrastructure/idempotency/IdempotencyFilter.kt`

- Extract `Idempotency-Key` header
- Check cache for existing response
- Store response on success

#### [NEW] `domain/common/IdempotencyRecord.kt`

- Key, response hash, created at
- TTL: 24 hours

---

## Phase 4: Feature Extensions (Weeks 11-13)

### Goal

Add referral program, privacy settings, receive transfers, and multi-language.

### 4.1 Referral Program

#### [NEW] `domain/referral/Referral.kt`

- Referrer user ID, referred phone
- Referral code, status
- Reward amounts

#### [NEW] `domain/referral/ReferralService.kt`

- `inviteFriend(userId, phone, name)`
- `validateReferralCode(code)`
- `completeReferral(referredUserId)` - credit rewards

#### [NEW] `api/v1/ReferralController.kt`

| Endpoint                    | Method | Description      |
| --------------------------- | ------ | ---------------- |
| `/api/v1/referrals/invite`  | POST   | Send invitation  |
| `/api/v1/referrals/history` | GET    | Referral history |
| `/api/v1/referrals/stats`   | GET    | Stats & rewards  |

---

### 4.2 Privacy Settings

#### [MODIFY] `domain/user/User.kt`

- Add embedded `PrivacySettings`

#### [NEW] `domain/privacy/PrivacyService.kt`

- `maskName(fullName, locale): String`
- `getRecipientDisplayName(userId, requesterId): String`

#### [NEW] `api/v1/PrivacyController.kt`

| Endpoint                   | Method | Description      |
| -------------------------- | ------ | ---------------- |
| `/api/v1/privacy/settings` | GET    | Current settings |
| `/api/v1/privacy/settings` | PATCH  | Update settings  |

---

### 4.3 Receive Transfers (External)

#### [NEW] `domain/receive/IncomingTransfer.kt`

- Transfer reference, transaction number
- Partner code, sender info
- Status: PENDING, CLAIMED, CANCELLED, EXPIRED

#### [NEW] `domain/receive/ReceiveTransferService.kt`

- `validateTransfer(reference, txNumber): TransferDetails`
- `claimTransfer(userId, reference)`
- `cancelTransfer(userId, reference, reason)`

#### [NEW] `infrastructure/external/PartnerNetworkClient.kt`

- Interface for partner integrations
- Mock implementation for testing

---

### 4.4 Multi-Language Support

#### [NEW] `resources/i18n/messages_ar.properties`

#### [NEW] `resources/i18n/messages_en.properties`

#### [NEW] `infrastructure/i18n/LocalizationService.kt`

- `getMessage(code, args, locale): String`
- `formatAmount(amount, currency, locale): String`
- `formatDate(instant, locale): String`

#### [NEW] `config/LocaleConfig.kt`

- `LocaleResolver` bean
- Accept-Language header handling

---

## Phase 5: Production Readiness (Weeks 14-15)

### Goal

Admin operations, audit, observability, and deployment.

### 5.1 Admin Operations

#### [NEW] `domain/admin/Role.kt`

- SUPER_ADMIN, COMPLIANCE_OFFICER, SUPPORT_AGENT
- Permission matrix

#### [NEW] `api/admin/AdminUserController.kt`

- Freeze/unfreeze wallets
- Override limits
- View user details (with audit)

#### [NEW] `api/admin/AdminTransactionController.kt`

- Manual refunds (four-eyes approval)
- Transaction investigation

---

### 5.2 Audit & Reconciliation

#### [NEW] `domain/audit/AuditEntry.kt`

- Actor, action, target entity
- Before/after state (JSON)
- Timestamp, IP address

#### [NEW] `infrastructure/audit/AuditInterceptor.kt`

- Automatic audit logging for sensitive operations

#### [NEW] `domain/reconciliation/ReconciliationJob.kt`

- Scheduled nightly reconciliation
- Alert on discrepancies

---

### 5.3 Observability

#### [NEW] `config/MetricsConfig.kt`

- Custom Prometheus metrics
- Transfer counters, latency histograms

#### [NEW] `infrastructure/logging/RequestLoggingFilter.kt`

- Correlation ID propagation
- Structured JSON logging

#### [NEW] `api/HealthController.kt`

- `/health/live` - liveness probe
- `/health/ready` - readiness probe (DB, cache)

---

### 5.4 Security Hardening

#### [MODIFY] `config/SecurityConfig.kt`

- Rate limiting (100 req/min per IP)
- CORS configuration
- HTTPS enforcement

#### [NEW] `infrastructure/security/RateLimitFilter.kt`

- Token bucket algorithm
- Per-endpoint limits

---

## Verification Plan

### Automated Tests

#### Unit Tests

```bash
./mvnw test -Dtest="*Test"
```

- Service layer tests with mocks
- Domain logic tests
- Fee/limit calculation tests

#### Integration Tests

```bash
./mvnw test -Dtest="*IT"
```

- Repository tests with H2
- API endpoint tests with MockMvc
- Full transfer flow tests

#### End-to-End Tests

```bash
./mvnw test -Dtest="*E2E" -Dspring.profiles.active=e2e
```

- Complete user registration → transfer flow
- Exchange with slippage protection
- Referral completion flow

### Manual Verification

| Test Case         | Steps                                                                                                                        | Expected Result                                                       |
| ----------------- | ---------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| User Registration | 1. POST `/api/v1/auth/register` with phone+password<br>2. Check DB for user and 3 wallets                                    | User created with YER, SAR, USD wallets, balance 0                    |
| P2P Transfer      | 1. Deposit to sender wallet (admin)<br>2. POST `/transfers/preview`<br>3. POST `/transfers/execute`<br>4. Check both wallets | Sender balance decreased, recipient increased, ledger entries created |
| Idempotency       | 1. Execute transfer twice with same key                                                                                      | Second call returns cached result, no duplicate transfer              |
| Exchange          | 1. Quote exchange<br>2. Wait 10+ seconds<br>3. Execute                                                                       | Should fail with "quote expired"                                      |

---

## Dependencies & Prerequisites

| Dependency | Version | Purpose                                   |
| ---------- | ------- | ----------------------------------------- |
| JDK        | 21      | Runtime                                   |
| PostgreSQL | 15+     | Database                                  |
| Redis      | 7+      | Rate limiting, caching (optional Phase 5) |
| Docker     | Latest  | Local development                         |

---

## File Summary

| Phase     | New Files | Modified Files |
| --------- | --------- | -------------- |
| Phase 1   | ~25       | 0              |
| Phase 2   | ~15       | 2              |
| Phase 3   | ~20       | 5              |
| Phase 4   | ~15       | 3              |
| Phase 5   | ~12       | 4              |
| **Total** | **~87**   | **~14**        |

---

## User Review Required

> [!IMPORTANT]
> Before proceeding with implementation, please confirm:
>
> 1. Is the 5-phase approach acceptable?
> 2. Should we start with Phase 1 immediately?
> 3. Any features to prioritize or deprioritize?
> 4. Are there any existing code files I should be aware of?

---

_Last Updated: 2026-02-07_
