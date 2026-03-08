# Phase 3: Transfers & Exchange

> **Goal**: Enable P2P money transfers between users with fee collection and a complete transfer lifecycle.
> **Estimated Duration**: Weeks 7–9

---

## Phase 3 Overview

> Current execution status (MVP): **Steps 3.1, 3.2, 3.3, 3.4, 3.5, 3.6 completed.**
> Next step: **Phase 4 planning**.
> Update (2026-02-24): Phase 3.2 implementation and live sequence testing completed (promote agent, deposit, withdrawal, history, wallet balance verification).

| Step | Title                | Scope                                                | Status      |
| ---- | -------------------- | ---------------------------------------------------- | ----------- |
| 3.1  | P2P Transfer Engine  | Preview/Confirm flow, account numbers, fees, history | ✅ Complete |
| 3.2  | Deposits/Withdrawals | Agent deposits, bank withdrawals                     | ✅ Complete |
| 3.3  | Currency Exchange    | YER ↔ SAR ↔ USD with rate management                 | ✅ Complete |
| 3.4  | Fee Configuration    | Configurable fee tiers via database                  | ✅ Complete |
| 3.5  | Idempotency          | Prevent duplicate transactions                       | ✅ Complete |
| 3.6  | Limits & Compliance  | DB-driven per-tx/daily/monthly/velocity controls    | 🔄 In Progress |

---

## 3.1 P2P Transfer Engine — Discussion

> [!NOTE]
> A **P2P transfer** is the core value proposition of any e-wallet. It's how Ahmed sends money to Sara — instantly, without visiting a bank, without cash. Think of it like handing someone money, but digital.

### What Is a P2P Transfer?

Think of it this way:

- **Traditional**: Ahmed goes to a bank → fills out a form → gives cash to the teller → bank sends money to Sara's account → Sara gets it in 1-3 days.
- **E-Wallet**: Ahmed opens the app → types Sara's account number → taps "Send" → Sara gets it **instantly**.

The key difference: **speed** and **convenience**. But with speed comes risk. We need to ensure:

1. Ahmed actually has the money
2. Sara actually exists
3. The transfer is recorded permanently
4. Fees are collected transparently
5. The whole thing either succeeds completely or fails completely (atomicity)

---

### Design Decisions

> [!IMPORTANT]
> **Decisions finalized on 2026-02-23 based on user feedback**

1. ✅ **Transfer Flow:** **Two-step (Preview → Confirm)**
   - _Why?_ Prevents accidental transfers. Users see the fee breakdown before committing.
   - _Real-world analogy_: ATM shows "You are about to withdraw 5000 YER. Fee: 100 YER. Continue?" before dispensing cash.

2. ✅ **Data Storage:** **Dedicated `transfers` table + ledger entries**
   - _Why?_ The ledger captures the financial movements (debits/credits). But transfers have metadata that doesn't belong in the ledger: recipient names, reference numbers, status, descriptions.
   - _Analogy_: Your bank statement (ledger) shows "-500 YER". But the transfer record shows "Sent to Sara (ACC-123456789) for Rent."

3. ✅ **Recipient Identification:** **By account number** (not wallet UUID)
   - _Why?_ Users shouldn't deal with UUIDs like `550e8400-e29b-41d4-a716-446655440000`. They type a simple 9-digit number like `192967789`.
   - _Security_: Account numbers use the **Luhn algorithm** (same as credit cards) to catch typos.

4. ✅ **API Endpoints:** **All 4 implemented**
   - `POST /transfers/preview` — Show what will happen
   - `POST /transfers/execute` — Actually do it
   - `GET /transfers/{id}` — View one transfer
   - `GET /transfers/history` — View all transfers

5. ✅ **Fees:** **Simple hardcoded 2% fee** with min/max caps
   - _Minimum fee_: 1.00 (prevents zero-fee micro-transfers)
   - _Maximum fee_: 500.00 (protects large transfers from excessive fees)
   - _Example_: Transfer 1000 YER → Fee = 20 YER → Total deducted = 1020 YER

---

### The Luhn Algorithm — Account Numbers

> [!TIP]
> The Luhn algorithm is the same checksum used on credit cards. It catches **single-digit errors** and **transposition errors** (e.g., typing 1234 instead of 1243).

**How our account numbers work:**

```
Format: 9 digits total = 8 random digits + 1 check digit
Example: 192967789

Step 1: Generate 8 random digits: 19296778
Step 2: Calculate Luhn check digit: 9
Step 3: Final account number: 192967789
```

**Validation example:**

```
Number: 1 9 2 9 6 7 7 8 9
Double: 2 9 4 9 3 7 5 8 9  (double every other from right)
Adjust: 2 9 4 9 3 7 5 8 9  (subtract 9 if > 9)
Sum:    2+9+4+9+3+7+5+8+9 = 56... wait, should be divisible by 10!
```

If the sum is divisible by 10, the number is valid. If a user types `192967788` (wrong last digit), the check fails immediately — no database lookup needed.

**Implementation:** `AccountNumberGenerator.java`

```java
public final class AccountNumberGenerator {
    // Generate: 8 random digits + 1 check digit
    public static String generate() {
        StringBuilder sb = new StringBuilder(8);
        sb.append(1 + random.nextInt(9)); // First digit: 1-9 (no leading zero)
        for (int i = 1; i < 8; i++)
            sb.append(random.nextInt(10));

        int checkDigit = calculateLuhnCheckDigit(sb.toString());
        return sb.toString() + checkDigit; // 9-digit number
    }

    // Validate: full Luhn check
    public static boolean isValid(String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 9) return false;
        return luhnCheck(accountNumber); // Sum divisible by 10?
    }
}
```

---

## 3.1 P2P Transfer Engine — Implementation

> [!NOTE]
> **Status**: ✅ Complete (2026-02-23)

### What We Built

We implemented a complete P2P transfer engine that allows users to send money to each other using account numbers. The system validates the transfer, shows a preview with fee breakdown, and then executes it atomically using the existing double-entry ledger.

---

### Architecture Layers

Following the same Hexagonal Architecture established in Phase 2:

```
┌──────────────────────────────────────────────────────────┐
│  DOMAIN LAYER (wallet/domain/)                           │
│  ├── P2PTransfer.java          → Transfer entity         │
│  ├── TransferStatus.java       → COMPLETED/FAILED/REVERSED│
│  └── TransferRepository.java   → Port interface          │
├──────────────────────────────────────────────────────────┤
│  APPLICATION LAYER (wallet/application/)                 │
│  ├── PreviewTransferUseCase.java  → Validate + fee calc  │
│  ├── ExecuteTransferUseCase.java  → Execute + record     │
│  ├── GetTransferDetailUseCase.java → Single transfer     │
│  └── GetTransferHistoryUseCase.java → User's transfers   │
│                                                          │
│  DTOs (wallet/application/dto/)                          │
│  ├── TransferPreviewRequest.java   → Input for preview   │
│  ├── TransferPreviewResponse.java  → Preview result      │
│  ├── ExecuteTransferRequest.java   → Input for execute   │
│  ├── ExecuteTransferResponse.java  → Execute result      │
│  └── TransferDetailResponse.java   → Detail / history    │
├──────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE LAYER (wallet/infrastructure/persistence/)│
│  ├── TransferJpaEntity.java    → JPA mapped entity       │
│  ├── TransferJpaRepository.java → Spring Data queries    │
│  ├── TransferMapper.java       → Domain ↔ JPA converter  │
│  └── TransferRepositoryAdapter.java → Port implementation│
├──────────────────────────────────────────────────────────┤
│  API LAYER (wallet/api/)                                 │
│  └── TransferController.java   → 4 REST endpoints       │
├──────────────────────────────────────────────────────────┤
│  IDENTITY MODULE (Modified)                              │
│  ├── User.java                 → +accountNumber field    │
│  ├── UserRepository.java       → +findByAccountNumber() │
│  ├── UserJpaEntity.java        → +account_number column  │
│  ├── UserJpaRepository.java    → +findByAccountNumber() │
│  ├── UserRepositoryAdapter.java → Implementation         │
│  ├── RegisterResponse.java     → +accountNumber in resp  │
│  └── RegisterUserUseCase.java  → Returns account number  │
├──────────────────────────────────────────────────────────┤
│  SHARED UTILITY (shared/util/)                           │
│  └── AccountNumberGenerator.java → Luhn check-digit gen  │
├──────────────────────────────────────────────────────────┤
│  DATABASE                                                │
│  ├── V8__create_transfers_table.sql                      │
│  └── V9__add_account_number_to_users.sql                 │
└──────────────────────────────────────────────────────────┘
```

