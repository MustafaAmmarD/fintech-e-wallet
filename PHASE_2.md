# Phase 2: Wallet & Ledger

> **Goal**: Build the financial backbone — wallets, balances, and a double-entry ledger system.
> **Estimated Duration**: Weeks 4–6

---

## Phase 2 Overview

> Current execution status (MVP): **Phase 2 implementation and transfer validation complete (2026-02-22).**
> Final status update: Steps `2.1` to `2.5` are complete; transfer scenarios were validated via Swagger.
> Completion checklist:
>
> - `2.1` Wallet Creation: ✅ Complete
> - `2.2` Double-Entry Ledger: ✅ Complete
> - `2.3` System Wallets: ✅ Complete
> - `2.4` Balance Management: ✅ Complete
> - `2.5` Wallet API: ✅ Complete

| Step | Title               | Scope                                       | Status     |
| ---- | ------------------- | ------------------------------------------- | ---------- |
| 2.1  | Wallet Creation     | Wallet entity, multi-currency, KYC gate     | ⬜ Pending |
| 2.2  | Double-Entry Ledger | Ledger entries, debit/credit, zero-sum rule | ⬜ Pending |
| 2.3  | System Wallets      | Liquidity pools, fee collection             | ⬜ Pending |
| 2.4  | Balance Management  | Cached balance, reconciliation, caching     | ⬜ Pending |
| 2.5  | Wallet API          | REST endpoints, transaction history         | ⬜ Pending |

---

## 2.1 Wallet Creation — Discussion

### Implementation Note (2026-02-20)

- Registration was updated to defer wallet provisioning.
- Wallets are no longer auto-created during `POST /api/v1/auth/register`.
- Wallet activation is now explicitly tied to KYC verification and admin approval flow.
- Added approval path in KYC module: `POST /api/v1/kyc/admin/documents/{documentId}/approve`.
- Added admin account review APIs for operations workflow:
  - `GET /api/v1/kyc/admin/accounts/pending`
  - `GET /api/v1/kyc/admin/accounts`
  - `GET /api/v1/kyc/admin/accounts/{userId}`
  - `POST /api/v1/kyc/admin/accounts/{userId}/approve`
  - `POST /api/v1/kyc/admin/accounts/{userId}/pend`
- Improved KYC pending-state consistency:
  - `pend` now reopens account documents as `PENDING` for re-review.
  - pending queue excludes accounts with zero pending documents.

> [!NOTE]
> A **wallet** is the most fundamental concept in our e-wallet system. It represents a user's money container — like a bank account, but digital and instant.

### What Is a Wallet?

Think of a wallet like a **bank account**:

- Has an **owner** (the user)
- Has a **currency** (YER, SAR, USD)
- Has a **balance** (how much money is in it)
- Has a **status** (active, frozen, closed)

**Real-World Analogy:**

Imagine you have 3 pockets:

- Left pocket = Yemeni Rials (YER)
- Right pocket = Saudi Riyals (SAR)
- Back pocket = US Dollars (USD)

Each pocket is a separate "wallet." You can't mix currencies — you can only put YER in the YER pocket.

---

### Why Multi-Currency?

**Yemen Context:**

- **YER** (Yemeni Rial) — Local currency for daily transactions
- **SAR** (Saudi Riyal) — Common because many Yemenis work in Saudi Arabia and send remittances
- **USD** (US Dollar) — International transactions, savings

**User Story:** Ahmed works in Saudi Arabia. He earns SAR, sends money to his family in Yemen in YER, and saves some in USD. He needs 3 wallets.

---

### When Should Wallets Be Created?

This is an important design decision. There are two approaches:

### 2.1 MVP Decision Lock (2026-02-20)

- Wallets are **not** created during `POST /api/v1/auth/register`.
- Wallets are created only after KYC approval via `POST /api/v1/kyc/admin/accounts/{userId}/approve`.
- Supported currencies for MVP are: `YER`, `SAR`, `USD`.
- One wallet per currency per user.
- KYC verification is required before wallet activation.

### 2.1.2 Current Code Contract (Verified)

- Wallet activation trigger is KYC account approval (`ApproveKycAccountUseCase` calls `createWalletsForUser`).
- Registration does not call wallet provisioning directly.
- Wallet provisioning creates `YER`, `SAR`, `USD` if missing (idempotent).
- Database enforces one-wallet-per-currency-per-user with unique `(user_id, currency)`.
- Wallet starts with `balance = 0` and `status = ACTIVE`.

**Option A: Auto-create on registration**

- User registers → System automatically creates 3 wallets (YER, SAR, USD)
- ✅ Simple for the user (wallets ready immediately)
- ❌ Wastes resources (user might never use SAR or USD)
- ❌ KYC issue: user might not be verified yet

**Option B: Create on demand (user chooses)**

- User registers → No wallets
- User clicks "Create YER Wallet" → Wallet created
- ✅ User only gets wallets they need
- ✅ KYC can be enforced (only verified users can create wallets)
- ❌ Extra step for the user

**Option C: Auto-create one, request others**

- User registers → System creates default wallet (YER)
- User can add SAR or USD wallets later
- ✅ Balance between convenience and flexibility

### 2.1 Decision Summary (Resolved)

1. **Wallet Creation Strategy:** Option A behavior moved from registration to KYC approval trigger.
2. **Supported Currencies:** `YER`, `SAR`, `USD` only for MVP.
3. **Wallet Limit:** One wallet per currency per user (enforced by unique constraint).
4. **KYC Requirement:** Yes, KYC is required before wallet activation.

---

## Design Decisions (Phase 2)

> [!IMPORTANT]
> **Decisions finalized on 2026-02-16 based on user feedback**

### 2.1 Wallet Creation Strategy

> [!WARNING]
> This block reflects an older draft. The active MVP behavior is locked in **2.1 MVP Decision Lock (2026-02-20)** above: wallets are created on KYC account approval, not on registration.

- **Decision**: **Auto-create 3 wallets (YER, SAR, USD) on registration.**
- **KYC Logic**: Since wallets are created _during_ registration (before KYC), they will start in a `PENDING_KYC` or `RESTRICTED` status.
- **Limit**: **1 wallet per currency** per user (Simplicity for MVP).
  - _Explanation for "Wallet Limit"_: We restrict users to 1 main wallet per currency to avoid complexity. In the future, we could allow "Savings Wallets", but for now, 1 user = 1 YER Wallet, 1 USD Wallet, etc.
- **Enforcement**: Users cannot deposit or send money until KYC is `VERIFIED`.

### 2.2 Ledger Design

- **Precision**:
  - **YER**: 0 decimals (display), store as standard 4-decimal for safety.
  - **USD/SAR**: 2 decimals.
- **Immutability**: **YES**.
  - _Explanation_: "Immutable" means **never edit or delete history**. If you accidentally send 100 YER, you cannot "delete" that row. You must create a _new_ transaction to "refund" 100 YER. This guarantees a perfect audit trail where history is never rewritten.
- **Balance Storage**: **Option C (Both)**.
  - _Explanation_: We will store the balance in the `count` column (for speed) _AND_ calculate it from the ledger (for accuracy). A background job will compare them nightly. If `Wallet Balance != Sum(Ledger Entries)`, the system alerts us to a bug.

### 2.3 System Wallets

- **Structure**: Separate system wallets for **each currency**.
  - `LIQUIDITY_YER`, `LIQUIDITY_SAR`, `LIQUIDITY_USD`
- **Fees**: **Separate Fee Wallets** (`FEES_YER`, etc.).
  - This keeps user money separate from company revenue.
- **Initial Balance**: **Zero (0) for Production**, **Seeded for Dev**.
  - _Explanation_: In production, money only exists if real cash enters the bank. However, for **Development/Testing**, we will "seed" the system with fake money (e.g., 1,000,000 YER) so you can simulate transactions without connecting to a real bank.

### 2.4 Balance Rules

- **Overdraft**: **NO**. Balance cannot go below 0.
- **Minimum Balance**: **0**. Users can withdraw everything.
- **Notifications**: **YES**. Push notification on every credit/debit.

### 2.5 API & History

- **Pagination**: **10 items** per page (Mobile friendly).
- **Details**: **Basic** (Date, Amount, Type).
- **Export**: **NO** (Not needed for MVP).

---

## 2.1 Wallet Creation — Implementation

> [!NOTE]
> **Status**: ✅ Complete (2026-02-16)

> [!WARNING]
> This implementation narrative uses the earlier registration-trigger model. For current MVP behavior, follow **2.1 MVP Decision Lock (2026-02-20)** and **2.1.2 Current Code Contract (Verified)**.

### What We Built

