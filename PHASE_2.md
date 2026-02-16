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

### Design Decisions (2.2 Ledger)

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

**Next: Phase 2.4 Balance Management** 📊