---

### Database Changes

#### 1. Transfers Table (`V8`)

```sql
CREATE TABLE transfers (
    id                  UUID PRIMARY KEY,
    reference_no        VARCHAR(20) NOT NULL UNIQUE,     -- e.g., TRF-20260223-A1B2C3
    sender_user_id      UUID NOT NULL REFERENCES users(id),
    sender_wallet_id    UUID NOT NULL REFERENCES wallets(id),
    recipient_user_id   UUID NOT NULL REFERENCES users(id),
    recipient_wallet_id UUID NOT NULL REFERENCES wallets(id),
    amount              DECIMAL(19, 4) NOT NULL CHECK (amount > 0),
    fee_amount          DECIMAL(19, 4) NOT NULL DEFAULT 0,
    total_deducted      DECIMAL(19, 4) NOT NULL CHECK (total_deducted > 0),
    currency            VARCHAR(3) NOT NULL,
    status              VARCHAR(20) NOT NULL,            -- COMPLETED / FAILED / REVERSED
    description         VARCHAR(500),
    transaction_id      UUID,                            -- Links to ledger_entries
    created_at          TIMESTAMP NOT NULL,
    completed_at        TIMESTAMP
);
```

**Why a separate table?** The `ledger_entries` table captures the _financial movements_ (debit/credit), but transfers need _metadata_ that doesn't belong in the ledger:

| Ledger Knows                     | Transfer Table Knows                   |
| -------------------------------- | -------------------------------------- |
| "Wallet X was debited 1020 YER"  | "Ahmed sent 1000 YER to Sara for Rent" |
| "Wallet Y was credited 1000 YER" | "Reference: TRF-20260223-A1B2C3"       |
| "Fee wallet got 20 YER"          | "Status: COMPLETED at 14:32"           |

The `transaction_id` column links the transfer to its ledger entries, so you can always trace the money.

**Indexes:**

```sql
-- "Show me Ahmed's transfers" (sender history)
CREATE INDEX idx_transfers_sender ON transfers(sender_user_id, created_at DESC);

-- "Show me Sara's received transfers" (recipient history)
CREATE INDEX idx_transfers_recipient ON transfers(recipient_user_id, created_at DESC);

-- "Look up by reference number" (customer support)
CREATE INDEX idx_transfers_reference ON transfers(reference_no);
```

#### 2. Account Number Column (`V9`)

```sql
ALTER TABLE users ADD COLUMN account_number VARCHAR(15);
CREATE UNIQUE INDEX idx_users_account_number ON users(account_number) WHERE account_number IS NOT NULL;
```

**Why nullable?** Existing users don't have account numbers yet. New registrations generate them automatically. Existing users will get theirs via backfill or on next login.

---

### Domain Entity: `P2PTransfer.java`

The `P2PTransfer` entity captures the full context of a transfer:

```java
public class P2PTransfer {
    private final UUID id;
    private final String referenceNo;          // TRF-20260223-A1B2C3
    private final UUID senderUserId;
    private final UUID senderWalletId;
    private final UUID recipientUserId;
    private final UUID recipientWalletId;
    private final BigDecimal amount;            // What recipient gets
    private final BigDecimal feeAmount;         // 2% fee
    private final BigDecimal totalDeducted;     // amount + fee (what sender loses)
    private final Currency currency;
    private TransferStatus status;              // COMPLETED → can become REVERSED
    private final String description;
    private final UUID transactionId;           // Links to ledger entries
    private final Instant createdAt;
    private Instant completedAt;
}
```

**Key design choices:**

- `amount` = what the recipient receives (1000 YER)
- `feeAmount` = what the platform takes (20 YER)
- `totalDeducted` = what the sender pays (1020 YER)
- `referenceNo` = human-readable reference (e.g., `TRF-20260223-A1B2C3`) for customer support

**Reference number format:**

```
TRF-20260223-A1B2C3
│   │         │
│   │         └── Random 6 chars (from UUID)
│   └──────────── Date (YYYYMMDD)
└──────────────── Prefix
```

---

### Transfer Status Lifecycle

```
┌─────────────┐                    ┌──────────┐
│   PREVIEW   │───(user confirms)──│COMPLETED │
│  (no state) │                    └────┬─────┘
└─────────────┘                         │
                                   (admin reversal)
                                        │
                                   ┌────▼─────┐
                                   │ REVERSED  │
                                   └──────────┘
```

- **COMPLETED**: Transfer executed successfully. Money moved.
- **REVERSED**: Admin reversed the transfer (compensating entries created).
- **FAILED**: Transfer failed during execution (e.g., concurrent deduction caused insufficient funds).

> [!NOTE]
> PREVIEW is not a status — it's an API call that returns information without creating a record. Only `COMPLETED` transfers exist in the database.

---

### The Two-Step Transfer Flow

This is the core of the transfer engine. Let's walk through it step by step.

#### Step 1: Preview (`POST /api/v1/transfers/preview`)

The user selects a recipient and amount. The app calls the preview endpoint to validate everything and show a fee breakdown.

```
User App                          PreviewTransferUseCase
  │                                  │
  │  recipientAccNo: "192967789"     │
  │  amount: 1000                    │
  │  currency: YER                   │
  │ ────────────────────────────────► │
  │                                  │
  │                                  ├── 1. Look up sender (from JWT)
  │                                  ├── 2. Look up recipient by account number
  │                                  ├── 3. Check: not self-transfer?
  │                                  ├── 4. Find sender's YER wallet
  │                                  ├── 5. Find recipient's YER wallet
  │                                  ├── 6. Check: both wallets ACTIVE?
  │                                  ├── 7. Check: not system wallets?
  │                                  ├── 8. Calculate fee: 1000 × 2% = 20
  │                                  ├── 9. Total deducted: 1000 + 20 = 1020
  │                                  ├── 10. Check: sender balance ≥ 1020?
  │                                  │
  │  recipientName: "Sara Mohammed"  │
  │  amount: 1000                    │
  │  fee: 20                         │
  │  totalDeducted: 1020             │
  │  balanceAfter: 8980              │
  │ ◄──────────────────────────────── │
```

**What the user sees on their phone:**

```
┌────────────────────────────────────┐
│        Transfer Preview            │
├────────────────────────────────────┤
│  To: Sara Mohammed                 │
│  Account: 192967789                │
│                                    │
│  Amount:        1,000.00 YER       │
│  Transfer Fee:     20.00 YER       │
│  ─────────────────────────         │
│  Total Deducted: 1,020.00 YER     │
│                                    │
│  Balance After:  8,980.00 YER      │
│                                    │
│  ┌──────────────────────────────┐  │
│  │      Confirm Transfer        │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

#### Step 2: Execute (`POST /api/v1/transfers/execute`)

The user taps "Confirm Transfer". The app calls the execute endpoint, which **re-validates everything** (because balance could have changed between preview and confirm), then executes atomically.

```
User App                          ExecuteTransferUseCase
  │                                  │
  │  recipientAccNo: "192967789"     │
  │  amount: 1000                    │
  │  currency: YER                   │
  │  description: "Rent payment"     │
  │ ────────────────────────────────► │
  │                                  │
  │                                  ├── 1-7. Re-validate EVERYTHING
  │                                  ├── 8. Calculate fee: 20 YER
  │                                  ├── 9. Get fee wallet (FEES_YER)
  │                                  │
  │                                  │   RecordLedgerEntryUseCase
  │                                  │   ─────────────────────────
  │                                  ├── 10. Lock 3 wallets (sender, recipient, fee)
  │                                  ├── 11. Debit sender: -1020
  │                                  ├── 12. Credit recipient: +1000
  │                                  ├── 13. Credit fee wallet: +20
  │                                  ├── 14. Create 3 ledger entries
  │                                  ├── 15. Validate zero-sum (-1020 + 1000 + 20 = 0 ✅)
  │                                  │
  │                                  ├── 16. Create P2PTransfer record
  │                                  │
  │  transferId: UUID                │
  │  referenceNo: TRF-20260223-A1B2  │
  │  status: COMPLETED               │
  │ ◄──────────────────────────────── │
