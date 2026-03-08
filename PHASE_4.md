# Phase 4: Feature Extensions

> **Goal**: Add referral program, privacy controls, in-app notifications, admin dashboard, and password management.
> **Estimated Duration**: Weeks 11–13

---

## Phase 4 Overview

> Current execution status (MVP): **Design complete, implementation pending.**
> All 9 design decisions approved (2026-03-01).
> Deferred item (2026-03-02): full manual referral regression sequence is postponed to a later session.

| Step | Title                  | Scope                                               | Status     |
| ---- | ---------------------- | --------------------------------------------------- | ---------- |
| 4.1  | Referral Program       | Code-based referrals, reward lifecycle, tracking    | ⬜ Pending |
| 4.2  | Privacy Settings       | User-controlled name masking in transfers           | ⬜ Pending |
| 4.3  | Notifications (In-App) | DB-stored notifications, unread tracking, mark-read | ⬜ Pending |
| 4.4  | Admin Dashboard APIs   | Freeze/unfreeze, user search, stats, tx lookup      | ⬜ Pending |
| 4.5  | Password Change        | Secure password update with old password check      | ⬜ Pending |

---

## Design Decisions (Finalized 2026-03-01)

| #   | Decision             | Choice                                                                |
| --- | -------------------- | --------------------------------------------------------------------- |
| 1   | Scope Split          | 5 features: Referral + Privacy + Notifications + Admin + Password     |
| 2   | Referral Program     | Code-based, fixed rewards (500 YER referrer, 200 YER referee)         |
| 3   | Privacy Settings     | User-controlled `showFullName` toggle with name masking               |
| 4   | Notifications        | In-app DB notifications; push (Firebase) added when mobile app exists |
| 5   | Admin Dashboard      | Essential ops: freeze/unfreeze, user search, tx lookup, stats         |
| 6   | Profile Management   | Password change only (name/email/language not editable via API)       |
| 7   | Migration Sequence   | V21 (referrals), V22 (privacy), V23 (notifications)                   |
| 8   | Testing Strategy     | Swagger test matrix per feature + edge cases                          |
| 9   | Implementation Order | Privacy → Notifications → Referral → Admin → Password → Wiring        |

---

## 4.1 Referral Program — Design

> [!NOTE]
> The referral system turns existing users into growth agents. Ahmed shares his code, Sara registers with it, and both earn a bonus when Sara becomes active.

### Referral Flow

```
1. Ahmed registers → gets referral code "REF-A1B2C3" (already exists in Phase 1)
2. Ahmed shares code with Sara (outside the app — WhatsApp, in person, etc.)
3. Sara registers with referralCode: "REF-A1B2C3"
4. System creates a Referral record: Ahmed → Sara, status = PENDING
5. Sara completes KYC + first financial transaction
6. System triggers CompleteReferralUseCase:
   a. Credit 500 YER to Ahmed's YER wallet (from LIQUIDITY_YER)
   b. Credit 200 YER to Sara's YER wallet (from LIQUIDITY_YER)
   c. Update referral status → REWARDED
   d. Create notification for Ahmed: "You earned 500 YER!"
```

### Reward Structure

- **Referrer reward:** 500 YER (one-time, per referee)
- **Referee reward:** 200 YER (welcome bonus, one-time)
- **Source:** LIQUIDITY_YER wallet (same as deposits)
- **Constraint:** Each user can only be referred once (`UNIQUE(referee_id)`)

### Database Schema (V21)

```sql
-- V21__create_referrals_table.sql
CREATE TABLE referrals (
    id              UUID PRIMARY KEY,
    referrer_id     UUID NOT NULL REFERENCES users(id),
    referee_id      UUID NOT NULL REFERENCES users(id),
    referral_code   VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- Status lifecycle: PENDING → REWARDED
    -- PENDING: referee registered but hasn't completed first tx yet
    -- REWARDED: both parties received their bonus
    referrer_reward NUMERIC(19,4),
    referee_reward  NUMERIC(19,4),
    rewarded_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(referee_id)  -- each user can only be referred once
);

CREATE INDEX idx_referrals_referrer ON referrals(referrer_id, created_at DESC);
CREATE INDEX idx_referrals_status ON referrals(status);
```

### API Endpoints

| Endpoint                        | Method | Auth | Description                        |
| ------------------------------- | ------ | ---- | ---------------------------------- |
| `GET /api/v1/referrals/my-code` | GET    | USER | Get my referral code + share info  |
| `GET /api/v1/referrals/stats`   | GET    | USER | Count of referrals + total rewards |
| `GET /api/v1/referrals/history` | GET    | USER | List of people I referred + status |