We implemented the core **Wallet domain** that allows each user to hold balances in multiple currencies. When a user registers, the system automatically creates 3 wallets: **YER**, **SAR**, and **USD**.

---

### Architecture Layers

Following Hexagonal Architecture, we created:

```
┌──────────────────────────────────────────────────────────┐
│  DOMAIN LAYER (wallet/domain/)                           │
│  ├── Wallet.java          → Domain entity (POJO)         │
│  ├── Currency.java        → Enum (YER, SAR, USD)         │
│  ├── WalletStatus.java    → Enum (ACTIVE, FROZEN, CLOSED)│
│  └── WalletRepository.java → Port (interface)            │
├──────────────────────────────────────────────────────────┤
│  APPLICATION LAYER (wallet/application/)                 │
│  └── CreateWalletUseCase.java → Auto-create logic        │
├──────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE LAYER (wallet/infrastructure/)           │
│  ├── WalletJpaEntity.java      → JPA persistence         │
│  ├── WalletJpaRepository.java  → Spring Data JPA         │
│  ├── WalletMapper.java         → Domain ↔ JPA converter  │
│  └── WalletRepositoryAdapter.java → Port implementation  │
├──────────────────────────────────────────────────────────┤
│  DATABASE                                                │
│  └── V5__create_wallets_table.sql → Flyway migration     │
└──────────────────────────────────────────────────────────┘
```

---

### Key Components

#### 1. Domain Entity: `Wallet.java`

Pure Java POJO with business logic:

**Fields:**

- `id`: UUID (primary key)
- `userId`: UUID (owner reference)
- `currency`: Currency enum
- `balance`: BigDecimal (cached for performance)
- `status`: WalletStatus enum
- `createdAt / updatedAt`: Instant timestamps

**Business Methods:**

- `credit(amount)`: Adds money, validates wallet is ACTIVE
- `debit(amount)`: Subtracts money, checks sufficient funds
- `freeze() / unfreeze()`: Status management

> [!IMPORTANT]
> **Why separate domain from JPA?** The domain entity has NO Spring/JPA annotations. This keeps business logic independent of infrastructure. If we switch from PostgreSQL to MongoDB, only the infrastructure layer changes.

---

#### 2. Value Objects: Enums

**`Currency.java`:**

```java
public enum Currency {
    YER,  // Yemeni Rial
    SAR,  // Saudi Riyal
    USD   // US Dollar
}
```

**`WalletStatus.java`:**

```java
public enum WalletStatus {
    ACTIVE,   // Normal operation
    FROZEN,   // Suspended (KYC issue, fraud alert)
    CLOSED    // Permanently closed
}
```

---

#### 3. Repository Port: `WalletRepository.java`

Domain interface defining data access needs:

```java
public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
    Optional<Wallet> findByUserIdAndCurrency(UUID userId, Currency currency);
    List<Wallet> findByUserId(UUID userId);
    boolean existsByUserIdAndCurrency(UUID userId, Currency currency);
}
```

> [!NOTE]
> This is a **port** in hexagonal architecture. The domain defines WHAT it needs, not HOW data is stored.

---

#### 4. Use Case: `CreateWalletUseCase.java`

Application service that orchestrates wallet creation:

**Logic:**

1. Check if wallet already exists (idempotent)
2. Create `Wallet` domain object
3. Save via repository

**Key Feature: Idempotency**

```java
if (!walletRepository.existsByUserIdAndCurrency(userId, Currency.YER)) {
    walletRepository.save(new Wallet(userId, Currency.YER));
}
```

This ensures calling `createWalletsForUser()` multiple times won't create duplicates.

---

#### 5. Infrastructure Adapter: `WalletRepositoryAdapter.java`

Implements the domain port using Spring Data JPA:

```java
@Component
@RequiredArgsConstructor
public class WalletRepositoryAdapter implements WalletRepository {
    private final WalletJpaRepository jpaRepository;
    private final WalletMapper mapper;

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = mapper.toEntity(wallet);
        WalletJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
    // ... other methods
}
```

**Pattern:**
Domain → Mapper → JPA Entity → Database  
Database → JPA Entity → Mapper → Domain

---

#### 6. Database Migration: `V5__create_wallets_table.sql`

```sql
CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, currency)  -- 1 wallet per currency per user
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);
```

**Key Constraints:**

- `UNIQUE(user_id, currency)`: Enforces 1 wallet per currency at database level
- `DECIMAL(19, 4)`: Supports precision for 4 decimals (even though YER uses 0, we store extra for safety)

---

### Integration with User Registration

**Modified: `RegisterUserUseCase.java`**

After creating a user, we now trigger wallet creation:

```java
@Transactional
public RegisterResponse execute(RegisterRequest request) {
    // ... create user ...
    User savedUser = userRepository.save(user);

    // NEW: Create 3 wallets for the user
    createWalletUseCase.createWalletsForUser(savedUser.getId());

    return new RegisterResponse(...);
}
```

> [!WARNING]
> **Transactional Integrity**: Both user creation and wallet creation happen in the SAME transaction. If wallet creation fails, user creation also rolls back. This prevents orphaned users without wallets.

---

### Testing the Implementation

**Manual Test:**

1. Register a new user via `/api/v1/auth/register`
2. Query the database:

```sql
SELECT * FROM wallets WHERE user_id = '<new-user-id>';
```

**Expected Result:**

```
| id   | user_id | currency | balance | status |
|------|---------|----------|---------|--------|
| ...  | ...     | YER      | 0.0000  | ACTIVE |
| ...  | ...     | SAR      | 0.0000  | ACTIVE |
| ...  | ...     | USD      | 0.0000  | ACTIVE |
```

---

### Design Rationale

**Q: Why auto-create wallets instead of on-demand?**  
**A:** User convenience. Most users will need at least YER. Pre-creating all 3 avoids extra API calls.

**Q: Why 3 currencies specifically?**  
**A:** Yemen context: YER (local), SAR (remittances from Saudi Arabia), USD (international/savings).

**Q: Why store balance in the wallet table if we have a ledger?**  
**A:** Performance. Reading balance from `wallets.balance` is O(1). Calculating from ledger entries is O(n). We'll reconcile them nightly to ensure accuracy.

**Q: What if a user never uses SAR or USD?**  
**A:** The wallet record is tiny (< 100 bytes). For 1 million users, 3 wallets each = 3 million records = ~300MB. Negligible storage cost for better UX.

---

## 2.2 Double-Entry Ledger — Discussion

> [!NOTE]
> This is the **most critical financial component**. Every transaction MUST be recorded as a double-entry to ensure money never appears or disappears.

### What is Double-Entry Bookkeeping?

**The Problem with Single-Entry:**

Imagine tracking money with a simple log:

```
Ahmed deposits 1000 YER  → Ahmed's balance = 1000
Ahmed sends 500 YER      → Ahmed's balance = 500
```

**Issues:**

- Where did the 500 YER go? We don't know!
- If there's a bug, how do we audit?
- If the system crashes mid-transaction, is money lost?

**The Solution: Double-Entry**

Every transaction creates **TWO entries** that balance to zero:

```
Ahmed sends 500 YER to Sara:
  Entry 1: Ahmed's wallet   -500 YER  (debit)
  Entry 2: Sara's wallet    +500 YER  (credit)
  ───────────────────────────────────
  Total:                       0 YER  ✅
```

**Golden Rule:** For every debit, there MUST be an equal credit. Total = 0.

---

### Why This Matters for Fintech