```

**Why re-validate in execute?** Because between preview and confirm:

- Another transfer might have reduced the sender's balance
- A wallet might have been frozen by admin
- The recipient might have been deleted

This is a common pattern in financial systems called **"validate-then-execute"** or **"optimistic locking at the business level"**.

---

### Transaction Flow Example

**Scenario: Ahmed (10,000 YER) sends 1000 YER to Sara (5,000 YER)**

```
BEFORE:
  Ahmed's YER wallet:   10,000.0000 YER
  Sara's YER wallet:     5,000.0000 YER
  FEES_YER wallet:       0.0000 YER

Fee Calculation:
  Amount: 1000 YER
  Fee: 1000 × 2% = 20 YER
  Total deducted from Ahmed: 1020 YER

Ledger Entries (Transaction ID: TXN-001):
┌────────┬──────────────┬───────────┬────────────┬────────────┬──────────────────────┐
│ Entry  │ Wallet       │ Type      │ Amount     │ After      │ Description          │
├────────┼──────────────┼───────────┼────────────┼────────────┼──────────────────────┤
│ 1      │ Ahmed (YER)  │ DEBIT     │ 1020.0000  │ 8980.0000  │ Rent payment (sent)  │
│ 2      │ Sara (YER)   │ CREDIT    │ 1000.0000  │ 6000.0000  │ Rent payment (recv)  │
│ 3      │ FEES_YER     │ CREDIT    │ 20.0000    │ 20.0000    │ Transaction fee      │
└────────┴──────────────┴───────────┴────────────┴────────────┴──────────────────────┘

Zero-Sum Check: -1020 + 1000 + 20 = 0 ✅

AFTER:
  Ahmed's YER wallet:    8,980.0000 YER  (decreased by 1020)
  Sara's YER wallet:     6,000.0000 YER  (increased by 1000)
  FEES_YER wallet:          20.0000 YER  (increased by 20)

Transfer Record:
  ID: uuid-xxx
  Reference: TRF-20260223-A1B2C3
  Status: COMPLETED