**Note:** Registration (`POST /api/v1/auth/register`) already accepts `referralCode`. We wire it to create a `Referral` record during registration.

### Module Structure

```
referral/
├── domain/
│   ├── Referral.java               -- Domain entity (POJO)
│   ├── ReferralStatus.java         -- Enum: PENDING, REWARDED
│   └── ReferralRepository.java     -- Port (interface)
├── application/
│   ├── LinkReferralUseCase.java     -- Called during registration
│   ├── CompleteReferralUseCase.java  -- Called after first tx
│   ├── GetReferralStatsUseCase.java
│   ├── GetReferralHistoryUseCase.java
│   └── dto/
│       ├── ReferralStatsResponse.java
│       ├── ReferralHistoryResponse.java
│       └── MyReferralCodeResponse.java
├── infrastructure/persistence/
│   ├── ReferralJpaEntity.java
│   ├── ReferralJpaRepository.java
│   ├── ReferralMapper.java
│   └── ReferralRepositoryAdapter.java
└── api/
    └── ReferralController.java
```

### Integration Points

- **Registration:** `RegisterUserUseCase` calls `LinkReferralUseCase` when `referralCode` is provided
- **First transaction:** After any successful ledger debit by a user who has a PENDING referral, trigger `CompleteReferralUseCase`
- **Notifications:** Referral reward triggers notification for the referrer

---

## 4.2 Privacy Settings — Design

> [!NOTE]
> Privacy controls let users decide whether strangers see their full name during transfers.

### How It Works

Each user has a `showFullName` boolean (default: `true`):

```
showFullName = true  → "Sara Mohammed Al-Hakami"    (other users see full name)
showFullName = false → "S*** M***"                  (other users see masked name)
```

**Masking rule:** First letter of each word + asterisks. The user always sees their own full name.

### Database Schema (V22)

```sql
-- V22__add_show_full_name_to_users.sql
ALTER TABLE users ADD COLUMN show_full_name BOOLEAN NOT NULL DEFAULT TRUE;
```

### Affected Response DTOs

These existing DTOs currently return full names and will now use `NameMaskingService`:

- `TransferPreviewResponse.recipientDisplayName`
- `ExecuteTransferResponse.recipientDisplayName`
- `TransferDetailResponse.senderDisplayName` / `recipientDisplayName`
- `AgentDepositResponse.recipientDisplayName`
- `AgentWithdrawResponse.userDisplayName`

### API Endpoints

| Endpoint                         | Method | Auth | Description          |
| -------------------------------- | ------ | ---- | -------------------- |
| `GET /api/v1/privacy/settings`   | GET    | USER | Get current settings |
| `PATCH /api/v1/privacy/settings` | PATCH  | USER | Toggle showFullName  |

### Key Component

**`NameMaskingService`** (in `shared/` or `privacy/` package):

```java
public String getDisplayName(User targetUser, UUID requesterId) {
    // User always sees their own full name
    if (targetUser.getId().equals(requesterId)) {
        return targetUser.getFullName();
    }
    // If target has privacy enabled, mask the name
    if (!targetUser.isShowFullName()) {
        return maskName(targetUser.getFullName());
    }
    return targetUser.getFullName();
}

private String maskName(String fullName) {
    // "Sara Mohammed" → "S*** M***"
    return Arrays.stream(fullName.split(" "))
        .map(word -> word.charAt(0) + "***")
        .collect(Collectors.joining(" "));
}
```

---

## 4.3 Notifications (In-App) — Design

> [!NOTE]
> In-app notifications let users know when money arrives, KYC is approved, or referral rewards are credited — without polling their wallet balance.

### Database Schema (V23)

```sql
-- V23__create_notifications_table.sql
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(30) NOT NULL,
    -- Types: TRANSFER_RECEIVED, DEPOSIT_COMPLETED, WITHDRAWAL_COMPLETED,
    --        EXCHANGE_COMPLETED, KYC_APPROVED, KYC_REJECTED,
    --        REFERRAL_REWARD, SYSTEM
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    reference_type  VARCHAR(30),     -- TRANSFER, DEPOSIT, EXCHANGE, REFERRAL, etc.
    reference_id    UUID,            -- Links to the source record
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
```

### API Endpoints