1. **Auditability**: Every YER can be traced from source to destination
2. **Reconciliation**: `SUM(all credits) - SUM(all debits) = 0` (if not, there's a bug!)
3. **Fraud Detection**: Any imbalance triggers immediate alerts
4. **Legal Compliance**: Required by financial regulators worldwide
5. **Reversals**: To undo a transaction, create opposite entries (not delete!)

> [!IMPORTANT]
> **Interview Gold**: If asked "How do you ensure money isn't lost?" → Double-entry ledger with zero-sum validation.

---

### Proposed `LedgerEntry` Entity

**Fields:**

| Field           | Type       | Purpose                                       |
| --------------- | ---------- | --------------------------------------------- |
| `id`            | UUID       | Primary key                                   |
| `transactionId` | UUID       | Groups related entries (e.g., transfer)       |
| `walletId`      | UUID       | Which wallet was affected                     |
| `entryType`     | Enum       | DEBIT or CREDIT                               |
| `amount`        | BigDecimal | Always positive (sign from `entryType`)       |
| `balanceAfter`  | BigDecimal | Wallet balance AFTER this entry (audit trail) |
| `currency`      | Currency   | YER, SAR, USD                                 |
| `referenceType` | Enum       | TRANSFER, DEPOSIT, FEE, REFUND, etc.          |
| `referenceId`   | UUID       | ID of the transfer/deposit/etc.               |
| `description`   | String     | Human-readable ("Transfer to Sara")           |
| `createdAt`     | Instant    | When it happened (IMMUTABLE!)                 |

---

### Example Transaction Flows

**Example 1: Ahmed sends 500 YER to Sara**

```
Transaction ID: TXN-001

Ledger Entries:
┌────────┬────────────┬───────────┬────────┬────────┬──────────────┐
│ Entry  │ Wallet     │ Type      │ Amount │ After  │ Description  │
├────────┼────────────┼───────────┼────────┼────────┼──────────────┤
│ 1      │ Ahmed-YER  │ DEBIT     │ 500    │ 500    │ Transfer out │
│ 2      │ Sara-YER   │ CREDIT    │ 500    │ 1500   │ Transfer in  │
└────────┴────────────┴───────────┴────────┴────────┴──────────────┘

Zero-Sum Check: -500 + 500 = 0 ✅
```

**Example 2: Ahmed sends 500 YER to Sara with 5 YER fee**

```
Transaction ID: TXN-002

Ledger Entries:
┌────────┬────────────┬───────────┬────────┬────────┬──────────────┐
│ Entry  │ Wallet     │ Type      │ Amount │ After  │ Description  │
├────────┼────────────┼───────────┼────────┼────────┼──────────────┤
│ 1      │ Ahmed-YER  │ DEBIT     │ 505    │ 0      │ Transfer+fee │
│ 2      │ Sara-YER   │ CREDIT    │ 500    │ 2000   │ Transfer in  │
│ 3      │ FEES_YER   │ CREDIT    │ 5      │ 1050   │ Fee collected│
└────────┴────────────┴───────────┴────────┴────────┴──────────────┘

Zero-Sum Check: -505 + 500 + 5 = 0 ✅
```

**Example 3: User deposits 1000 YER (via agent)**

```
Transaction ID: TXN-003

Ledger Entries:
┌────────┬────────────────┬───────────┬────────┬────────┬──────────────┐
│ Entry  │ Wallet         │ Type      │ Amount │ After  │ Description  │
├────────┼────────────────┼───────────┼────────┼────────┼──────────────┤
│ 1      │ LIQUIDITY_YER  │ DEBIT     │ 1000   │ 999000 │ Deposit out  │
│ 2      │ Ahmed-YER      │ CREDIT    │ 1000   │ 1500   │ Deposit in   │
└────────┴────────────────┴───────────┴────────┴────────┴──────────────┘

Zero-Sum Check: -1000 + 1000 = 0 ✅
```

---

### Design Decisions (2.2 Ledger)

### 2.2 MVP Decision Lock (2026-02-20)

- Keep ledger entries **immutable** (append-only history).
- Keep `ReferenceType` for MVP as: `TRANSFER`, `DEPOSIT`, `WITHDRAWAL`, `FEE`.
- Keep **zero-sum validation mandatory** for each recorded transaction set.
- Keep **cached wallet balance + ledger reconciliation** model for MVP.
- Keep transaction limits enforced in ledger recording flow for MVP.

### 2.2.2 Current Code Contract (Verified)

- `RecordLedgerEntryUseCase` records double/triple entry transactions and runs zero-sum validation.
- `LedgerEntry` is immutable in domain model (final fields, no setters).
- `V6__create_ledger_entries_table.sql` enforces positive `amount` plus enum checks for `entry_type` and `reference_type`.
- `GetTransactionHistoryUseCase` reads latest entries by wallet with a limit.
- `ReconcileBalanceUseCase` recomputes wallet balance from ledger entries and compares against cached balance.
- Wallet loading for debit/credit now uses explicit `PESSIMISTIC_WRITE` lock via `findByIdForUpdate`.
- Wallet locking now uses stable UUID sort order before balance updates to reduce deadlock risk.
- Limits enforced in `RecordLedgerEntryUseCase`:
  - Minimum transfer amount: `1`
  - Maximum transaction amount: `100000`
  - Daily debit cap (KYC `VERIFIED`): `500000`
  - Daily debit cap (non-verified): `10000`
- Daily limit checks are skipped only for system wallets (`userId = null`).

### 2.2 Decision Summary (Resolved for MVP)

1. **Ledger Integrity:** Immutable append-only entries + zero-sum validation remain mandatory.
2. **Reference Model:** Keep current four reference types in MVP.
3. **Balance Strategy:** Keep cached balance for reads, ledger sum for reconciliation.
4. **Concurrency + Limits:** Explicit row locking and MVP transaction limits are now enforced in ledger recording.

> [!NOTE]
> The historical decision list below is kept for traceability. If there is any conflict, follow the **2.2 MVP Decision Lock (2026-02-20)**.

> [!IMPORTANT]
> **Decisions finalized on 2026-02-16 based on user feedback**

1. ✅ **Ledger Immutability:** **YES** — Append-only, no UPDATE/DELETE
2. ✅ **Balance Snapshot:** **YES** — Store `balanceAfter` in each entry for audit trail
3. ✅ **Reference Types:** Start with **4 types**:
   - `TRANSFER` — User-to-user money transfer
   - `DEPOSIT` — Agent/bank → user
   - `WITHDRAWAL` — User → agent/bank (cash out)
   - `FEE` — System transaction fees

   _Note: REFUND and ADJUSTMENT will be added in future phases_

4. ✅ **Concurrency Control:** **Pessimistic Locking** (`SELECT ... FOR UPDATE`)
   - **Why?** Safety over speed for financial transactions
   - **Trade-off:** Slightly slower but prevents race conditions
   - **When to change?** Only if we hit 100,000+ concurrent transactions/second

5. ✅ **Transaction Limits (MVP):**
   - Max per transaction: **100,000 YER**
   - Daily limit (verified users): **500,000 YER**
   - Daily limit (unverified users): **10,000 YER**
   - Minimum transfer: **1 YER**

---

### Explanation: Reference Types

**What are they?**  
Reference types categorize WHY a ledger entry was created.

**Example Use Cases:**

- **Reporting**: "Show all DEPOSITS this month"
- **User History**: Filter by TRANSFER only
- **Revenue Analytics**: Calculate total FEE income
- **Compliance**: "Show all WITHDRAWALS over 10,000 YER"

---

### Explanation: Pessimistic vs Optimistic Locking

**The Race Condition Problem:**

Ahmed has 1000 YER. Two transfers happen simultaneously:

- Transfer A: Send 800 YER
- Transfer B: Send 500 YER

**Without locking:**

```
Thread A reads 1000 → approve (800 < 1000)
Thread B reads 1000 → approve (500 < 1000)
Thread A debits 800 → balance = 200
Thread B debits 500 → balance = -300 ❌ OVERDRAFT!
```

**Pessimistic Locking Solution:**

```sql
SELECT balance FROM wallets WHERE id = ? FOR UPDATE;
-- Row is LOCKED until transaction commits
```

```
Thread A locks, reads 1000, debits 800 → balance = 200, unlocks ✅
Thread B waits... locks, reads 200, rejects (500 > 200) ✅
```

**Why we chose it:**

- ✅ Impossible to overdraft
- ✅ Simpler code (no retry logic)
- ✅ Better UX (clear success/failure)
- ❌ Slightly slower (acceptable for MVP)

---

## 2.2 Double-Entry Ledger — Implementation

> [!NOTE]
> **Status**: 🚧 In Progress

### Implementation Note (2026-02-21)

- Added explicit DB locking path for wallets:
  - `WalletRepository.findByIdForUpdate(...)`
  - `WalletJpaRepository.findByIdForUpdate(...)` with `PESSIMISTIC_WRITE`
- Updated ledger execution flow to lock wallets in stable UUID order before debit/credit.
- Added ledger-based daily debit aggregation:
  - `LedgerRepository.sumDebitsByWalletIdBetween(...)`
  - `LedgerEntryJpaRepository.sumAmountByWalletIdAndEntryTypeBetween(...)`
- Enforced MVP limits inside `RecordLedgerEntryUseCase`:
  - Minimum amount = `1`
  - Maximum per transaction = `100000`
  - Daily debit cap = `500000` for `VERIFIED`, otherwise `10000`
- System wallets (`userId = null`) are excluded from user daily-limit checks.

### Live Swagger Verification (2026-02-21)

Validated sequence end-to-end against running app (`http://localhost:8080`):

1. **Register user**  
   `POST /api/v1/auth/register`  
   Result: account created.

2. **Login from new device (before OTP)**  
   `POST /api/v1/auth/login` with new `deviceId`  
   Result: `400` with `OTP_VERIFICATION_REQUIRED`.

3. **Verify OTP for new device**  
   `POST /api/v1/devices/verify-otp`  
   Result: device trusted successfully.

4. **Login again from same device**  
   `POST /api/v1/auth/login`  
   Result: success with JWT tokens.

5. **Check wallets before KYC approval**  
   `GET /api/v1/wallets`  
   Result: `0` wallets (as expected for deferred wallet activation).

6. **Upload KYC document**  
   `POST /api/v1/kyc/upload` (`multipart/form-data`, `documentType=NATIONAL_ID`, file attached)  
   Result: document stored in `PENDING`.

7. **Approve account KYC**  
   `POST /api/v1/kyc/admin/accounts/{userId}/approve`  
   Result: user `kycStatus = VERIFIED`.

8. **Check wallets after KYC approval**  
   `GET /api/v1/wallets`  
   Result: `3` wallets created (`YER`, `SAR`, `USD`).

> [!NOTE]
> Ledger hardening changes (explicit row locking + amount/daily limits in `RecordLedgerEntryUseCase`) are active in application code, but they are not directly triggerable from current public Swagger endpoints until transfer/deposit APIs are wired to ledger recording in the next phase.

### What We Built

We implemented the **double-entry ledger system** — the financial backbone that ensures every transaction is perfectly balanced and auditable.

---

### Architecture Layers

```
┌──────────────────────────────────────────────────────────┐
│  DOMAIN LAYER (wallet/domain/)                           │
│  ├── LedgerEntry.java      → Immutable ledger entry      │
│  ├── EntryType.java        → Enum (DEBIT, CREDIT)        │
│  ├──Reference.java    → Enum (TRANSFER, DEPOSIT, FEE)│
│  └── LedgerRepository.java → Port interface              │
├──────────────────────────────────────────────────────────┤
│  APPLICATION LAYER (wallet/application/)                 │
│  └── RecordLedgerEntryUseCase.java                       │
│      → Double-entry recording with locking               │
├──────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE LAYER (wallet/infrastructure/)           │
│  ├── LedgerEntryJpaEntity.java                           │
│  ├── LedgerEntryJpaRepository.java                       │
│  ├── LedgerEntryMapper.java                              │
│  └── LedgerRepositoryAdapter.java                        │
├──────────────────────────────────────────────────────────┤
│  DATABASE                                                │
│  └── V6__create_ledger_entries_table.sql                 │
└──────────────────────────────────────────────────────────┘
```

---

### Key Components

#### 1. Domain Entity: `LedgerEntry.java`

**IMMUTABLE** — Once created, a ledger entry can NEVER be updated or deleted.

**Fields:**

- `id`: UUID (primary key)
- `transactionId`: UUID (groups related entries)
- `walletId`: UUID (which wallet was affected)
- `entryType`: DEBIT or CREDIT
- `amount`: BigDecimal (always positive)
- `balanceAfter`: BigDecimal (snapshot of wallet balance after this entry)
- `currency`: Currency enum
- `referenceType`: TRANSFER, DEPOSIT, WITHDRAWAL, FEE
- `referenceId`: UUID (ID of the source transaction)
- `description`: String (human-readable)
- `createdAt`: Instant (timestamp)

**Why no setters?** Immutability ensures the ledger cannot be tampered with. To correct an error, you create new offsetting entries, not edit old ones.

---

#### 2. Value Objects: Enums

**`EntryType.java`:**

```java
public enum EntryType {
    DEBIT,   // Money leaving (-)
    CREDIT   // Money entering (+)
}
```

**`ReferenceType.java`:**

```java
public enum ReferenceType {
    TRANSFER,    // User → user
    DEPOSIT,     // Agent/bank → user
    WITHDRAWAL,  // User → agent/bank
    FEE          // System fee
}
```

---

#### 3. Use Case: `RecordLedgerEntryUseCase.java`

**Core Methods:**

1. **`recordDoubleEntry(...)`** — Simple 2-entry transaction
   - Locks both wallets (`FOR UPDATE`)
   - Debits source, credits destination
   - Creates 2 ledger entries
   - Validates zero-sum

2. **`recordTransferWithFee(...)`** — 3-entry transaction
   - Locks 3 wallets (sender, receiver, fee wallet)
   - Debits sender (amount + fee)
   - Credits receiver (amount)
   - Credits fee wallet (fee)
   - Validates zero-sum

**Pessimistic Locking in Action:**

```java
@Transactional
public UUID recordDoubleEntry(...) {
    // 1. Load wallets (JPA automatically adds FOR UPDATE due to @Transactional)
    Wallet fromWallet = walletRepository.findById(fromWalletId)...;
    Wallet toWallet = walletRepository.findById(toWalletId)...;

    // 2. Update balances (domain validation)
    fromWallet.debit(amount);  // Validates sufficient funds
    toWallet.credit(amount);

    // 3. Save (lock released on commit)
    walletRepository.save(fromWallet);
    walletRepository.save(toWallet);

    // 4. Create ledger entries
    ...
}
```

**Zero-Sum Validation:**

```java
private void validateZeroSum(List<LedgerEntry> entries) {
    BigDecimal sum = ZERO;
    for (LedgerEntry entry : entries) {
        sum = entryType == DEBIT ? sum.subtract(amount) : sum.add(amount);
    }

    if (sum != ZERO) {
        throw new IllegalStateException("CRITICAL BUG: Entries don't balance!");
    }
}
```

---

#### 4. Database Migration: `V6__create_ledger_entries_table.sql`

```sql
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    entry_type VARCHAR(10) CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount DECIMAL(19, 4) CHECK (amount > 0),
    balance_after DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference_type VARCHAR(20) CHECK (...),
    reference_id UUID NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

-- Optimized indexes
CREATE INDEX idx_ledger_wallet_created
    ON ledger_entries(wallet_id, created_at DESC);

CREATE INDEX idx_ledger_transaction
    ON ledger_entries(transaction_id);
```

**Key Constraints:**

- `amount > 0`: Amounts are always positive, sign from `entry_type`
- `CHECK` constraints: Enforce valid enum values at DB level
- Indexes: Fast wallet history queries

---

### How It Works: Example Flow

**Scenario:** Ahmed sends 500 YER to Sara

**Step 1: Load Wallets (Pessimistic Lock)**

```
SELECT * FROM wallets WHERE id = 'ahmed-yer' FOR UPDATE;
SELECT * FROM wallets WHERE id = 'sara-yer' FOR UPDATE;
-- Rows locked until transaction commits
```

**Step 2: Update Balances**

```
Ahmed: 1000 → 500
Sara:   500 → 1000
```

**Step 3: Create Ledger Entries**

```
Entry 1: {
    transactionId: TXN-001,
    walletId: ahmed-yer,
    entryType: DEBIT,
    amount: 500,
    balanceAfter: 500,
    ...
}

Entry 2: {
    transactionId: TXN-001,
    walletId: sara-yer,
    entryType: CREDIT,
    amount: 500,
    balanceAfter: 1000,
    ...
}
```

**Step 4: Zero-Sum Check**

```
Sum = -500 (DEBIT) + 500 (CREDIT) = 0 ✅
```

**Step 5: Commit Transaction**

```
Wallet updates saved ✅
Ledger entries saved ✅
Locks released ✅
```

---

### Design Rationale

**Q: Why store `balanceAfter` in each entry?**  
**A:** Audit trail. You can see the exact wallet balance at any point in history without recalculating. Essential for investigations and reconciliation.

**Q: Why pessimistic locking instead of optimistic?**  
**A:** For financial transactions, **correctness > speed**. Pessimistic locking guarantees no race conditions. The performance cost is negligible for <10,000 concurrent users.

**Q: What if the zero-sum check fails?**  
**A:** The entire transaction rolls back. This should NEVER happen if the code is correct, so we throw a `Illegal StateException` to alert us immediately.

**Q: Can ledger entries be deleted?**  
**A:** NO. They're immutable. To reverse a transaction, create offsetting entries. This maintains a perfect audit trail.

---

### Testing the Implementation

**Unit Test Scenario:**

```java
// Given: Ahmed has 1000 YER, Sara has 500 YER
UUID txnId = recordLedgerEntryUseCase.recordDoubleEntry(
    ahmedWalletId, saraWalletId,
    BigDecimal.valueOf(300), ReferenceType.TRANSFER,
    UUID.randomUUID(), "Test transfer"
);

// Then: Balances updated
assertEquals(700, ahmedWallet.getBalance());
assertEquals(800, saraWallet.getBalance());

// And: 2 ledger entries created
List<LedgerEntry> entries = ledgerRepository.findByTransactionId(txnId);
assertEquals(2, entries.size());

// And: Zero-sum validated
BigDecimal sum = entries.stream()
    .map(e -> e.getEntryType() == DEBIT ? e.getAmount().negate() : e.getAmount())
    .reduce(ZERO, BigDecimal::add);
assertEquals(ZERO, sum);
```

---

---

## 2.3 System Wallets — Discussion

> [!NOTE]
> System wallets are **special wallets owned by the platform** (not users) that manage liquidity and collect fees.

### What Are System Wallets?

Think of system wallets as the platform's **internal bank accounts**:

**User Wallets:**

- Owned by individual users (Ahmed, Sara, etc.)
- Hold user money

**System Wallets:**

- Owned by the platform
- Manage operational liquidity and revenue

---

### Why Do We Need System Wallets?

**Problem:** Where does money come from when a user deposits cash?

```
Ahmed goes to an agent and gives 1000 YER cash.
Agent clicks "Deposit 1000 YER to Ahmed's wallet."

Ahmed's wallet: 0 → 1000 YER ✅

But in double-entry, EVERY credit needs a debit!
Where did the 1000 YER get debited from? 🤔
```

**Answer:** The **LIQUIDITY wallet**!

```
Ledger Entries:
  DEBIT:  LIQUIDITY_YER  1000 YER  (system wallet)
  CREDIT: Ahmed's wallet 1000 YER  (user wallet)
  ────────────────────────────────
  Sum: 0 ✅
```

---

### Two Types of System Wallets

#### 1. Liquidity Wallets (Float Management)

**Purpose:** Holds the platform's available cash reserves.

**Real-World Analogy:**  
When you deposit cash at an ATM, the bank's vault (liquidity) decreases, your account (user wallet) increases.

**Operations:**

- **Deposit:** DEBIT liquidity → CREDIT user
- **Withdrawal:** DEBIT user → CREDIT liquidity

**One per currency:**

- `LIQUIDITY_YER`
- `LIQUIDITY_SAR`
- `LIQUIDITY_USD`

---

#### 2. Fee Wallets (Revenue Collection)

**Purpose:** Collects transaction fees (platform revenue).

**Example:**  
Ahmed sends 500 YER to Sara with a 5 YER fee.

```
Ledger Entries:
  DEBIT:  Ahmed's wallet  505 YER  (user pays 500 + 5 fee)
  CREDIT: Sara's wallet   500 YER  (user receives 500)
  CREDIT: FEES_YER          5 YER  (platform earns 5)
  ─────────────────────────────────
  Sum: 0 ✅
```

**One per currency:**

- `FEES_YER`
- `FEES_SAR`
- `FEES_USD`

---

### Initial Balances

**Production:**

- All system wallets start at **0 balance**
- Money only exists when real cash is deposited by agents

**Development/Testing:**

- Seed with fake money for testing (e.g., 10,000,000 YER each)
- Allows simulating deposits/withdrawals without real cash

---

### System Wallet IDs

We'll use special **reserved UUIDs** for system wallets (not real user IDs):

```java
// Liquidity Wallets
LIQUIDITY_YER: 00000000-0000-0000-0000-000000000001
LIQUIDITY_SAR: 00000000-0000-0000-0000-000000000002
LIQUIDITY_USD: 00000000-0000-0000-0000-000000000003

// Fee Wallets
FEES_YER: 00000000-0000-0000-0000-000000000011
FEES_SAR: 00000000-0000-0000-0000-000000000012
FEES_USD: 00000000-0000-0000-0000-000000000013
```

**Why reserved UUIDs?**

- Predictable (easy to reference in code)
- No collision with real user UUIDs (user_id column will be NULL)
- Easy to identify in database queries

---

### Implementation Plan

**What We'll Build:**

1. **Migration: `V7__create_system_wallets.sql`**
   - Insert 6 system wallets (3 liquidity + 3 fee)
   - Production: 0 balance
   - [Optional] Dev seed: 10M balance each

2. **Constants Class:** `SystemWallets.java`
   - Define reserved UUIDs for easy reference

3. **Use Case:** `GetSystemWalletUseCase.java`
   - Helper to fetch system wallets by currency and type

---

## 2.3 System Wallets — Implementation

> [!NOTE]
> **Status**: 🚧 In Progress

### What We Built

We created 6 special platform-owned wallets for liquidity management and fee collection, with predictable UUIDs for easy reference.

---

### Components Created

#### 1. Constants Class: `SystemWallets.java`

Centralized definition of all system wallet IDs:

```java
public final class SystemWallets {
    // Liquidity Wallets
    public static final UUID LIQUIDITY_YER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID LIQUIDITY_SAR = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID LIQUIDITY_USD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // Fee Wallets
    public static final UUID FEES_YER = UUID.fromString("00000000-0000-0000-0000-000000000011");
    public static final UUID FEES_SAR = UUID.fromString("00000000-0000-0000-0000-000000000012");
    public static final UUID FEES_USD = UUID.fromString("00000000-0000-0000-0000-000000000013");

    public static UUID getLiquidityWallet(Currency currency) { ... }
    public static UUID getFeeWallet(Currency currency) { ... }
    public static boolean isSystemWallet(UUID walletId) { ... }
}
```

**Usage Examples:**

```java
// Get fee wallet for YER
UUID feeWallet = SystemWallets.getFeeWallet(Currency.YER);

// Check if a wallet is a system wallet
if (SystemWallets.isSystemWallet(walletId)) {
    // Handle system wallet logic
}
```

---

#### 2. Migration: `V6_5__allow_system_wallets.sql`

Modified the wallets table to support system wallets:

```sql
-- Allow NULL user_id for system wallets
ALTER TABLE wallets ALTER COLUMN user_id DROP NOT NULL;

-- Update UNIQUE constraint (user wallets only)
ALTER TABLE wallets ADD CONSTRAINT wallets_user_currency_unique
    UNIQUE (user_id, currency);
```

**Why?**

- System wallets have `user_id = NULL` (not owned by any user)
- User wallets still enforce 1 wallet per currency per user

---

#### 3. Migration: `V7__create_system_wallets.sql`

Inserts the 6 system wallets with zero initial balance:

```sql
-- Liquidity Wallets
INSERT INTO wallets (id, user_id, currency, balance, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', NULL, 'YER', 0.0000, 'ACTIVE', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000002', NULL, 'SAR', 0.0000, 'ACTIVE', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000003', NULL, 'USD', 0.0000, 'ACTIVE', NOW(), NOW());

-- Fee Wallets
INSERT INTO wallets (id, user_id, currency, balance, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000011', NULL, 'YER', 0.0000, 'ACTIVE', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000012', NULL, 'SAR', 0.0000, 'ACTIVE', NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000013', NULL, 'USD', 0.0000, 'ACTIVE', NOW(), NOW());
```

---

### How System Wallets Work

**Example 1: User Deposit**

Ahmed deposits 1000 YER cash at an agent:

```java
// Use case code
UUID txnId = recordLedgerEntryUseCase.recordDoubleEntry(
    SystemWallets.LIQUIDITY_YER,  // FROM: system liquidity
    ahmedWalletId,                // TO: user wallet
    BigDecimal.valueOf(1000),
    ReferenceType.DEPOSIT,
    depositId,
    "Cash deposit"
);
```

**Ledger Result:**

```
Entry 1: DEBIT  LIQUIDITY_YER  1000 YER (liquidity decreases)
Entry 2: CREDIT Ahmed's wallet 1000 YER (user gains money)
───────────────────────────────────────
Sum: 0 ✅
```

---

**Example 2: Transfer with Fee**

Ahmed sends 500 YER to Sara with 5 YER fee:

```java
UUID txnId = recordLedgerEntryUseCase.recordTransferWithFee(
    ahmedWalletId,              // FROM: sender
    saraWalletId,               // TO: recipient
    BigDecimal.valueOf(500),    // transfer amount
    SystemWallets.FEES_YER,     // fee wallet
    BigDecimal.valueOf(5),      // fee amount
    transferId,
    "Transfer to Sara"
);
```

**Ledger Result:**

```
Entry 1: DEBIT  Ahmed's wallet  505 YER (pays 500 + 5 fee)
Entry 2: CREDIT Sara's wallet   500 YER (receives 500)
Entry 3: CREDIT FEES_YER          5 YER (platform earns fee)
──────────────────────────────────────────
Sum: 0 ✅
```

---

### Database State After Migration

Running the migration creates:

```sql
SELECT id, currency, balance, user_id FROM wallets WHERE user_id IS NULL;
```

| id                                   | currency | balance | user_id |
| ------------------------------------ | -------- | ------- | ------- |
| 00000000-0000-0000-0000-000000000001 | YER      | 0.0000  | NULL    |
| 00000000-0000-0000-0000-000000000002 | SAR      | 0.0000  | NULL    |
| 00000000-0000-0000-0000-000000000003 | USD      | 0.0000  | NULL    |
| 00000000-0000-0000-0000-000000000011 | YER      | 0.0000  | NULL    |
| 00000000-0000-0000-0000-000000000012 | SAR      | 0.0000  | NULL    |
| 00000000-0000-0000-0000-000000000013 | USD      | 0.0000  | NULL    |

---

### Design Rationale

**Q: Why use reserved UUIDs instead of generating random ones?**  
**A:** Predictability. System wallets need to be referenced frequently in code. Using constants like `SystemWallets.FEES_YER` is much cleaner than looking up wallets by some arbitrary UUID.

**Q: Why NULL user_id instead of a special "SYSTEM" user?**  
**A:** Simpler and more explicit. NULL clearly indicates "not owned by a user". Creating a fake SYSTEM user complicates queries and authentication logic.

**Q: Why start with 0 balance in production?**  
**A:** Money should only exist when real cash enters the system. Starting with fake balances would be accounting fraud!

**Q: How do we seed test data for development?**  
**A:** Manually run UPDATE queries or create separate dev-only seed scripts that credit the liquidity wallets. This keeps production migrations clean.

---

---

## 2.4 Balance Management — Discussion

> [!NOTE]
> Balance management is about **reading wallet balances efficiently** while ensuring they stay accurate through reconciliation.

### The Balance Dilemma

**Two Ways to Get Balance:**

**Option A: Read from Wallet Table (Cached)**

```sql
SELECT balance FROM wallets WHERE id = ?;
```

- ✅ **Fast**: Single row lookup O(1)
- ❌ **Risk**: Could become stale if bugs exist

**Option B: Calculate from Ledger (Source of Truth)**

```sql
SELECT SUM(CASE
    WHEN entry_type = 'CREDIT' THEN amount
    ELSE -amount
END) FROM ledger_entries WHERE wallet_id = ?;
```

- ✅ **Accurate**: Always correct
- ❌ **Slow**: Scans all entries O(n)

---

### Our Hybrid Approach

**Decision:** Use **cached balance** for reads, but **validate** with ledger periodically.

```
┌─────────────────────────────────────────────────────────┐
│  READS (User requests balance)                          │
│  → Use wallets.balance (fast, cached)                   │
├─────────────────────────────────────────────────────────┤
│  WRITES (Transactions)                                  │
│  → Update wallets.balance immediately                   │
│  → Record ledger entries (source of truth)              │
├─────────────────────────────────────────────────────────┤
│  RECONCILIATION (Nightly background job)                │
│  → Compare wallets.balance vs SUM(ledger_entries)       │
│  → Alert if mismatch detected                           │
└─────────────────────────────────────────────────────────┘
```

---

### Why This Works

1. **Performance**: 99.9% of requests are fast (cached)
2. **Accuracy**: Ledger is immutable source of truth
3. **Safety**: Regular reconciliation catches bugs before they compound
4. **Auditability**: If balance is wrong, ledger shows exact history

---

### Reconciliation Logic

**What It Does:**

```java
For each wallet:
    cachedBalance = wallet.getBalance()
    ledgerBalance = SUM(ledger entries for this wallet)

    if (cachedBalance != ledgerBalance) {
        ALERT: "Wallet ${walletId} has balance mismatch!"
        Log the discrepancy for investigation
    }
```

**When to Run:**

- **Nightly** (batch job at 2 AM)
- **On-demand** (admin endpoint for debugging)
- **Real-time** (optional: after every transaction for critical wallets)

---

### Use Cases We'll Build

#### 1. `GetBalanceUseCase`

**Simple balance retrieval:**

```java
public record BalanceResponse(
    UUID walletId,
    Currency currency,
    BigDecimal balance,
    WalletStatus status
) {}

@Service
public class GetBalanceUseCase {
    public BalanceResponse execute(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)...;
        return new BalanceResponse(
            wallet.getId(),
            wallet.getCurrency(),
            wallet.getBalance(),  // ← Cached value
            wallet.getStatus()
        );
    }
}
```

---

#### 2. `GetAllWalletsUseCase`

**List all wallets for a user:**

```java
public record WalletSummary(
    UUID walletId,
    Currency currency,
    BigDecimal balance,
    WalletStatus status
) {}

@Service
public class GetAllWalletsUseCase {
    public List<WalletSummary> execute(UUID userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
            .map(w -> new WalletSummary(...))
            .toList();
    }
}
```

---

#### 3. `ReconcileBalanceUseCase`

**Verify cached balance matches ledger:**

```java
public record ReconciliationResult(
    UUID walletId,
    BigDecimal cachedBalance,
    BigDecimal ledgerBalance,
    boolean matches,
    BigDecimal discrepancy
) {}

@Service
public class ReconcileBalanceUseCase {
    public ReconciliationResult execute(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)...;

        // Calculate from ledger
        BigDecimal ledgerBalance = calculateBalanceFromLedger(walletId);

        // Compare
        boolean matches = wallet.getBalance().equals(ledgerBalance);
        BigDecimal discrepancy = wallet.getBalance().subtract(ledgerBalance);

        if (!matches) {
            log.error("BALANCE MISMATCH: Wallet {}", walletId);
            // Could trigger alert, email, Slack notification, etc.
        }

        return new ReconciliationResult(...);
    }

    private BigDecimal calculateBalanceFromLedger(UUID walletId) {
        List<LedgerEntry> entries = ledgerRepository.findByWalletId(walletId);
        BigDecimal sum = BigDecimal.ZERO;
        for (LedgerEntry entry : entries) {
            sum = (entry.getEntryType() == DEBIT)
                ? sum.subtract(entry.getAmount())
                : sum.add(entry.getAmount());
        }
        return sum;
    }
}
```

---

### Edge Cases

**Q: What if reconciliation finds a mismatch?**  
**A:** For MVP, just log it loudly. In production, trigger alerts and mark wallet as FROZEN until admin investigates.

**Q: Should we auto-fix mismatches?**  
**A:** NO! If there's a mismatch, it indicates a BUG. Fixing it silently hides the bug. Instead, alert and investigate.

**Q: What if a user has thousands of transactions?**  
**A:** For large wallets, calculating from ledger becomes slow. Consider:

- Adding indexes on `(wallet_id, created_at)`
- Using database aggregation functions
- Caching ledger sum separately

---

## 2.4 Balance Management — Implementation

> [!NOTE]
> **Status**: ✅ Completed

### What We Built

Three use cases for efficient balance management with reconciliation:

---

### Components Created

#### 1. DTOs (Data Transfer Objects)

**`BalanceResponse.java`:**

```java
public record BalanceResponse(
    UUID walletId,
    Currency currency,
    BigDecimal balance,
    WalletStatus status
) {}
```

**`WalletSummary.java`:**

```java
public record WalletSummary(
    UUID walletId,
    Currency currency,
    BigDecimal balance,
    WalletStatus status
) {}
```

**`ReconciliationResult.java`:**

```java
public record ReconciliationResult(
    UUID walletId,
    BigDecimal cachedBalance,
    BigDecimal ledgerBalance,
    boolean matches,
    BigDecimal discrepancy
) {}
```

---

#### 2. Use Case: `GetBalanceUseCase.java`

Retrieves wallet balance from cached value (fast):

```java
@Service
public class GetBalanceUseCase {
    public BalanceResponse execute(UUID walletId) {
        Wallet wallet = walletsRepository.findById(walletId)...;
        return new BalanceResponse(
            wallet.getId(),
            wallet.getCurrency(),
            wallet.getBalance(),  // O(1) cached read
            wallet.getStatus()
        );
    }
}
```

**Performance:** O(1) — Single database row lookup

---

#### 3. Use Case: `GetAllWalletsUseCase.java`

Lists all wallets for a user:

```java
@Service
public class GetAllWalletsUseCase {
    public List<WalletSummary> execute(UUID userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
            .map(w -> new WalletSummary(...))
            .toList();
    }
}
```

**Usage:** Display all 3 wallets (YER, SAR, USD) on user's dashboard

---

#### 4. Use Case: `ReconcileBalanceUseCase.java`

Verifies cached balance matches ledger-calculated balance:

```java
@Service
public class ReconcileBalanceUseCase {
    public ReconciliationResult execute(UUID walletId) {
        // 1. Get cached balance
        BigDecimal cachedBalance = wallet.getBalance();

        // 2. Calculate from ledger (source of truth)
        BigDecimal ledgerBalance = calculateBalanceFromLedger(walletId);

        // 3. Compare
        boolean matches = cachedBalance.equals(ledgerBalance);
        BigDecimal discrepancy = cachedBalance.subtract(ledgerBalance);

        // 4. Alert if mismatch
        if (!matches) {
            log.error("BALANCE MISMATCH: Wallet {}, Discrepancy: {}",
                wallet Id, discrepancy);
        }

        return new ReconciliationResult(...);
    }

    private BigDecimal calculateBalanceFromLedger(UUID walletId) {
        List<LedgerEntry> entries = ledgerRepository.findByWalletId(walletId);
        return entries.stream()
            .map(e -> e.getEntryType() == DEBIT
                ? e.getAmount().negate()
                : e.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

**When to Use:**

- Nightly batch job (recommended)
- Admin endpoint for debugging
- After every transaction (optional, for critical wallets)

---

### How It Works: Example Flow

**Scenario 1: User Checks Balance**

```java
// User requests balance for their YER wallet
BalanceResponse response = getBalanceUseCase.execute(ahmedYerWalletId);

// Response:
{
    "walletId": "...",
    "currency": "YER",
    "balance": 1000.0000,  // ← Cached value, FAST!
    "status": "ACTIVE"
}
```

**Performance:** ~1ms (single SELECT query)

---

**Scenario 2: User Views All Wallets**

```java
// User opens wallet dashboard
List<WalletSummary> wallets = getAllWalletsUseCase.execute(ahmedUserId);

// Response:
[
    { "currency": "YER", "balance": 1000.00 },
    { "currency": "SAR", "balance": 0.00 },
    { "currency": "USD", "balance": 50.00 }
]
```

---

**Scenario 3: Nightly Reconciliation Job**

```java
// Scheduled job runs at 2 AM
@Scheduled(cron = "0 0 2 * * *")
public void reconcileAllWallets() {
    List<Wallet> allWallets = walletRepository.findAll();

    for (Wallet wallet : allWallets) {
        ReconciliationResult result = reconcileBalanceUseCase.execute(wallet.getId());

        if (!result.matches()) {
            // Send alert to Slack/Email
            alertService.send("Balance mismatch detected: " + result);
        }
    }
}
```

**What It Detects:**

- Bugs in transaction logic
- Race conditions that slipped through
- Database corruption
- Manual SQL errors

---

### Design Rationale

**Q: Why not just always calculate from ledger?**  
**A:** Performance. For 1 million users checking balance 10 times/day = 10M queries. Calculating from ledger each time would scan potentially thousands of rows per query. Cached balance is O(1).

**Q: What if cached balance is wrong?**  
**A:** Reconciliation catches it. The ledger is immutable (source of truth), so we can always detect and fix discrepancies.

**Q: Why not use database triggers to keep balance updated?**  
**A:** We UPDATE the balance in application code (domain logic in `Wallet.debit()` / `Wallet.credit()`). This keeps business logic visible and testable.

**Q: Can reconciliation auto-fix mismatches?**  
**A:** NO! A mismatch indicates a BUG. Auto-fixing hides the root cause. Instead, alert admins to investigate. If needed, admins can manually run:

```sql
UPDATE wallets SET balance = (
    SELECT SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END)
    FROM ledger_entries WHERE wallet_id = wallets.id
);
```

---

---

## 2.5 Wallet API — Discussion

> [!NOTE]
> The API layer exposes wallet functionality to external clients (mobile apps, web frontend).

### What APIs Should We Expose?

Based on typical user needs:

1. **GET /wallets** — List all my wallets
2. **GET /wallets/{id}** — Get specific wallet balance
3. **GET /wallets/{id}/transactions** — View transaction history
4. **POST /wallets/transfer** — Transfer money between user wallets

### API Design Principles

**1. RESTful Conventions:**

- Use plural nouns (`/wallets`, not `/wallet`)
- Use HTTP methods correctly (GET for reads, POST for transfer)
- Use proper status codes (200, 404, 401, etc.)

**2. Security:**

- All endpoints require `@PreAuthorize("hasRole('USER')")`
- User can only access their own wallets
- JWT authentication via `Authorization: Bearer <token>`

**3. Pagination:**

- Transaction history uses pagination (10 items per page)
- Cursor-based or offset-based (we'll use offset for simplicity)

---

### Proposed Endpoints

#### 1. GET /api/v1/wallets

**Description:** List all wallets for authenticated user

**Request:**

```http
GET /api/v1/wallets
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**

