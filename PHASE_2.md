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

## 2.2 Double-Entry Ledger — Discussion

> [!NOTE]
> The **double-entry ledger** is the most critical component in any financial system. It's how banks have tracked money for 700 years. Every transaction creates TWO entries: one debit and one credit.

### Why Double-Entry? (The Most Important Concept)

**The Problem with Single-Entry:**

Imagine tracking money with a simple table:

```
| User    | Action   | Amount  | Balance |
|---------|----------|---------|---------|
| Ahmed   | Deposit  | +1000   | 1000    |
| Ahmed   | Send     | -500    | 500     |
| Sara    | Receive  | +500    | 500     |
```

**What's wrong?**

- Where did Ahmed's 500 go? We don't know!
- If Sara says "I never got the money" — who's right?
- If the system crashes mid-transfer — who has the money?
- If there's a bug — how do we audit?

**The Solution: Double-Entry**

Every transaction creates TWO entries that MUST sum to zero:

```
| Entry | Debit Wallet | Credit Wallet | Amount | Reference    |
|-------|------------- |---------------|--------|--------------|
| 1     | Ahmed (YER)  |               | -500   | TXN-001      |
| 2     |              | Sara (YER)    | +500   | TXN-001      |
                                    SUM = 0 ✅
```

**The Golden Rule:** For every transaction, total debits = total credits. If they don't balance, something went wrong.

---

### Real-World Examples

**Example 1: Ahmed sends 500 YER to Sara**

```
Ahmed's Wallet:  -500 YER  (debit)
Sara's Wallet:   +500 YER  (credit)
                 ─────────
Sum:                0 ✅
```

**Example 2: Ahmed sends 500 YER to Sara, system charges 5 YER fee**

```
Ahmed's Wallet:  -505 YER  (debit: 500 transfer + 5 fee)
Sara's Wallet:   +500 YER  (credit)
Fee Wallet:      +5 YER    (credit)
                 ─────────
Sum:                0 ✅
```

**Example 3: Deposit 1000 YER into Ahmed's wallet**

```
Liquidity Wallet: -1000 YER  (debit: money leaves the system pool)
Ahmed's Wallet:   +1000 YER  (credit)
                  ─────────
Sum:                 0 ✅
```

**Key Insight:** Money never appears or disappears. It always **moves** from one wallet to another.

---

### Why Is This So Important for Fintech?

1. **Auditability**: Every YER can be traced. Regulators love this.
2. **Reconciliation**: At any time, SUM(all debits) = SUM(all credits). If not, there's a bug.
3. **Fraud Detection**: Any imbalance = immediate alert.
4. **Legal Compliance**: Required by financial regulations worldwide.
5. **Reversals**: To reverse a transaction, just create opposite entries.

**Interview Gold:** If you're asked "How do you ensure money isn't lost or created from nothing?" — the answer is **double-entry ledger with zero-sum validation**.

---

### Ledger Entry Fields

What information does each ledger entry need?

```
LedgerEntry:
  id              → Unique identifier (UUID)
  transactionId   → Groups related entries (e.g., both sides of a transfer)
  walletId        → Which wallet was affected
  entryType       → DEBIT or CREDIT
  amount          → How much (always positive, sign determined by entryType)
  balanceAfter    → Wallet balance after this entry (for audit trail)
  description     → Human-readable ("Transfer to Sara")
  referenceType   → What caused this (TRANSFER, DEPOSIT, FEE, REFUND)
  referenceId     → ID of the transfer/deposit/etc.
  createdAt       → When it happened
```

### Questions for You (2.2)

1. **Amount Precision:** How many decimal places for money?
   - 2 decimals (1000.50) — standard for most currencies
   - 4 decimals (1000.5000) — more precise, good for exchange rates
   - YER has no decimals in practice (smallest unit is 1 YER)

2. **Immutable Ledger:** Should ledger entries be immutable (never updated/deleted)?
   - Yes (industry standard — corrections are new entries, not edits)
   - No (simpler, but less auditable)

3. **Balance Storage:** Where should we store the current balance?
   - **Option A: Calculate from ledger** — `SUM(credits) - SUM(debits)` every time
     - ✅ Always accurate
     - ❌ Slow (scans all entries)
   - **Option B: Cache in wallet** — Store `balance` field in wallet table
     - ✅ Fast reads
     - ❌ Could get out of sync
   - **Option C: Both** — Cache + nightly reconciliation job
     - ✅ Fast reads + guaranteed accuracy
     - ❌ More complex

---