| Endpoint                                   | Method | Auth | Description                       |
| ------------------------------------------ | ------ | ---- | --------------------------------- |
| `GET /api/v1/notifications?limit=20`       | GET    | USER | List notifications (newest first) |
| `GET /api/v1/notifications/unread-count`   | GET    | USER | Unread count (for badge number)   |
| `PATCH /api/v1/notifications/{id}/read`    | PATCH  | USER | Mark one notification as read     |
| `POST /api/v1/notifications/mark-all-read` | POST   | USER | Mark all as read                  |

### Notification Triggers

| Event              | Recipient | Title                   | Example Message                                   |
| ------------------ | --------- | ----------------------- | ------------------------------------------------- |
| Transfer received  | Recipient | "Transfer Received"     | "You received 1,000 YER from Ahmed Ali"           |
| Deposit completed  | User      | "Deposit Completed"     | "5,000 YER deposited to your wallet"              |
| Withdrawal done    | User      | "Withdrawal Completed"  | "2,000 YER withdrawn from your wallet"            |
| Exchange completed | User      | "Exchange Completed"    | "Exchanged 100 SAR → 13,950 YER"                  |
| KYC approved       | User      | "Identity Verified"     | "Your identity has been verified ✅"              |
| KYC rejected       | User      | "KYC Document Rejected" | "Your KYC was rejected. Please re-submit"         |
| Referral reward    | Referrer  | "Referral Reward"       | "You earned 500 YER! Sara joined using your code" |

### Module Structure

```
notification/
├── domain/
│   ├── Notification.java            -- Domain entity (POJO)
│   ├── NotificationType.java        -- Enum
│   └── NotificationRepository.java  -- Port (interface)
├── application/
│   ├── CreateNotificationUseCase.java    -- Called by other modules
│   ├── GetNotificationsUseCase.java
│   ├── GetUnreadCountUseCase.java
│   ├── MarkReadUseCase.java
│   ├── MarkAllReadUseCase.java
│   └── dto/
│       ├── NotificationResponse.java
│       └── UnreadCountResponse.java
├── infrastructure/persistence/
│   ├── NotificationJpaEntity.java
│   ├── NotificationJpaRepository.java
│   ├── NotificationMapper.java
│   └── NotificationRepositoryAdapter.java
└── api/
    └── NotificationController.java
```

### Future: Mobile App Push Notifications

When a mobile app is built, add Firebase/FCM push on top:

1. Add `POST /api/v1/devices/push-token` endpoint to register FCM tokens
2. Inside `CreateNotificationUseCase`, after saving to DB, call `firebasePushService.send()`
3. The DB notifications become the **history screen**, push becomes the **real-time alert**
4. Nothing in the current design gets thrown away

---

## 4.4 Admin Dashboard APIs — Design

> [!NOTE]
> These endpoints give admins the tools to manage users, investigate transactions, and monitor system health.

### Existing Admin Capabilities (from Phases 1–3)

| Capability            | Endpoint                                       |
| --------------------- | ---------------------------------------------- |
| Promote user to agent | `POST /api/v1/admin/users/{id}/promote-agent`  |
| Set exchange rates    | `POST /api/v1/admin/exchange-rates`            |
| Manage fee rules      | `GET/POST /api/v1/admin/fees/rules`            |
| KYC review & approval | `POST /api/v1/kyc/admin/accounts/{id}/approve` |

### New Admin Endpoints (Phase 4)

| Endpoint                                    | Method | Description                                 |
| ------------------------------------------- | ------ | ------------------------------------------- |
| `POST /api/v1/admin/wallets/{id}/freeze`    | POST   | Freeze a wallet (block all financial ops)   |
| `POST /api/v1/admin/wallets/{id}/unfreeze`  | POST   | Unfreeze a wallet (restore operations)      |
| `GET /api/v1/admin/users/{id}`              | GET    | View full user details + all wallets        |
| `GET /api/v1/admin/users?search=...`        | GET    | Search users by phone number or name        |
| `GET /api/v1/admin/transactions?userId=...` | GET    | View a user's ledger entries (paginated)    |
| `GET /api/v1/admin/stats/summary`           | GET    | System-wide totals (users, wallets, volume) |

All endpoints require `ROLE_ADMIN`.

### Freeze/Unfreeze Behavior

- Frozen wallets reject all debit and credit operations
- `Wallet.freeze()` and `Wallet.unfreeze()` methods already exist in domain
- `WalletStatus.FROZEN` already exists
- Need to wire admin endpoints to call these methods