```json
{
  "wallets": [
    {
      "walletId": "...",
      "currency": "YER",
      "balance": "1000.0000",
      "status": "ACTIVE"
    },
    {
      "walletId": "...",
      "currency": "SAR",
      "balance": "0.0000",
      "status": "ACTIVE"
    },
    {
      "walletId": "...",
      "currency": "USD",
      "balance": "50.0000",
      "status": "ACTIVE"
    }
  ]
}
```

---

#### 2. GET /api/v1/wallets/{walletId}

**Description:** Get balance for a specific wallet

**Request:**

```http
GET /api/v1/wallets/{walletId}
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**

```json
{
  "walletId": "...",
  "currency": "YER",
  "balance": "1000.0000",
  "status": "ACTIVE"
}
```

**Error (403 Forbidden):**

```json
{
  "timestamp": "2024-02-16T20:00:00Z",
  "status": 403,
  "error": "FORBIDDEN",
  "message": "You don't have access to this wallet"
}
```

---

#### 3. GET /api/v1/wallets/{walletId}/transactions

**Description:** Get transaction history for a wallet

**Query Parameters:**

- `page` (optional, default: 0)
- `size` (optional, default: 10, max: 50)

**Request:**

```http
GET /api/v1/wallets/{walletId}/transactions?page=0&size=10
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**

