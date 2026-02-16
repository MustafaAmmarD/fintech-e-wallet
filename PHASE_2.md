# Phase 2: Wallet & Ledger

> **Goal**: Build the financial backbone — wallets, balances, and a double-entry ledger system.
> **Estimated Duration**: Weeks 4–6

---

## Phase 2 Overview

| Step | Title               | Scope                                       | Status     |
| ---- | ------------------- | ------------------------------------------- | ---------- |
| 2.1  | Wallet Creation     | Wallet entity, multi-currency, KYC gate     | ⬜ Pending |
| 2.2  | Double-Entry Ledger | Ledger entries, debit/credit, zero-sum rule | ⬜ Pending |
| 2.3  | System Wallets      | Liquidity pools, fee collection             | ⬜ Pending |
| 2.4  | Balance Management  | Cached balance, reconciliation, caching     | ⬜ Pending |
| 2.5  | Wallet API          | REST endpoints, transaction history         | ⬜ Pending |

---

## 2.1 Wallet Creation — Discussion

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

### Questions for You (2.1)

1. **Wallet Creation Strategy:** Which option do you prefer?
   - A: Auto-create 3 wallets on registration
   - B: User creates wallets on demand
   - C: Auto-create YER, user adds others

2. **Supported Currencies:** Which currencies should we support?
   - YER (Yemeni Rial) — mandatory?
   - SAR (Saudi Riyal) — yes/no?
   - USD (US Dollar) — yes/no?
   - Others? (EUR, GBP, AED?)

3. **Wallet Limit:** How many wallets per currency per user?
   - 1 wallet per currency (simplest)
   - Multiple wallets per currency (like "Savings YER", "Daily YER")

4. **KYC Requirement:** Must users complete KYC before creating a wallet?
   - Yes (more secure, compliant)
   - No (faster onboarding, verify later)

---

## Design Decisions (Phase 2)

> [!IMPORTANT]
> **Decisions finalized on 2026-02-16 based on user feedback**

### 2.1 Wallet Creation Strategy

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

### Questions for You (Before Implementation)

Before I start coding the ledger system, I need your decisions on:

1. **Ledger Immutability**: Should ledger entries be TRULY immutable (no UPDATE/DELETE allowed)?
   - **YES** (recommended): Corrections are new entries, history never changes
   - **NO**: Allow admin to edit/delete entries (risky, but flexible)

2. **Balance Snapshot**: Should each `LedgerEntry` store `balanceAfter`?
   - **YES**: Easier auditing (can see balance at any point in time)
   - **NO**: Calculate balance by summing entries (slower but saves space)

3. **Reference Types**: What types of transactions should we support initially?
   - `TRANSFER` (user to user)
   - `DEPOSIT` (agent → user)
   - `WITHDRAWAL` (user → agent)
   - `FEE` (system charges)
   - `REFUND` (reverse a transfer)
   - Others?

4. **Concurrent Transactions**: How should we handle race conditions?
   - **Pessimistic Locking**: `SELECT ... FOR UPDATE` (safe, slower)
   - **Optimistic Locking**: Version field (fast, but retry on conflict)

5. **Transaction Limits**: Should we enforce limits for MVP?
   - Max transfer amount per transaction?
   - Daily transfer limit per user?
   - Or no limits for now?

---

**Please answer these questions so I can proceed with the ledger implementation!** 🏗️