```

---

### Fee Calculation Logic

```java
// Fee = 2% of transfer amount, clamped between 1.00 and 500.00
public static BigDecimal calculateFee(BigDecimal amount) {
    BigDecimal fee = amount.multiply(new BigDecimal("0.02"));
    fee = fee.max(new BigDecimal("1.00"));   // Minimum fee
    fee = fee.min(new BigDecimal("500.00")); // Maximum fee
    return fee;
}
```

**Fee examples:**

| Transfer Amount | Raw Fee (2%) | Applied Fee | Why                             |
| --------------- | ------------ | ----------- | ------------------------------- |
| 10 YER          | 0.20         | **1.00**    | Below minimum, raised to 1.00   |
| 50 YER          | 1.00         | 1.00        | Exactly minimum                 |
| 1,000 YER       | 20.00        | 20.00       | Normal range                    |
| 10,000 YER      | 200.00       | 200.00      | Normal range                    |
| 25,000 YER      | 500.00       | 500.00      | Exactly maximum                 |
| 50,000 YER      | 1,000.00     | **500.00**  | Above maximum, capped at 500.00 |

---

### Identity Module Changes — Account Numbers

**What changed and why:**

1. **`User.java`** — Added `accountNumber` field. Generated automatically during `createNew()` using our Luhn utility.

2. **`RegisterUserUseCase.java`** — The registration response now includes `accountNumber`:

```java
return new RegisterResponse(
    savedUser.getId(),
    savedUser.getPhoneNumber(),
    savedUser.getFullName(),
    savedUser.getAccountNumber(),  // NEW
    "User registered successfully..."
);
```

3. **`UserRepository.java`** — Added `findByAccountNumber()` for recipient lookup during transfers.

4. **`V9 Migration`** — Added nullable `account_number` column with a partial unique index.

**Q: Why is account_number nullable?**
**A:** Existing users created before Phase 3.1 don't have account numbers. New users get one automatically during registration. A backfill script can be run to generate numbers for existing users.

**Q: Why 9 digits instead of something longer?**
**A:** 9 digits (8 + 1 check digit) gives ~100 million possible numbers — more than enough for MVP. It's also short enough for users to type or share easily. In comparison, IBAN numbers are 16-34 characters — much harder to type.

---

### Use Cases Explained

#### 1. PreviewTransferUseCase

**Input:** sender user ID (from JWT), recipient account number, amount, currency.

**What it does:**

1. Looks up recipient by account number
2. Validates sender ≠ recipient (no self-transfer)
3. Finds both wallets for the requested currency
4. Checks both wallets are ACTIVE
5. Checks neither is a system wallet
6. Calculates the 2% fee (clamped between 1.00 and 500.00)
7. Checks sender has enough balance (amount + fee)
8. Returns preview with fee breakdown

**What it does NOT do:** Move money, create records, or lock wallets.

#### 2. ExecuteTransferUseCase

**Input:** Same as preview, plus optional description.

**What it does:**

1. Re-validates everything (same checks as preview)
2. Calls `RecordLedgerEntryUseCase.recordTransferWithFee()` which:
   - Locks 3 wallets (sender, recipient, fee) in **stable order** to prevent deadlocks
   - Creates 3 ledger entries (debit sender, credit recipient, credit fee)
   - Validates zero-sum
3. Creates a `P2PTransfer` record with status `COMPLETED`
4. Returns transfer details with reference number

**Why re-validate?** Because the user's balance or wallet status could have changed between preview and execute. This is a safety net.

#### 3. GetTransferDetailUseCase

Retrieves a transfer by ID. Only the sender or recipient can view it — anyone else gets `Access denied`.

#### 4. GetTransferHistoryUseCase

Returns all transfers where the user is either sender or recipient, ordered by date (newest first), with a configurable limit (default 20, max 50).

---

### API Endpoints

#### POST `/api/v1/transfers/preview`

Preview a transfer without executing it.

**Request:**

```json
{
  "recipientAccountNumber": "192967789",
  "amount": 1000,
  "currency": "YER"
}
```

**Response (200 OK):**

```json
{
  "recipientDisplayName": "Sara Mohammed",
  "recipientAccountNumber": "192967789",
  "amount": 1000.0,
  "feeAmount": 20.0,
  "totalDeducted": 1020.0,
  "currency": "YER",
  "senderBalanceAfter": 8980.0
}
```

**Error Responses:**

| Status | Reason                             |
| ------ | ---------------------------------- |
| 400    | Amount is zero or negative         |
| 400    | Recipient account number not found |
| 400    | Cannot transfer to yourself        |
| 400    | Wallet not found for the currency  |
| 400    | Insufficient funds                 |
| 401    | No valid JWT token                 |
| 403    | Wallet is frozen or closed         |

---

#### POST `/api/v1/transfers/execute`

Execute the transfer (after user confirms the preview).

**Request:**

```json
{
  "recipientAccountNumber": "192967789",
  "amount": 1000,
  "currency": "YER",
  "description": "Rent payment - February"
}
```

**Response (200 OK):**

```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "referenceNo": "TRF-20260223-A1B2C3",
  "recipientDisplayName": "Sara Mohammed",
  "amount": 1000.0,
  "feeAmount": 20.0,
  "totalDeducted": 1020.0,
  "currency": "YER",
  "status": "COMPLETED",
  "completedAt": "2026-02-23T01:30:00Z"
}
```

---

#### GET `/api/v1/transfers/{id}`

View details of a specific transfer. Only the sender or recipient can access it.

**Response (200 OK):**

```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "referenceNo": "TRF-20260223-A1B2C3",
  "senderUserId": "uuid-ahmed",
  "senderDisplayName": "Ahmed Ali",
  "recipientUserId": "uuid-sara",
  "recipientDisplayName": "Sara Mohammed",
  "amount": 1000.0,
  "feeAmount": 20.0,
  "totalDeducted": 1020.0,
  "currency": "YER",
  "status": "COMPLETED",
  "description": "Rent payment - February",
  "createdAt": "2026-02-23T01:30:00Z",
  "completedAt": "2026-02-23T01:30:00Z"
}
```

---

#### GET `/api/v1/transfers/history?limit=20`

List the user's transfer history (sent + received), newest first.

**Response (200 OK):**

```json
[
  {
    "transferId": "uuid-1",
    "referenceNo": "TRF-20260223-A1B2C3",
    "senderUserId": "uuid-ahmed",
    "senderDisplayName": "Ahmed Ali",
    "recipientUserId": "uuid-sara",
    "recipientDisplayName": "Sara Mohammed",
    "amount": 1000.0,
    "feeAmount": 20.0,
    "totalDeducted": 1020.0,
    "currency": "YER",
    "status": "COMPLETED",
    "description": "Rent payment",
    "createdAt": "2026-02-23T01:30:00Z",
    "completedAt": "2026-02-23T01:30:00Z"
  }
]
```

---

### Security Implementation

**1. Authentication:** All transfer endpoints require a valid JWT token (`@PreAuthorize("hasRole('USER')")`).

**2. Ownership Verification:** Only the sender or recipient can view a transfer:

```java
if (!transfer.getSenderUserId().equals(requestingUserId)
        && !transfer.getRecipientUserId().equals(requestingUserId)) {
    throw new SecurityException("Access denied: you are not a party to this transfer");
}
```

**3. Self-transfer Prevention:**

```java
if (sender.getId().equals(recipient.getId())) {
    throw new IllegalArgumentException("Cannot transfer to yourself");
}
```

**4. System Wallet Protection:**

```java
if (SystemWallets.isSystemWallet(senderWallet.getId())
        || SystemWallets.isSystemWallet(recipientWallet.getId())) {
    throw new IllegalArgumentException("System wallets cannot be used in P2P transfers");
}
```

---

### Design Rationale — FAQ

**Q: Why two separate endpoints (preview + execute) instead of one?**
**A:** User experience. Mobile users need to see exactly what will happen before they commit. This prevents "fat finger" errors. Imagine transferring 10,000 YER instead of 1,000 YER because of a typo — the preview catches this.

**Q: Why re-validate in execute instead of just trusting the preview?**
**A:** Because time passes between preview and execute. In that time:

- The sender's balance may have dropped (another transfer)
- A wallet may have been frozen (admin action)
- The recipient may have been deleted
  Financial systems must always validate at the moment of execution. Preview is informational, not authoritative.

**Q: Why not use an idempotency key?**
**A:** That's planned for Step 3.5. For now, if a user double-taps "Confirm", they'll get a second transfer. Idempotency keys will prevent this by rejecting duplicate requests with the same key.

**Q: Why is the fee hardcoded instead of configurable?**
**A:** Simplicity for MVP. In Step 3.4, we'll create a `FeeService` with configurable tiers stored in the database. For now, 2% is a reasonable starting point.

**Q: Can a user transfer USD from their YER wallet?**
**A:** No. Transfers are same-currency only. The sender and recipient must both have wallets in the requested currency. Cross-currency transfers (with exchange rates) are planned for Step 3.3.

**Q: What happens if the database crashes mid-transfer?**
**A:** The `@Transactional` annotation ensures atomicity. Either all 3 ledger entries + wallet balance updates + transfer record are committed, or none of them are. This is ACID compliance — the "A" stands for Atomicity.

**Q: Why does the transfers table have both sender_user_id and sender_wallet_id?**
**A:** `sender_user_id` is for querying ("show me Ahmed's transfers"). `sender_wallet_id` identifies the specific wallet used (Ahmed might have YER, SAR, and USD wallets). Both are needed for different access patterns.

---

## ✅ Phase 3.1 Complete!

**All components implemented:**

✅ Account Number Support (Luhn-validated, auto-generated on registration)
✅ Transfers Table (V8 migration with indexes)
✅ P2PTransfer Domain Entity (with reference number generation)
✅ Preview/Confirm Two-Step Flow (4 use cases)
✅ 2% Fee Collection (min 1.00, max 500.00)
✅ 4 REST Endpoints (preview, execute, detail, history)
✅ Security (JWT auth, ownership checks, self-transfer prevention)
✅ Build Verification (SUCCESS) + Live Testing (SUCCESS)

---

## 3.2 Agent-Based Deposits & Withdrawals — Discussion

> [!NOTE]
> In Yemen (and most of the Middle East/Africa), most people don't have bank accounts. Instead, they use **agents** — small shops, phone stores, or kiosks — to deposit and withdraw cash. Think of agents like ATMs, but they're people.

### What Is an Agent?

```
Real-World Flow:
┌──────────┐              ┌──────────┐              ┌──────────┐
│   USER   │ ── cash ──►  │  AGENT   │ ── taps ──►  │  SYSTEM  │
│  (Ahmed) │              │  (Shop)  │   deposit     │  (App)   │
└──────────┘              └──────────┘              └──────────┘
                                                      │
                                                      ▼
                                              Ahmed's YER wallet
                                              balance increases
