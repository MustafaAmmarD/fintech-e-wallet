# Phase 5: Advanced Features & Bill Payments

> **Goal**: Add Bill Payments, idempotency, and prepare for future extensions like QR codes and Scheduled Payments.
> **Estimated Duration**: Weeks 14–15

---

## Phase 5 Overview

> Current execution status: **Step 5.1 (Bill Payments) Complete (2026-03-08).**
> All design decisions approved. QR Payments (Step 5.2) skipped as it is primarily a mobile application responsibility.

| Step | Title                 | Scope                                                      | Status      |
| ---- | --------------------- | ---------------------------------------------------------- | ----------- |
| 5.1  | Bill Payments         | Biller entities, payment endpoints, mock integration, fees | ✅ Complete |
| 5.2  | QR Code Payments      | Generate and decode signed QR tokens                       | ⏭️ Skipped  |
| 5.3  | Scheduled Payments    | Recurring transfers and bill payments                      | ⬜ Pending  |
| 5.4  | Merchant Integration  | Dedicated merchant accounts and settlement                 | ⬜ Pending  |
| 5.5  | Loyalty Program       | Earning points on transactions                             | ⬜ Pending  |
| 5.6  | Reporting & Analytics | Admin dashboard charts and transaction exports             | ⬜ Pending  |

---

## Design Decisions (Finalized 2026-03-07)

| #   | Decision           | Choice                                                                             |
| --- | ------------------ | ---------------------------------------------------------------------------------- |
| 1   | Biller Wallets     | ✅ **YES** — Each biller has a real wallet in the `wallets` table to collect funds |
| 2   | Biller Integration | ✅ **MockBillerService** — Always succeeds, with a configurable failure rate       |
| 3   | Fees               | ✅ **Flat Fee** — Fixed 50 YER fee per bill payment                                |
| 4   | Idempotency        | ✅ **Enabled** — `Idempotency-Key` header required to prevent double-charging      |
| 5   | Notifications      | ✅ **Dedicated** — `BILL_PAYMENT_COMPLETED` notification sent on success           |

---

## 5.1 Bill Payments — Design

> [!NOTE]
> Bill payments allow users to pay utilitarian services (Telecom, Electricity, Internet, Water) directly from their YER wallet. The backend interfaces with external biller APIs (currently simulated via `MockBillerService`).

### Bill Payment Flow

```
1. Client requests billers list (`GET /api/v1/bills/billers`)
2. User selects a biller (e.g., "Yemen Mobile") and enters an amount + their phone number
3. Client requests a preview (`POST /api/v1/bills/preview`)
4. Backend confirms user has sufficient balance for Amount + 50 YER Fee
5. Client sends execution request (`POST /api/v1/bills/execute`) WITH an `Idempotency-Key` header
6. Backend processes payment atomically:
   a. Debits user wallet (Amount + Fee)
   b. Credits biller wallet (Amount)
   c. Credits system fee wallet (Fee)
   d. Calls MockBillerService (external integration)
   e. Records transaction in `bill_payments` table
   f. Triggers asynchronous `BILL_PAYMENT_COMPLETED` notification
```

### Database Schema (V27, V28, V30)

```sql
-- V27__create_billers_table.sql
CREATE TABLE billers (
    id                  UUID PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    category            VARCHAR(30) NOT NULL,
    supported_currency  VARCHAR(3) NOT NULL DEFAULT 'YER',
    wallet_id           UUID NOT NULL REFERENCES wallets(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- V28__create_bill_payments_table.sql
CREATE TABLE bill_payments (
    id                      UUID PRIMARY KEY,
    reference_no            VARCHAR(30) NOT NULL UNIQUE,
    user_id                 UUID NOT NULL REFERENCES users(id),
    biller_id               UUID NOT NULL REFERENCES billers(id),
    customer_account_number VARCHAR(50) NOT NULL,
    amount                  DECIMAL(19,4) NOT NULL,
    fee_amount              DECIMAL(19,4) NOT NULL DEFAULT 0,
    total_deducted          DECIMAL(19,4) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    transaction_id          UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bill_payments_user ON bill_payments(user_id, created_at DESC);

-- V30__allow_bill_payment_reference_type.sql
-- Updates the CHECK constraint on ledger_entries to allow 'BILL_PAYMENT' references.
```