```json
{
  "transactions": [
    {
      "id": "...",
      "transactionId": "...",
      "type": "CREDIT",
      "amount": "500.0000",
      "balanceAfter": "1500.0000",
      "referenceType": "TRANSFER",
      "description": "Transfer from Ahmed",
      "createdAt": "2024-02-16T15:30:00Z"
    },
    {
      "id": "...",
      "transactionId": "...",
      "type": "DEBIT",
      "amount": "200.0000",
      "balanceAfter": "1000.0000",
      "referenceType": "WITHDRAWAL",
      "description": "Cash withdrawal",
      "createdAt": "2024-02-16T14:00:00Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3
}
```

---

#### 4. POST /api/v1/wallets/transfer

**Description:** Transfer money from the authenticated user's source wallet to another user wallet.

**Request:**

```http
POST /api/v1/wallets/transfer
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

```json
{
  "fromWalletId": "source-wallet-uuid",
  "toWalletId": "destination-wallet-uuid",
  "amount": 500,
  "description": "Test transfer from Swagger"
}
```

**Response (200 OK):**

```json
{
  "transactionId": "txn-uuid",
  "referenceId": "transfer-ref-uuid",
  "fromWalletId": "source-wallet-uuid",
  "toWalletId": "destination-wallet-uuid",
  "currency": "YER",
  "amount": 500,
  "description": "Test transfer from Swagger",
  "createdAt": "2026-02-21T12:30:00Z"
}
```

**Validation/Rules enforced by backend:**

- Source wallet must belong to authenticated user.
- Source and destination must be user wallets (not system wallets).
- Amount must be >= `1` and <= `100000`.
- Daily debit cap enforced by KYC status (`500000` verified, `10000` non-verified).

---

### Security Considerations

**Authorization Check:**

Every endpoint must verify:

1. User is authenticated (JWT valid)
2. User owns the wallet being accessed

```java
@GetMapping("/wallets/{walletId}")
public ResponseEntity<BalanceResponse> getBalance(
    @PathVariable UUID walletId,
    @AuthenticationPrincipal JwtUserDetails userDetails
) {
    // Verify ownership
    Wallet wallet = walletRepository.findById(walletId)...;
    if (!wallet.getUserId().equals(userDetails.getUserId())) {
        throw new ForbiddenException("Not your wallet");
    }

    return ResponseEntity.ok(getBalanceUseCase.execute(walletId));
}
```

---

## 2.5 Wallet API — Implementation

> [!NOTE]
> **Status**: 🚧 In Progress

### Implementation Note (2026-02-21)

- Added new transfer endpoint: `POST /api/v1/wallets/transfer`.
- Added transfer use case: `TransferMoneyUseCase`.
- Added transfer DTOs:
  - `TransferRequest`
  - `TransferResponse`
- Transfer endpoint routes through `RecordLedgerEntryUseCase` so row-locking and amount/daily limits are enforced in one place.

> Final execution result: ✅ Complete (transfer endpoint fully validated in Swagger).

### Swagger Validation Note (2026-02-22)

Transfer testing was completed end-to-end in local development using Swagger UI against `POST /api/v1/wallets/transfer`.

| Scenario                               | Payload highlight                   | Expected result                                                               | Actual    |
| -------------------------------------- | ----------------------------------- | ----------------------------------------------------------------------------- | --------- |
| Amount below minimum                   | `amount: 0.5`                       | `400 INVALID_ARGUMENT` + `Amount must be at least 1`                          | ✅ Passed |
| Amount above maximum                   | `amount: 100001`                    | `400 INVALID_ARGUMENT` + `Amount exceeds max per transaction limit of 100000` | ✅ Passed |
| Valid amount with empty sender balance | `amount: 100`                       | `400 BUSINESS_RULE_VIOLATION` + `Insufficient funds`                          | ✅ Passed |
| Happy path transfer                    | funded sender wallet, `amount: 500` | `200 OK` + `transactionId` and `referenceId`                                  | ✅ Passed |

Sample successful transfer:

- `transactionId`: `1b8998d7-52ab-47d2-ace6-8d15c9951279`
- `referenceId`: `905fbf02-06e6-4681-8c90-2371f8afcde8`
- `fromWalletId`: `0aa4c3c9-ae81-4b7f-b46f-b44b9ab9fbfa`
- `toWalletId`: `3208c64e-56af-4669-8009-425fe160453f`
- `currency`: `YER`
- `amount`: `500`

### What We Built

Four secure REST endpoints to expose wallet functionality to clients.

---

### Components Created

#### 1. Controller: `WalletController.java`

**Security:**

- All endpoints require `@PreAuthorize("hasRole('USER')")`
- Wallet ownership verified before access
- Uses `@Authentication Principal UUID userId` from JWT

**Endpoints:**

**GET /api/v1/wallets** — List all user wallets

```java
@GetMapping
@PreAuthorize("hasRole('USER')")
public ResponseEntity<List<WalletSummary>> getAllWallets(
    @AuthenticationPrincipal UUID userId
) {
    List<WalletSummary> wallets = getAllWalletsUseCase.execute(userId);
    return ResponseEntity.ok(wallets);
}
```

**GET /api/v1/wallets/{walletId}** — Get specific wallet balance

```java
@GetMapping("/{walletId}")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<BalanceResponse> getBalance(
    @PathVariable UUID walletId,
    @AuthenticationPrincipal UUID userId
) {
    verifyWalletOwnership(walletId, userId);  // Security check
    return ResponseEntity.ok(getBalanceUseCase.execute(walletId));
}
```

**GET /api/v1/wallets/{walletId}/transactions** — Transaction history

```java
@GetMapping("/{walletId}/transactions")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
    @PathVariable UUID walletId,
    @RequestParam(defaultValue = "10") int size,
    @AuthenticationPrincipal UUID userId
) {
    verifyWalletOwnership(walletId, userId);
    int limit = Math.min(size, 50);  // Max 50 items
    return ResponseEntity.ok(getTransactionHistoryUseCase.execute(walletId, limit));
}
```

**POST /api/v1/wallets/transfer** — Wallet-to-wallet transfer

```java
@PostMapping("/transfer")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<TransferResponse> transfer(
    @Valid @RequestBody TransferRequest request,
    @AuthenticationPrincipal UUID userId
) {
    return ResponseEntity.ok(transferMoneyUseCase.execute(userId, request));
}
```

---

#### 2. Use Case: `GetTransactionHistoryUseCase.java`

Retrieves paginated transaction history:

```java
@Service
public class GetTransactionHistoryUseCase {
    public List<TransactionResponse> execute(UUID walletId, int limit) {
        List<LedgerEntry> entries = ledgerRepository
            .findByWalletIdOrderByCreatedAtDesc(walletId, limit);

        return entries.stream()
            .map(entry -> new TransactionResponse(...))
            .toList();
    }
}
```

---

#### 3. DTO: `TransactionResponse.java`

```java
public record TransactionResponse(
    UUID id,
    UUID transactionId,
    EntryType type,             // DEBIT or CREDIT
    BigDecimal amount,
    BigDecimal balanceAfter,
    ReferenceType referenceType, // TRANSFER, DEPOSIT, etc.
    String description,
    Instant createdAt
) {}
```

---

### How It Works: Example API Calls

**1. List All Wallets**

```bash
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:8080/api/v1/wallets
```

**Response (200 OK):**

```json
[
  {
    "walletId": "uuid-1",
    "currency": "YER",
    "balance": "1000.0000",
    "status": "ACTIVE"
  },
  {
    "walletId": "uuid-2",
    "currency": "SAR",
    "balance": "0.0000",
    "status": "ACTIVE"
  },
  {
    "walletId": "uuid-3",
    "currency": "USD",
    "balance": "50.0000",
    "status": "ACTIVE"
  }
]
```

---

**2. Get Specific Wallet Balance**

```bash
curl -H "Authorization: Bearer <jwt-token>" \
     http://localhost:8080/api/v1/wallets/uuid-1