## 2.3 System Wallets — Discussion

> [!NOTE]
> **System wallets** are special wallets owned by the platform (not users). They represent the system's money pools and are essential for the double-entry model to work.

### Why Do We Need System Wallets?

**The Problem:** When Ahmed deposits 1000 YER, where does the money come from?

In the real world:

- Ahmed gives cash to an agent → Agent gives cash to the company → Company credits Ahmed's digital wallet

In our system:

- We need a "source" wallet for the money
- This is the **Liquidity Wallet** — it represents all the real money the company holds

**Think of it like a bank vault:**

- The vault has all the cash
- When a customer deposits, money moves from "vault" to "customer account"
- When a customer withdraws, money moves from "customer account" back to "vault"

---

### Types of System Wallets

| Wallet Name       | Purpose                    | Example                                         |
| ----------------- | -------------------------- | ----------------------------------------------- |
| **LIQUIDITY_YER** | Pool of YER deposits       | When user deposits 1000 YER, it comes from here |
| **LIQUIDITY_SAR** | Pool of SAR deposits       | Same for Saudi Riyals                           |
| **LIQUIDITY_USD** | Pool of USD deposits       | Same for US Dollars                             |
| **FEES_YER**      | Collected transaction fees | 5 YER fee → goes here                           |
| **FEES_SAR**      | Collected SAR fees         | Same for SAR fees                               |
| **FEES_USD**      | Collected USD fees         | Same for USD fees                               |

**Example: Full deposit flow**

```
1. Ahmed deposits 1000 YER at agent
2. System creates ledger entries:
   - LIQUIDITY_YER: -1000 (debit)
   - Ahmed's YER wallet: +1000 (credit)
   - Sum = 0 ✅
3. Ahmed's balance: 1000 YER
```

**Example: Full transfer with fee**

```
1. Ahmed sends 500 YER to Sara (fee: 5 YER)
2. System creates ledger entries:
   - Ahmed's YER wallet: -505 (debit)
   - Sara's YER wallet: +500 (credit)
   - FEES_YER: +5 (credit)
   - Sum = 0 ✅
```

### Questions for You (2.3)

1. **System Wallet per Currency:** Create one set of system wallets per supported currency?
   - Yes (LIQUIDITY_YER, LIQUIDITY_SAR, LIQUIDITY_USD separately)
   - No (single LIQUIDITY wallet for all currencies)

2. **Fee Wallet:** Should fees go to a separate wallet or the same liquidity wallet?
   - Separate FEES wallet (easier accounting, tax reporting)
   - Same liquidity wallet (simpler, but harder to track revenue)

3. **Initial System Wallet Balance:** What starting balance?
   - 0 (start empty, grows with real deposits)
   - Seed amount (for testing/demo purposes)

---

## 2.4 Balance Management — Discussion

> [!NOTE]
> Balance management is deceptively complex. Reading "how much money does Ahmed have?" seems simple, but doing it **accurately and fast** at scale is a real engineering challenge.

### The Challenge: Speed vs Accuracy

**Option A: Calculate every time**

```sql
SELECT SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END)
FROM ledger_entries
WHERE wallet_id = ?
```

- ✅ Always 100% accurate
- ❌ Scans thousands of rows (slow)
- ❌ Gets slower as more transactions happen
- ❌ Imagine 10,000 users checking balance simultaneously

**Option B: Cached balance in wallet table**

```sql
SELECT balance FROM wallets WHERE id = ?
```

- ✅ Single row lookup (O(1), microseconds)
- ✅ Scales infinitely
- ❌ Could get out of sync (if update fails mid-transaction)

**Option C: Both (Recommended)**

- Store cached balance in `wallets.balance` for fast reads
- Update balance atomically with ledger entries (same transaction)
- Run nightly **reconciliation job** to compare:
  - `wallets.balance` vs `SUM(ledger_entries)`
  - If they differ → ALERT! Something went wrong.

---

### Reconciliation: The Safety Net

**What is reconciliation?**

Every night at 2 AM, a scheduled job runs:

```
For each wallet:
  calculated_balance = SUM(credits) - SUM(debits) from ledger
  cached_balance = wallet.balance

  if (calculated_balance != cached_balance):
    ALERT! Discrepancy of (difference) found!
    Freeze wallet if difference > threshold
```

**Why this matters:**

- Catches bugs before users notice
- Required by financial auditors
- Shows regulators you take accuracy seriously

---

### Concurrent Access: The Race Condition Problem