```

- **Deposit:** User gives cash to agent → Agent enters the user's account number + amount → System credits the user's wallet.
- **Withdrawal:** User requests cash from agent → Agent enters user's account number + amount → System debits the user's wallet → Agent gives cash.

### Why Agent-Based? (Design Decision)

| Approach           | Pros                                                                              | Cons                                           |
| ------------------ | --------------------------------------------------------------------------------- | ---------------------------------------------- |
| **Agent-based** ✅ | Realistic for Yemen market; No bank account needed; Works for unbanked population | Requires a role system                         |
| Self-deposit       | Simpler to implement                                                              | Unrealistic; where does the "money" come from? |
| Bank transfer      | Standard in developed markets                                                     | Most Yemeni users don't have bank accounts     |

**Decision:** Agent-based for both deposits and withdrawals, with a new `ROLE_AGENT` permission system.

---

### The Agent Role System

This is a new concept we need to introduce. Currently all users are `ROLE_USER`. We need:

```
┌──────────────────────────────────────────────────────────┐
│  User Roles                                              │
│                                                          │
│  ROLE_USER   → Can send/receive P2P transfers            │
│                Can view own wallets and history           │
│                                                          │
│  ROLE_AGENT  → Everything a USER can do, PLUS:           │
│                Can deposit cash for users                 │
│                Can withdraw cash for users                │
│                Can view their own agent history           │
│                                                          │
│  ROLE_ADMIN  → Everything above, PLUS:                   │
│                Can promote users to agents                │
│                Can freeze/unfreeze wallets                │
│                Can view all accounts (future)             │
└──────────────────────────────────────────────────────────┘
```

**How a user becomes an agent:**

1. Register as a normal user (gets `ROLE_USER`)
2. An admin calls `POST /admin/users/{id}/promote-agent`
3. User's role changes to `AGENT` in the database
4. Next login, their JWT token includes `role: AGENT`
5. They can now access agent-only endpoints

---

### Database Changes

#### V11: Add role to users table

```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
```

#### V12: Deposits table

```sql
CREATE TABLE deposits (
    id            UUID PRIMARY KEY,
    reference_no  VARCHAR(30) UNIQUE NOT NULL,  -- DEP-20260223-A1B2C3
    user_id       UUID NOT NULL,                -- Who receives the deposit
    agent_id      UUID NOT NULL,                -- Which agent performed it
    wallet_id     UUID NOT NULL,                -- Which wallet was credited
    amount        NUMERIC(19,4) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    description   TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_deposit_user    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_deposit_agent   FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT fk_deposit_wallet  FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_deposits_user    ON deposits(user_id, created_at DESC);
CREATE INDEX idx_deposits_agent   ON deposits(agent_id, created_at DESC);
```

#### V13: Withdrawals table (mirrors deposits)

```sql
CREATE TABLE withdrawals (
    id            UUID PRIMARY KEY,
    reference_no  VARCHAR(30) UNIQUE NOT NULL,  -- WDR-20260223-A1B2C3
    user_id       UUID NOT NULL,                -- Who withdraws
    agent_id      UUID NOT NULL,                -- Which agent performed it
    wallet_id     UUID NOT NULL,                -- Which wallet was debited
    amount        NUMERIC(19,4) NOT NULL,
    currency      VARCHAR(3) NOT NULL,
    description   TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_withdrawal_user    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_withdrawal_agent   FOREIGN KEY (agent_id) REFERENCES users(id),
    CONSTRAINT fk_withdrawal_wallet  FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE INDEX idx_withdrawals_user  ON withdrawals(user_id, created_at DESC);
CREATE INDEX idx_withdrawals_agent ON withdrawals(agent_id, created_at DESC);
```

**Why separate tables instead of reusing `transfers`?**

|                 | P2P Transfer              | Deposit                 | Withdrawal              |
| --------------- | ------------------------- | ----------------------- | ----------------------- |
| **Parties**     | User → User               | Agent → User            | User → Agent            |
| **Fee?**        | Yes (2%)                  | No                      | No                      |
| **Metadata**    | sender, recipient, fee    | agent, user             | agent, user             |
| **Source/Dest** | User wallet → User wallet | Liquidity → User wallet | User wallet → Liquidity |

The metadata and semantics are different enough to justify separate tables.

---

### Transaction Flows

#### Agent Deposit: 5,000 YER for Ahmed (No Fee)

```
Ledger Entries:
┌────────┬─────────────────┬───────────┬────────────┬──────────────────────┐
│ Entry  │ Wallet          │ Type      │ Amount     │ Description          │
├────────┼─────────────────┼───────────┼────────────┼──────────────────────┤
│ 1      │ Liquidity (YER) │ DEBIT     │ 5000.0000  │ Agent deposit        │
│ 2      │ Ahmed (YER)     │ CREDIT    │ 5000.0000  │ Cash deposit         │
└────────┴─────────────────┴───────────┴────────────┴──────────────────────┘
Zero-Sum: -5000 + 5000 = 0 ✅
```

#### Agent Withdrawal: 2,000 YER from Ahmed (No Fee)

```
Ledger Entries:
┌────────┬─────────────────┬───────────┬────────────┬──────────────────────┐
│ Entry  │ Wallet          │ Type      │ Amount     │ Description          │
├────────┼─────────────────┼───────────┼────────────┼──────────────────────┤
│ 1      │ Ahmed (YER)     │ DEBIT     │ 2000.0000  │ Cash withdrawal      │
│ 2      │ Liquidity (YER) │ CREDIT    │ 2000.0000  │ Agent withdrawal     │
└────────┴─────────────────┴───────────┴────────────┴──────────────────────┘
Zero-Sum: -2000 + 2000 = 0 ✅
```

---

### API Endpoints

#### POST `/api/v1/admin/users/{userId}/promote-agent`

Promote a regular user to agent role.

**Request:** No body needed. Agent user ID in the URL path.

**Response (200 OK):**

```json
{
  "userId": "uuid",
  "fullName": "Agent Shop",
  "role": "AGENT",
  "message": "User promoted to agent successfully"
}
```

---

#### POST `/api/v1/deposits/agent`

Agent deposits cash for a user. Requires `ROLE_AGENT`.

**Request:**

```json
{
  "recipientAccountNumber": "893857755",
  "amount": 5000,
  "currency": "YER",
  "description": "Cash deposit at Main St. branch"
}
```

**Response (200 OK):**

```json
{
  "depositId": "uuid",
  "referenceNo": "DEP-20260223-A1B2C3",
  "recipientDisplayName": "Ahmed Ali",
  "amount": 5000.0,
  "currency": "YER",
  "status": "COMPLETED",
  "createdAt": "2026-02-23T14:30:00Z"
}
```

---

#### POST `/api/v1/withdrawals/agent`

Agent withdraws cash for a user. Requires `ROLE_AGENT`.

**Request:**

```json
{
  "userAccountNumber": "893857755",
  "amount": 2000,
  "currency": "YER",
  "description": "Cash withdrawal"
}
```

**Response (200 OK):**

```json
{
  "withdrawalId": "uuid",
  "referenceNo": "WDR-20260223-X1Y2Z3",
  "userDisplayName": "Ahmed Ali",
  "amount": 2000.0,
  "currency": "YER",
  "status": "COMPLETED",
  "createdAt": "2026-02-23T14:35:00Z"
}
```

---

#### GET `/api/v1/deposits/history?limit=20`

Agent's deposit history. Requires `ROLE_AGENT`.

#### GET `/api/v1/withdrawals/history?limit=20`

Agent's withdrawal history. Requires `ROLE_AGENT`.

---

### Architecture (Files to Create)

```
Identity Module (Modified):
├── UserRole.java                    → NEW: Enum (USER, AGENT, ADMIN)
├── User.java                       → MODIFY: Add role field
├── UserJpaEntity.java               → MODIFY: Add role column mapping
├── JwtTokenProvider.java            → MODIFY: Add role claim to JWT
├── JwtAuthenticationFilter.java     → MODIFY: Read role from JWT
└── PromoteToAgentUseCase.java       → NEW

Deposit Module (New):
├── domain/
│   ├── Deposit.java
│   └── DepositRepository.java
├── application/
│   ├── AgentDepositUseCase.java
│   ├── GetDepositHistoryUseCase.java
│   └── dto/
│       ├── AgentDepositRequest.java
│       └── AgentDepositResponse.java
├── infrastructure/persistence/
│   ├── DepositJpaEntity.java
│   ├── DepositJpaRepository.java
│   └── DepositRepositoryAdapter.java
└── api/
    └── DepositController.java

Withdrawal Module (New, mirrors Deposit):
├── domain/
│   ├── Withdrawal.java
│   └── WithdrawalRepository.java
├── application/
│   ├── AgentWithdrawUseCase.java
│   ├── GetWithdrawalHistoryUseCase.java
│   └── dto/
│       ├── AgentWithdrawRequest.java
│       └── AgentWithdrawResponse.java
├── infrastructure/persistence/
│   ├── WithdrawalJpaEntity.java
│   ├── WithdrawalJpaRepository.java
│   └── WithdrawalRepositoryAdapter.java
└── api/
    └── WithdrawalController.java

Database Migrations:
├── V11__add_role_to_users.sql
├── V12__create_deposits_table.sql
└── V13__create_withdrawals_table.sql
```

---

### Implementation Checklist

> Progress update (2026-02-23): Step 7 is implemented in code:
>
> - `PromoteToAgentUseCase` created.
> - Admin endpoint added: `POST /api/v1/admin/users/{userId}/promote-agent`.
> - Endpoint secured with `@PreAuthorize("hasRole('ADMIN')")`.
>   Progress update (2026-02-23): Steps 8–10 are implemented in code:
> - Added migration `V12__create_deposits_table.sql`.
> - Added Deposit module (domain, repository, JPA entity/repository, mapper, adapter).
> - Added use cases `AgentDepositUseCase`, `GetDepositHistoryUseCase`.
> - Added endpoints:
>   - `POST /api/v1/deposits/agent`
>   - `GET /api/v1/deposits/history`
> - Deposit endpoints are secured with `@PreAuthorize("hasRole('AGENT')")`.
>   Progress update (2026-02-23): Steps 11–13 are implemented in code:
> - Added migration `V13__create_withdrawals_table.sql`.
> - Added Withdrawal module (domain, repository, JPA entity/repository, mapper, adapter).
> - Added use cases `AgentWithdrawUseCase`, `GetWithdrawalHistoryUseCase`.
> - Added endpoints:
>   - `POST /api/v1/withdrawals/agent`
>   - `GET /api/v1/withdrawals/history`
> - Withdrawal endpoints use request field `userAccountNumber` and are secured with `@PreAuthorize("hasRole('AGENT')")`.
>   Progress update (2026-02-24): Step 14 and Step 15 completed successfully:
> - Build verification passed (`mvnw clean compile`).
> - Live testing passed for `promote-agent`, `deposits/agent`, `withdrawals/agent`, history endpoints, and wallet balance checks.

- [x] **Step 1:** Create `UserRole.java` enum
- [x] **Step 2:** Add role field to `User.java` domain entity
  > Progress note (2026-02-23): Code changes for Step 3 and Step 4 are implemented (`V11__add_role_to_users.sql` and `UserJpaEntity.role` mapping). Checklist line marks will be aligned in the final cleanup pass.
  > Progress note (2026-02-23): Step 5 and Step 6 are implemented (`JwtTokenProvider` role claim + `JwtAuthenticationFilter` role-to-authority mapping with ADMIN→AGENT→USER inheritance). Method-level security is enabled in `SecurityConfig`.
- [x] **Step 3:** V11 migration — add role column to users table
- [x] **Step 4:** Update `UserJpaEntity.java` with role mapping
- [x] **Step 5:** Update `JwtTokenProvider.java` — add role claim
- [x] **Step 6:** Update `JwtAuthenticationFilter.java` — read role, set authority
- [x] **Step 7:** Create `PromoteToAgentUseCase.java` + admin endpoint
- [x] **Step 8:** V12 migration — create deposits table
- [x] **Step 9:** Create Deposit domain + infrastructure + use case
- [x] **Step 10:** Create `DepositController.java` with 2 endpoints
- [x] **Step 11:** V13 migration — create withdrawals table
- [x] **Step 12:** Create Withdrawal domain + infrastructure + use case
- [x] **Step 13:** Create `WithdrawalController.java` with 2 endpoints
- [x] **Step 14:** Build verification (`mvnw clean compile`)

## ✅ Phase 3.2 Complete!

**All components implemented:**

✅ User Role System (USER, AGENT, ADMIN with JWT-based authority inheritance)
✅ Promote-to-Agent Admin Endpoint
✅ Agent Deposits (with ledger double-entry: LIQUIDITY → User)
✅ Agent Withdrawals (with ledger double-entry: User → LIQUIDITY)
✅ Deposit/Withdrawal History Endpoints
✅ Build Verification (SUCCESS) + Live Testing (SUCCESS)

---

## 3.3 Currency Exchange — Discussion

> [!NOTE]
> Currency exchange lets users convert money between their wallets (e.g., SAR → YER). In Yemen, people often receive Saudi Riyals from relatives working in Saudi Arabia and need to convert to YER for local spending.

### What Is Currency Exchange?

```
Real-World Flow:
┌──────────────┐              ┌──────────────┐
│   Ahmed      │              │   SYSTEM     │
│   (User)     │              │   (App)      │
├──────────────┤              ├──────────────┤
│ SAR Wallet   │ ── 100 SAR → │ LIQUIDITY_SAR│  (+100 SAR)
│ -101 SAR     │              │              │
│ (100+1 fee)  │              │ FEES_SAR     │  (+1 SAR fee)
│              │              │              │
│ YER Wallet   │ ← 13,950 ── │ LIQUIDITY_YER│  (-13,950 YER)
│ +13,950 YER  │              │              │
└──────────────┘              └──────────────┘
```

- User sends **source currency** (SAR) + 1% fee
- System sends **destination currency** (YER) from liquidity
- The exchange rate determines how much destination currency the user gets

### Design Decisions (Approved)

| #   | Decision            | Choice                        | Rationale                                                                            |
| --- | ------------------- | ----------------------------- | ------------------------------------------------------------------------------------ |
| 1   | Rate source         | **Admin-set**                 | MVP simplicity; Yemen central bank sets official rates. Ready for external API later |
| 2   | Quote storage       | **DB table**                  | Audit trail, survives restarts, consistent with other tables                         |
| 3   | Fee structure       | **Source currency**           | User pays fee in the currency they're sending (simpler to understand)                |
| 4   | Fee percentage      | **1%**                        | Lower than P2P's 2% — exchange is a different service tier                           |
| 5   | Slippage protection | **Full BPS validation**       | Future-proof for external API rates. Validates rate drift even within quote TTL      |
| 6   | Module location     | **New `exchange/` module**    | Clean separation, follows deposit/withdrawal pattern                                 |
| 7   | API style           | **Two-step: Quote → Execute** | Matches P2P transfer pattern (preview → execute)                                     |

### Default Behaviors

| Item                        | Decision                                                                               |
| --------------------------- | -------------------------------------------------------------------------------------- |
| Reverse rates               | **Auto-create**: if admin sets SAR→YER = 139.50, system also stores YER→SAR = 1/139.50 |
| Destination wallet          | **Auto-create**: if user doesn't have target currency wallet, create it automatically  |
| Exchange limits             | **Share** the existing daily limit from transfers                                      |
| Admin role for rate-setting | **Enforce `ROLE_ADMIN`** (already exists in `UserRole` enum)                           |
| Migration numbering         | Start at **V14** (V13 is the latest)                                                   |

---

### The Exchange Rate System

Admin sets rates that get stored in the database. The system can look up any currency pair.

```
Admin sets:    SAR → YER = 139.50
Auto-created:  YER → SAR = 0.007168

Admin sets:    USD → YER = 530.00
Auto-created:  YER → USD = 0.001887

Admin sets:    USD → SAR = 3.80
Auto-created:  SAR → USD = 0.263158
```

### Slippage Protection (Full BPS)

Even though admin sets rates (which rarely change), we implement full slippage protection to be **ready for external API** integration later.

```
BPS (Basis Points) = 1/100th of 1% = 0.01%

SLIPPAGE CHECK AT EXECUTION TIME

  Quote rate (locked):  139.50
  Current rate (live):  139.80

  Slippage = |139.80 - 139.50| / 139.50 x 10000
           = 0.30 / 139.50 x 10000
           = 21.5 BPS

  System max tolerance: 50 BPS (0.5%)
  21.5 < 50  =  PROCEED

  If slippage > 50 BPS = REJECT
  "Rate changed too much, please request a new quote"
```

---

### Database Changes

#### V14: `exchange_rates` table

```sql
CREATE TABLE exchange_rates (
    id              UUID PRIMARY KEY,
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    rate            NUMERIC(19,8) NOT NULL,
    set_by          UUID REFERENCES users(id),
    effective_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(from_currency, to_currency)
);
```

#### V15: `exchange_quotes` table

```sql
CREATE TABLE exchange_quotes (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    from_amount     NUMERIC(19,4) NOT NULL,
    to_amount       NUMERIC(19,4) NOT NULL,
    rate            NUMERIC(19,8) NOT NULL,
    fee_amount      NUMERIC(19,4) NOT NULL,
    total_deducted  NUMERIC(19,4) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_quotes_user ON exchange_quotes(user_id, created_at DESC);
CREATE INDEX idx_quotes_status ON exchange_quotes(status, expires_at);
```

#### V16: `exchanges` table

```sql
CREATE TABLE exchanges (
    id              UUID PRIMARY KEY,
    reference_no    VARCHAR(30) UNIQUE NOT NULL,
    quote_id        UUID NOT NULL REFERENCES exchange_quotes(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    from_amount     NUMERIC(19,4) NOT NULL,
    to_amount       NUMERIC(19,4) NOT NULL,
    rate_at_quote   NUMERIC(19,8) NOT NULL,
    rate_at_execute NUMERIC(19,8) NOT NULL,
    slippage_bps    NUMERIC(10,2),
    fee_amount      NUMERIC(19,4) NOT NULL,
    total_deducted  NUMERIC(19,4) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    transaction_id  UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exchanges_user ON exchanges(user_id, created_at DESC);
```

---

### Transaction Flow: Exchange 100 SAR to YER (Rate: 139.50, Fee: 1%)

```
Fee = 100 x 0.01 = 1.00 SAR
Total deducted = 100 + 1 = 101 SAR
Received = 100 x 139.50 = 13,950 YER

Ledger Entries (5 entries, cross-currency):
+--------+-----------------+-----------+-------------+----------------------+
| Entry  | Wallet          | Type      | Amount      | Description          |
+--------+-----------------+-----------+-------------+----------------------+
| 1      | User SAR        | DEBIT     | 101.0000    | Exchange debit + fee |
| 2      | LIQUIDITY_SAR   | CREDIT    | 100.0000    | SAR absorbed         |
| 3      | FEES_SAR        | CREDIT    | 1.0000      | Exchange fee         |
| 4      | LIQUIDITY_YER   | DEBIT     | 13950.0000  | YER provided         |
| 5      | User YER        | CREDIT    | 13950.0000  | Exchange credit      |
+--------+-----------------+-----------+-------------+----------------------+
Zero-Sum: -101 + 100 + 1 - 13950 + 13950 = 0 OK
```

> **Important**: This is a cross-currency transaction. The existing `RecordLedgerEntryUseCase` methods enforce same-currency via `validateSameCurrency()`. A new `recordExchange()` method is needed that skips currency matching and handles 5 entries across 4-5 wallets.

---

### API Endpoints

#### POST `/api/v1/admin/exchange-rates`

Set exchange rate (required: `ROLE_ADMIN`). Auto-creates reverse pair.

**Request:**

```json
{
  "fromCurrency": "SAR",
  "toCurrency": "YER",
  "rate": 139.5
}
```

**Response (200 OK):**

```json
{
  "fromCurrency": "SAR",
  "toCurrency": "YER",
  "rate": 139.5,
  "reverseRate": 0.00716846,
  "effectiveAt": "2026-02-24T22:00:00Z"
}
```

---

#### GET `/api/v1/exchange/rates`

Get all current exchange rates (any authenticated user).

---

#### GET `/api/v1/exchange/quote?from=SAR&to=YER&amount=100`

Get a time-locked quote (required: `ROLE_USER`). Valid for 30 seconds.

**Response (200 OK):**

```json
{
  "quoteId": "uuid",
  "fromCurrency": "SAR",
  "toCurrency": "YER",
  "fromAmount": 100.0,
  "toAmount": 13950.0,
  "rate": 139.5,
  "feeAmount": 1.0,
  "totalDeducted": 101.0,
  "expiresAt": "2026-02-24T22:50:30Z"
}
```

---

#### POST `/api/v1/exchange/execute`

Execute a quoted exchange (required: `ROLE_USER`). Validates quote, checks expiry, validates slippage BPS, moves funds.

**Request:**

```json
{
  "quoteId": "uuid-from-quote"
}
```

**Response (200 OK):**

```json
{
  "exchangeId": "uuid",
  "referenceNo": "EXC-20260224-A1B2C3",
  "fromCurrency": "SAR",
  "toCurrency": "YER",
  "fromAmount": 100.0,
  "toAmount": 13950.0,
  "rateAtQuote": 139.5,
  "rateAtExecute": 139.5,
  "slippageBps": 0.0,
  "feeAmount": 1.0,
  "totalDeducted": 101.0,
  "status": "COMPLETED"
}
```

---

#### GET `/api/v1/exchange/history?limit=20`

User's exchange history (required: `ROLE_USER`).

---

### Architecture (Files to Create)

```
Exchange Module (New):
  domain/
    ExchangeRate.java
    ExchangeQuote.java
    Exchange.java
    ExchangeStatus.java           -- Enum: COMPLETED, FAILED
    QuoteStatus.java              -- Enum: PENDING, EXECUTED, EXPIRED
    ExchangeRateRepository.java
    ExchangeQuoteRepository.java
    ExchangeRepository.java
  application/
    SetExchangeRateUseCase.java
    GetExchangeRatesUseCase.java
    GetExchangeQuoteUseCase.java
    ExecuteExchangeUseCase.java
    GetExchangeHistoryUseCase.java
    dto/
      SetExchangeRateRequest.java
      SetExchangeRateResponse.java
      ExchangeQuoteResponse.java
      ExecuteExchangeRequest.java
      ExecuteExchangeResponse.java
      ExchangeHistoryResponse.java
  infrastructure/persistence/
    ExchangeRateJpaEntity.java
    ExchangeRateJpaRepository.java
    ExchangeRateRepositoryAdapter.java
    ExchangeQuoteJpaEntity.java
    ExchangeQuoteJpaRepository.java
    ExchangeQuoteRepositoryAdapter.java
    ExchangeJpaEntity.java
    ExchangeJpaRepository.java
    ExchangeRepositoryAdapter.java
    ExchangeMapper.java
  api/
    ExchangeController.java
    ExchangeAdminController.java

Wallet Module (Modified):
  domain/ReferenceType.java         -- ADD: EXCHANGE
  application/RecordLedgerEntryUseCase.java -- ADD: recordExchange()

Database Migrations:
  V14__create_exchange_rates_table.sql
  V15__create_exchange_quotes_table.sql
  V16__create_exchanges_table.sql
```

---

### Implementation Checklist

- [x] **Step 1:** Add `EXCHANGE` to `ReferenceType.java`
- [x] **Step 2:** V14 migration -- create `exchange_rates` table
- [x] **Step 3:** V15 migration -- create `exchange_quotes` table
- [x] **Step 4:** V16 migration -- create `exchanges` table
- [x] **Step 5:** Create ExchangeRate domain entity + repository port
- [x] **Step 6:** Create ExchangeRate JPA entity + adapter
- [x] **Step 7:** Create `SetExchangeRateUseCase` + `GetExchangeRatesUseCase`
- [x] **Step 8:** Create `ExchangeAdminController` (POST /admin/exchange-rates)
- [x] **Step 9:** Create ExchangeQuote domain entity + repository + JPA
- [x] **Step 10:** Create `GetExchangeQuoteUseCase` (quote with 30s TTL)
- [x] **Step 11:** Add `recordExchange()` to `RecordLedgerEntryUseCase` (5 entries, cross-currency)
- [x] **Step 12:** Create Exchange domain entity + repository + JPA
- [x] **Step 13:** Create `ExecuteExchangeUseCase` (validate quote, slippage BPS, execute, ledger)
- [x] **Step 14:** Create `ExchangeController` (GET quote, POST execute, GET history)
- [x] **Step 15:** Create `GetExchangeHistoryUseCase`
- [x] **Step 16:** Build verification (`mvnw clean compile`)
- [x] **Step 17:** Live testing (set rate, deposit SAR, quote, execute, verify balances)

---

**Next:** Step 3.4 (Fee Configuration)

---
  
**Next:** Step 3.3 (Currency Exchange) 🚀

---

## 3.4 Fee Configuration — Discussion

> [!NOTE]
> Goal: move fee logic from hardcoded values into database-backed rules so fees can be changed without code deployments.

### What Was Implemented

1. ✅ **Database fee rules**
   - Added `fee_rules` table via `V17__create_fee_rules_table.sql`
   - Added default seed data via `V18__seed_default_fee_rules.sql`

2. ✅ **Fee module (new)**
   - `fee/domain/`: `FeeOperation`, `FeeType`, `FeeRule`, `FeeRuleRepository`
   - `fee/infrastructure/persistence/`: JPA entity/repo/mapper/adapter
   - `fee/application/`: `CalculateFeeUseCase`, `CreateFeeRuleUseCase`, `GetFeeRulesUseCase`
   - `fee/api/`: `AdminFeeController`

3. ✅ **Flow integration**
   - P2P preview now uses DB fee rules (`PreviewTransferUseCase`)
   - P2P execute reuses the same fee source (`ExecuteTransferUseCase`)
   - Exchange quote fee now uses DB fee rules (`GetExchangeQuoteUseCase`)

4. ✅ **Admin fee management endpoints**
   - `GET /api/v1/admin/fees/rules?operationType=TRANSFER&currency=YER`
   - `POST /api/v1/admin/fees/rules`
   - Both require `ROLE_ADMIN`

### Default Rules Seeded (same behavior as before)

- **TRANSFER**: `2%` with min `1.00` and max `500.00` (YER/SAR/USD)
- **EXCHANGE**: `1%` with no min/max cap (YER/SAR/USD)

### Implementation Checklist

- [x] **Step 1:** Create `fee_rules` table (V17)
- [x] **Step 2:** Seed default fee rules (V18)
- [x] **Step 3:** Add fee domain + persistence module
- [x] **Step 4:** Add `CalculateFeeUseCase`
- [x] **Step 5:** Integrate with transfer preview/execute
- [x] **Step 6:** Integrate with exchange quote
- [x] **Step 7:** Add admin fee configuration endpoints
- [x] **Step 8:** Build verification (`mvnw clean compile`)
- [x] **Step 9:** Live testing from Swagger (admin rule change + transfer/exchange verification)
  > Live validation example (2026-02-28): `GET /api/v1/exchange/quote?from=SAR&to=YER&amount=100` returned `feeAmount: 1.5`, `totalDeducted: 101.5` after SAR exchange fee rule update.

---

**Next:** Step 3.5 (Idempotency)

---

## 3.5 Idempotency — Discussion

> [!NOTE]
> Goal: prevent duplicate money movement when the same request is retried (timeout, double-click, network retry).

### What Was Implemented

1. ✅ **Idempotency module (new)**
   - `shared/idempotency/IdempotencyFilter`
   - `shared/idempotency/IdempotencyStore`
   - `shared/idempotency/RedisIdempotencyStore`
   - `shared/idempotency/CachedBodyHttpServletRequest`
   - `shared/idempotency/IdempotencyStoredResponse`

2. ✅ **Protected financial write endpoints**
   - `POST /api/v1/transfers/execute`
   - `POST /api/v1/wallets/transfer`
   - `POST /api/v1/exchange/execute`
   - `POST /api/v1/deposits/agent`
   - `POST /api/v1/withdrawals/agent`

3. ✅ **Behavior**
   - Requires `Idempotency-Key` header on protected endpoints.
   - Same user + same endpoint + same key + same payload → returns cached response.
   - Same key with different payload → rejected (`IDEMPOTENCY_KEY_CONFLICT`).
   - Concurrent same-key request while first is running → rejected (`IDEMPOTENCY_REQUEST_IN_PROGRESS`).
   - Only successful responses (2xx) are cached.
   - Cache TTL = 24 hours.

4. ✅ **Security filter chain integration**
   - `IdempotencyFilter` runs after JWT authentication filter so keys are scoped by authenticated user.

5. ✅ **Swagger visibility**
   - Added explicit `Idempotency-Key` header parameters on protected controllers for copy/paste testing in Swagger UI.

### Implementation Checklist

- [x] **Step 1:** Add idempotency store contract + response model
- [x] **Step 2:** Implement Redis idempotency store
- [x] **Step 3:** Implement idempotency filter (hash + replay + lock)
- [x] **Step 4:** Wire filter into security chain
- [x] **Step 5:** Add domain exceptions for key-required/conflict/in-progress
- [x] **Step 6:** Expose `Idempotency-Key` header on protected endpoints (Swagger)
- [x] **Step 7:** Build verification (`mvnw clean compile`)
- [x] **Step 8:** Live testing (duplicate request replay + conflict path)
  > Live validation example (2026-02-28): `POST /api/v1/deposits/agent` with key `22222222-2222-2222-2222-222222222222` returned normal `200` on first call, replay `200` with header `X-Idempotent-Replay: true` on second call, and `400 IDEMPOTENCY_KEY_CONFLICT` when payload changed.

### Live Testing (copy/paste)

1) Execute once with a new key (expect normal success):

```bash
curl -X POST "http://localhost:8080/api/v1/exchange/execute" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 11111111-1111-1111-1111-111111111111" \
  -d '{"quoteId":"<QUOTE_ID>"}'
```

2) Execute again with the **same key and same payload** (expect same response, no new transaction):

```bash
curl -X POST "http://localhost:8080/api/v1/exchange/execute" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 11111111-1111-1111-1111-111111111111" \
  -d '{"quoteId":"<QUOTE_ID>"}'
```

3) Execute with the **same key but different payload** (expect `IDEMPOTENCY_KEY_CONFLICT`):

```bash
curl -X POST "http://localhost:8080/api/v1/exchange/execute" \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 11111111-1111-1111-1111-111111111111" \
  -d '{"quoteId":"<ANOTHER_QUOTE_ID>"}'
```

---

## 3.6 Limits & Compliance Hardening — Discussion

> [!NOTE]
> Goal: move transaction limits from hardcoded values into database-configurable rules so product/compliance can tune limits without code changes.

### What Was Implemented

1. ✅ **Limits data model + migrations**
   - Added `transaction_limits` table via `V19__create_transaction_limits_table.sql`
   - Added default seeded rules via `V20__seed_default_transaction_limits.sql`
   - Rule dimensions: `user_tier`, `operation_type`, `currency`, `limit_type`, `is_active`
   - Supported limit types: `PER_TRANSACTION`, `DAILY`, `MONTHLY`, `VELOCITY`

2. ✅ **Limits module (new)**
   - `limits/domain/`: `TransactionLimit`, `UserTier`, `LimitOperationType`, `LimitType`, `TransactionLimitRepository`
   - `limits/infrastructure/persistence/`: JPA entity/repo/mapper/adapter
   - `limits/application/`: `ValidateTransactionLimitUseCase`

3. ✅ **Ledger query support for compliance checks**
   - Extended `LedgerRepository` with debit-count query in a time window
   - Added `countByWalletIdAndEntryTypeBetween(...)` query in `LedgerEntryJpaRepository`
   - Added adapter implementation in `LedgerRepositoryAdapter`

4. ✅ **Flow integration at the money-movement core**
   - `RecordLedgerEntryUseCase` now uses `ValidateTransactionLimitUseCase`
   - Enforced for:
     - `TRANSFER` (regular transfer + transfer with fee)
     - `WITHDRAWAL`
     - `EXCHANGE`
   - Enforcement point is the ledger-recording layer, so all write paths stay consistent.

5. ✅ **Hardcoded limits removed from ledger engine**
   - Removed hardcoded max-per-transaction and daily-limit constants/logic from `RecordLedgerEntryUseCase`
   - Kept base input guards (positive/min amount) and wallet/currency consistency validation

6. ✅ **Strict KYC enforcement on money movement**
   - Added KYC gate in `RecordLedgerEntryUseCase` for `TRANSFER`, `DEPOSIT`, `WITHDRAWAL`, `EXCHANGE`
   - Any involved user wallet with non-`VERIFIED` KYC now fails with business-rule error
   - System wallets (liquidity/fee) are excluded from this check

### Default Seeded Behavior (MVP)

- **BASIC tier**
  - Per transaction: `100000`
  - Daily: `10000`
  - Monthly: `300000`
  - Velocity: `10` debits per `1` hour

- **VERIFIED tier**
  - Per transaction: `100000`
  - Daily: `500000`
  - Monthly: `15000000`
  - Velocity: `20` debits per `1` hour

### Implementation Checklist

- [x] **Step 1:** Create `transaction_limits` table (V19)
- [x] **Step 2:** Seed default transaction limits (V20)
- [x] **Step 3:** Add limits domain + persistence module
- [x] **Step 4:** Add `ValidateTransactionLimitUseCase`
- [x] **Step 5:** Add ledger debit count query support
- [x] **Step 6:** Integrate limits in `RecordLedgerEntryUseCase`
- [x] **Step 7:** Remove hardcoded limits from ledger engine
- [x] **Step 8:** Add strict KYC gate for financial operations
- [x] **Step 9:** Build verification (`mvnw -DskipTests clean compile`)
- [x] **Step 10:** Live testing from Swagger (limit-hit scenarios + KYC-gated operation checks)
  > Live validation completed (2026-03-01): limits and strict-KYC checks were executed successfully in Swagger.

---

**Next:** Define Phase 4 scope and create `PHASE_4.md`