```

**Response (200 OK):**

```json
{
  "walletId": "uuid-1",
  "currency": "YER",
  "balance": "1000.0000",
  "status": "ACTIVE"
}
```

**Error (403 Forbidden) — Trying to access someone else's wallet:**

```json
{
  "timestamp": "2024-02-16T20:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: wallet belongs to another user"
}
```

---

**3. Get Transaction History**

```bash
curl -H "Authorization: Bearer <jwt-token>" \
     "http://localhost:8080/api/v1/wallets/uuid-1/transactions?size=5"
```

**Response (200 OK):**

```json
[
  {
    "id": "entry-uuid-1",
    "transactionId": "txn-uuid-1",
    "type": "CREDIT",
    "amount": "500.0000",
    "balanceAfter": "1500.0000",
    "referenceType": "TRANSFER",
    "description": "Transfer from Ahmed",
    "createdAt": "2024-02-16T15:30:00Z"
  },
  {
    "id": "entry-uuid-2",
    "transactionId": "txn-uuid-2",
    "type": "DEBIT",
    "amount": "200.0000",
    "balanceAfter": "1000.0000",
    "referenceType": "WITHDRAWAL",
    "description": "Cash withdrawal",
    "createdAt": "2024-02-16T14:00:00Z"
  }
]
```

---

### Security Implementation

**Ownership Verification:**

```java
private void verify WalletOwnership(UUID walletId, UUID userId) {
    Wallet wallet = walletRepository.findById(walletId)
        .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

    if (!wallet.getUserId().equals(userId)) {
        throw new SecurityException("Access denied: wallet belongs to another user");
    }
}
```

**Why it matters:**

- Prevents users from viewing other users' wallets
- Returns 403 Forbidden if ownership doesn't match
- Checked before every wallet-specific operation

---

### Design Rationale

**Q: Why not use pagination for transaction history?**  
**A:** For MVP, we use simple limit-based fetching. Full pagination (with total pages, etc.) can be added later if needed.

**Q: Why verify ownership in controller instead of use case?**  
**A:** The use case is wallet-agnostic (can be used by admin tools). Authorization is an API concern, so it belongs in the controller layer.

**Q: Why max 50 transactions per request?**  
**A:** Prevent abuse. Users requesting `size=999999` would overload the database. Capping at 50 is reasonable for mobile apps.

**Q: Can system wallets be accessed via this API?**  
**A:** No. System wallets have `user_id = NULL`, so the ownership check will fail. Only admins can access system wallets (via separate admin API, to be built later).

---

## ✅ Phase 2 Complete!

**All components implemented:**

✅ 2.1 Wallet Domain (Wallet entity, CreateWalletUseCase)  
✅ 2.2 Ledger System (LedgerEntry, RecordLedgerEntryUseCase)  
✅ 2.3 System Wallets (6 wallets with reserved UUIDs)  
✅ 2.4 Balance Management (GetBalanceUseCase, Reconciliation)  
✅ 2.5 Wallet API (4 REST endpoints)

**What we can do now:**

- Users get 3 wallets (YER, SAR, USD) after KYC account approval
- View balances via API
- View transaction history
- Transfer between user wallets via API
- Double-entry ledger ensures perfect auditing
- System wallets ready for deposits/withdrawals/fees
- Reconciliation verifies cached balances

**Next phase:** Phase 3 — P2P Transfers, Deposits, Exchange → See `PHASE_3.md` (Step 3.1 ✅ Complete!)

```

```