### Stats Summary Response (Example)

```json
{
  "totalUsers": 1250,
  "totalWallets": 3750,
  "totalTransfers": 8940,
  "totalDeposits": 2100,
  "totalWithdrawals": 890,
  "totalExchanges": 450,
  "activeReferrals": 320
}
```

---

## 4.5 Password Change — Design

> [!NOTE]
> Users need to be able to change their password securely, with old password verification.

### API Endpoint

| Endpoint                               | Method | Auth | Description               |
| -------------------------------------- | ------ | ---- | ------------------------- |
| `POST /api/v1/profile/change-password` | POST   | USER | Change password (old+new) |

### Request/Response

**Request:**

```json
{
  "currentPassword": "OldPassword123",
  "newPassword": "NewSecurePass456"
}
```

**Response (200 OK):**

```json
{
  "message": "Password changed successfully"
}
```

### Business Rules

1. `currentPassword` must match existing hash (BCrypt verify)
2. `newPassword` minimum 8 characters
3. `newPassword` must not equal `currentPassword`
4. On success, update `passwordHash` and `updatedAt`
5. Phone number, name, email, language are **not** changeable via API

### Implementation

- `ChangePasswordUseCase` in `identity/application/`
- `ChangePasswordRequest` DTO with validation annotations
- `ProfileController` in `identity/api/`

---

## Migration Sequence

| Version | File                                   | Feature      | Description                         |
| ------- | -------------------------------------- | ------------ | ----------------------------------- |
| V21     | `V21__create_referrals_table.sql`      | Referral     | Referrals table + indexes           |
| V22     | `V22__add_show_full_name_to_users.sql` | Privacy      | Add show_full_name boolean to users |
| V23     | `V23__create_notifications_table.sql`  | Notification | Notifications table + indexes       |

No migration needed for admin dashboard (queries existing tables) or password change (uses existing column).

---

## Implementation Order

| Step | Feature              | Dependencies                      | Effort |
| ---- | -------------------- | --------------------------------- | ------ |
| 1    | Privacy Settings     | None                              | Small  |
| 2    | Notifications Module | None                              | Medium |
| 3    | Referral Program     | Notifications (for reward alerts) | Medium |
| 4    | Admin Dashboard      | None (uses existing data)         | Medium |
| 5    | Password Change      | None                              | Small  |
| 6    | Notification Wiring  | Steps 2,3,4 complete              | Medium |
| 7    | Final Verification   | All steps complete                | Small  |

---

## Testing Strategy

### Swagger Test Sequence (Per Feature)

**Referral Program:**

> TODO (Deferred): execute the full referral end-to-end regression later.
>
> Deferred checklist:
> - Re-run register/login/device-trust flow for referrer and referee.
> - Re-validate first financial transaction trigger path.
> - Re-validate referral transition `PENDING -> REWARDED`.
> - Re-validate reward balances for both users.
> - Re-validate referral reward notification delivery.

1. Register Ahmed (gets referral code)
2. Register Sara with Ahmed's referral code
3. `GET /referrals/my-code` (Ahmed) → shows code
4. `GET /referrals/stats` (Ahmed) → 1 referral, PENDING
5. KYC approve Sara → deposit → first transaction
6. Verify rewards credited to both wallets
7. Verify notification received by Ahmed

**Privacy Settings:**

1. `GET /privacy/settings` → `showFullName: true`
2. `PATCH /privacy/settings` → `{ "showFullName": false }`
3. Transfer preview to this user → masked name
4. Self-view → full name visible

**Notifications:**

1. Deposit to Ahmed → `GET /notifications` → deposit notification
2. `GET /notifications/unread-count` → 1
3. `PATCH /notifications/{id}/read` → mark read
4. `GET /notifications/unread-count` → 0

**Admin Dashboard:**

1. Freeze wallet → try transfer → rejected
2. Unfreeze → try transfer → success
3. User search + detail view
4. Transaction lookup + stats

**Password Change:**

1. Change password (correct old) → success
2. Login with new → success
3. Login with old → fail
4. Change with wrong old → rejected

### Edge Cases

| Scenario                            | Expected         |
| ----------------------------------- | ---------------- |
| Invalid referral code               | Rejected         |
| Self-referral                       | Rejected         |
| Same user referred twice            | UNIQUE violation |
| Freeze already-frozen wallet        | Idempotent       |
| Mark non-existent notification read | 404              |
| Wrong old password                  | 400              |
| USER calls admin endpoint           | 403              |