### API Endpoints

| Endpoint                | Method | Auth | Description                                              |
| ----------------------- | ------ | ---- | -------------------------------------------------------- |
| `/api/v1/bills/billers` | GET    | USER | List all active billers (optional `?category=TELECOM`)   |
| `/api/v1/bills/preview` | POST   | USER | Preview payment (calculates 50 YER fee + checks balance) |
| `/api/v1/bills/execute` | POST   | USER | Execute payment (requires `Idempotency-Key` header)      |
| `/api/v1/bills/history` | GET    | USER | View user's past bill payments                           |

### Module Structure

```
bill/
├── domain/
│   ├── Biller.java                  -- Domain entity for the company
│   ├── BillerCategory.java          -- Enum (TELECOM, ELECTRICITY, etc)
│   ├── BillPayment.java             -- Domain entity for the transaction
│   └── BillRepository.java          -- Port (interfaces)
├── application/
│   ├── GetBillersUseCase.java
│   ├── PreviewBillPaymentUseCase.java
│   ├── ExecuteBillPaymentUseCase.java
│   ├── GetBillHistoryUseCase.java
│   ├── MockBillerService.java       -- Simulates external REST APIs
│   └── dto/
│       └── (Various Requests and Responses)
├── infrastructure/persistence/
│   ├── BillerJpaEntity.java
│   ├── BillPaymentJpaEntity.java
│   ├── BillerJpaRepository.java
│   ├── BillPaymentJpaRepository.java
│   ├── BillMapper.java
│   └── BillRepositoryAdapter.java
└── api/
    └── BillPaymentController.java
```

---

## Migration Sequence

| Version | File                                         | Feature       | Description                            |
| ------- | -------------------------------------------- | ------------- | -------------------------------------- |
| V27     | `V27__create_billers_table.sql`              | Bill Payments | Billers master table                   |
| V28     | `V28__create_bill_payments_table.sql`        | Bill Payments | Bill transactions table + indexes      |
| V29     | `V29__seed_billers_dev.sql`                  | Bill Payments | Seeds Yemen Mobile, SabaFon, etc.      |
| V30     | `V30__allow_bill_payment_reference_type.sql` | Ledger        | Allows 'BILL_PAYMENT' check constraint |

---

## Implementation Order

| Step | Feature                    | Dependencies          | Effort |
| ---- | -------------------------- | --------------------- | ------ |
| 1    | Database Migrations        | None                  | Small  |
| 2    | Domain Models (Biller)     | Step 1 complete       | Small  |
| 3    | MockBillerService          | None                  | Small  |
| 4    | Bill Use Cases & DTOs      | Steps 2,3 complete    | Medium |
| 5    | BillPaymentController      | Step 4 complete       | Medium |
| 6    | Notification & Idempotency | Core backend complete | Small  |
| 7    | Final Verification         | All steps complete    | Small  |

---

## Testing Strategy

### Swagger Test Sequence

**Bill Payments:**

1. `GET /api/v1/bills/billers` → Returns array of seeded billers (Yemen Mobile, SabaFon, Electricity, YemenNet).
2. `POST /api/v1/bills/preview` (billerCode: YEMEN_MOBILE, amount: 2500) → Returns preview showing 50 YER fee and 2550 YER total.
3. `POST /api/v1/bills/execute` (Same body, plus `Idempotency-Key` header) → Wallet debited, biller credited, transaction logged.
4. `GET /api/v1/bills/history` → Displays the newly executed bill payment.
5. `GET /api/v1/notifications` → Verify a customized `BILL_PAYMENT_COMPLETED` notification is generated (instead of a generic `TRANSFER_SENT`).

### Edge Cases

| Scenario                                    | Expected         |
| ------------------------------------------- | ---------------- |
| Duplicate execution with same Idempotency   | 200 Cached Resp  |
| Execution missing Idempotency-Key           | 400 Bad Request  |
| Insufficient balance (Including 50 YER fee) | 400 Bad Request  |
| Invalid Biller Code                         | 400 Bad Request  |
| `MockBillerService` fails (random chance)   | Payment Reversed |
