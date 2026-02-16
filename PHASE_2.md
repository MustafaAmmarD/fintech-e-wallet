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