**Scenario:** Ahmed has 1000 YER. Two transfers happen simultaneously:

- Transfer A: Send 800 YER to Sara
- Transfer B: Send 500 YER to Omar

```
Without protection:
  Thread A reads balance: 1000 → OK, 800 < 1000 → proceed
  Thread B reads balance: 1000 → OK, 500 < 1000 → proceed
  Thread A debits 800 → balance = 200
  Thread B debits 500 → balance = -300 ❌ OVERDRAFT!
```

Ahmed now has -300 YER. This is a **race condition**.

**Solution: Pessimistic Locking**

```sql
SELECT balance FROM wallets WHERE id = ? FOR UPDATE
-- This LOCKS the row until the transaction completes
-- Thread B waits until Thread A finishes
```

```
With locking:
  Thread A locks wallet, reads 1000 → debit 800 → balance = 200 → unlock
  Thread B locks wallet, reads 200 → 500 > 200 → REJECT! ✅
```

### Questions for You (2.4)

1. **Overdraft:** Should users be allowed to go negative?
   - No (standard for e-wallets)
   - Yes, with a limit (like a credit line)

2. **Minimum Balance:** Should there be a minimum balance?
   - 0 (can withdraw everything)
   - Small amount (e.g., 1 YER must remain)

3. **Balance Notifications:** Notify users on balance changes?
   - Yes (push notification for every transaction)
   - No (users check manually)

---

## 2.5 Wallet API — Discussion

> [!NOTE]
> The wallet API is how users interact with their wallets: create wallets, check balances, and view transaction history.

### Proposed Endpoints

| Endpoint                            | Method | Description                  | Auth Required |
| ----------------------------------- | ------ | ---------------------------- | ------------- |
| `/api/v1/wallets`                   | POST   | Create a new wallet          | ✅ + KYC      |
| `/api/v1/wallets`                   | GET    | List user's wallets          | ✅            |
| `/api/v1/wallets/{id}`              | GET    | Get wallet details + balance | ✅            |
| `/api/v1/wallets/{id}/transactions` | GET    | Transaction history          | ✅            |

### Transaction History

When a user opens their wallet, they expect to see a list like:

```
Today:
  ↓ Received 500 YER from Sara           +500 YER
  ↑ Sent 200 YER to Omar                 -200 YER

Yesterday:
  ↓ Deposit                              +1000 YER
  ↑ Transfer fee                         -5 YER
```

**Design Decisions:**

- **Pagination:** How many transactions per page? (10? 20? 50?)
- **Sorting:** Most recent first (default)
- **Filtering:** By date range? By type (sent/received)?

### Questions for You (2.5)

1. **Pagination Size:** How many transactions per page?
   - 10 (mobile-friendly)
   - 20 (balance of info and speed)
   - 50 (for power users)

2. **Transaction Details:** What info to show per transaction?
   - Basic: amount, date, type
   - Detailed: amount, date, type, counterparty name, reference ID, balance after

3. **Export:** Should users be able to export transaction history?
   - Yes (PDF/CSV download)
   - No (not for MVP)

---

## Summary: What We'll Build in Phase 2

```
┌─────────────────────────────────────────────────────────────┐
│                     WALLET MODULE                           │
│                                                             │
│  Domain Layer:                                              │
│    ├── Wallet entity (id, userId, currency, balance, status)│
│    ├── LedgerEntry entity (debit/credit, amount, reference) │
│    ├── WalletRepository (port)                              │
│    └── LedgerEntryRepository (port)                         │
│                                                             │
│  Application Layer:                                         │
│    ├── CreateWalletUseCase (with KYC gate)                  │
│    ├── GetBalanceUseCase                                    │
│    ├── GetTransactionHistoryUseCase                         │
│    └── RecordLedgerEntryUseCase                             │
│                                                             │
│  Infrastructure Layer:                                      │
│    ├── WalletJpaEntity + Repository + Mapper                │
│    ├── LedgerEntryJpaEntity + Repository + Mapper           │
│    └── Database migrations (V5, V6)                         │
│                                                             │
│  API Layer:                                                 │
│    └── WalletController                                     │
└─────────────────────────────────────────────────────────────┘
```

---

**Please answer the questions above before we start building!** 🏗️

I need your decisions on:

- 2.1: Wallet creation strategy, currencies, limits, KYC requirement
- 2.2: Amount precision, immutability, balance storage
- 2.3: System wallets per currency, fee wallets, initial balance
- 2.4: Overdraft, minimum balance, notifications
- 2.5: Pagination, transaction details, export
