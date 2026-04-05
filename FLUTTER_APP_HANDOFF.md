# Fintech E-Wallet: Flutter Mobile App Handoff

## What This Document Is

This document is a complete description of the backend system that has already been built and deployed for a FinTech E-Wallet application. The goal is to build a **Flutter mobile app** that connects to this backend so that end-users can manage their digital wallets from their phones.

---

## The Backend: What Has Been Built

A full Spring Boot (Java 21) REST API backend is live and deployed. It handles user identity, financial transactions, KYC compliance, notifications, and admin operations.

### Live Server Information

- **Live API Base URL:** `https://fintech-e-wallet.onrender.com`
- **Swagger UI (Interactive API Docs):** `https://fintech-e-wallet.onrender.com/swagger-ui.html`
- **GitHub Repository (Backend):** `https://github.com/MustafaAmmarD/fintech-e-wallet`

### Authentication Method

The backend uses **JWT (JSON Web Token)** authentication.

- When a user logs in, the server returns an `accessToken` and a `refreshToken`.
- All protected endpoints require the header: `Authorization: Bearer <accessToken>`.
- Access tokens expire after **1 hour**. Use the refresh endpoint to get a new one without asking the user to log in again.
- Refresh tokens expire after **7 days**.

### User Roles

There are 3 roles in the system:

- **USER** — A regular end-user who can send/receive money, pay bills, etc. (This is who the Flutter app is for.)
- **AGENT** — A cash-in/cash-out agent (like a teller) who can deposit or withdraw physical cash into/from a user's wallet.
- **ADMIN** — Has full control: approving KYC, freezing wallets, setting exchange rates, managing fee rules.

### Currency Support

The system supports **multi-currency wallets**. When a user is approved, they get wallets in:

- **YER** (Yemeni Rial) — Primary
- **USD** (US Dollar)
- **SAR** (Saudi Riyal)

### Registration Flow (Important Business Logic)

The user registration and activation flow has multiple steps:

1. User registers with phone number, password, and full name.
2. User uploads KYC documents (ID card, etc.).
3. An Admin reviews and approves the KYC.
4. Once approved, the system automatically activates the user's wallets.
5. Only then can the user perform financial operations.

Until KYC is approved, the user cannot send money, pay bills, etc.

---

## Complete API Reference

Below is every endpoint the backend exposes, grouped by feature area. All paths are relative to the base URL.

### 1. Authentication (`/api/v1/auth`)

| Method | Endpoint           | Auth Required       | Description                                                                                                                                                                    |
| ------ | ------------------ | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| POST   | `/register`        | No                  | Register a new user. Requires: `phoneNumber` (Yemen format `+967XXXXXXXXX`), `password` (8-50 chars), `fullName` (2-100 chars). Optional: `email`, `language`, `referralCode`. |
| POST   | `/login`           | No                  | Login with `phoneNumber` and `password`. Returns `accessToken`, `refreshToken`, and user info.                                                                                 |
| POST   | `/refresh`         | Yes (Refresh Token) | Send the refresh token in the `Authorization: Bearer` header to get a new access token.                                                                                        |
| POST   | `/logout`          | Yes                 | Blacklists the current access token.                                                                                                                                           |
| POST   | `/change-password` | Yes                 | Change password. Requires: `currentPassword`, `newPassword`.                                                                                                                   |

### 2. Wallets (`/api/v1/wallets`)

| Method | Endpoint                   | Auth Required | Description                                                                                                |
| ------ | -------------------------- | ------------- | ---------------------------------------------------------------------------------------------------------- |
| GET    | `/`                        | Yes (USER)    | List all wallets for the logged-in user (YER, USD, SAR). Returns wallet ID, currency, balance, and status. |
| GET    | `/{walletId}`              | Yes (USER)    | Get balance details for a specific wallet.                                                                 |
| GET    | `/{walletId}/transactions` | Yes (USER)    | Get transaction history (ledger entries) for a wallet. Supports `?size=10` query param.                    |
| POST   | `/transfer`                | Yes (USER)    | Transfer money between users. Requires `Idempotency-Key` header to prevent duplicate transfers.            |

### 3. Transfers (`/api/v1/transfers`)

| Method | Endpoint            | Auth Required | Description                                                                                |
| ------ | ------------------- | ------------- | ------------------------------------------------------------------------------------------ |
| POST   | `/preview`          | Yes (USER)    | Preview a transfer before executing. Returns fee breakdown and total. Does NOT move money. |
| POST   | `/execute`          | Yes (USER)    | Execute a confirmed transfer. Requires `Idempotency-Key` header.                           |
| GET    | `/{id}`             | Yes (USER)    | Get details of a specific transfer.                                                        |
| GET    | `/history?limit=20` | Yes (USER)    | Get transfer history (sent + received).                                                    |

### 4. Bill Payments (`/api/v1/bills`)

| Method | Endpoint                    | Auth Required | Description                                                                                 |
| ------ | --------------------------- | ------------- | ------------------------------------------------------------------------------------------- |
| GET    | `/billers?category=TELECOM` | No            | List all available billers (telecom, electricity, water, internet). Can filter by category. |
| POST   | `/preview`                  | Yes (USER)    | Preview a bill payment. Shows fee and total before execution.                               |
| POST   | `/execute`                  | Yes (USER)    | Execute a bill payment. Requires `Idempotency-Key` header.                                  |
| GET    | `/history`                  | Yes (USER)    | Get bill payment history.                                                                   |

### 5. Currency Exchange (`/api/v1/exchange`)

| Method | Endpoint                             | Auth Required | Description                                               |
| ------ | ------------------------------------ | ------------- | --------------------------------------------------------- |
| GET    | `/rates`                             | No            | Get all current exchange rates (YER ↔ USD ↔ SAR).         |
| GET    | `/quote?from=YER&to=USD&amount=1000` | Yes (USER)    | Get a live exchange quote (valid for 30 seconds).         |
| POST   | `/execute`                           | Yes (USER)    | Execute a valid quote. Requires `Idempotency-Key` header. |
| GET    | `/history?limit=20`                  | Yes (USER)    | Get exchange history.                                     |

### 6. KYC (`/api/v1/kyc`)

| Method | Endpoint  | Auth Required | Description                                                                                                           |
| ------ | --------- | ------------- | --------------------------------------------------------------------------------------------------------------------- |
| POST   | `/upload` | Yes (USER)    | Upload a KYC document (multipart form: `documentType` + `file`). Types: `NATIONAL_ID`, `PASSPORT`, `DRIVERS_LICENSE`. |
| GET    | `/status` | Yes (USER)    | Get the user's current KYC verification status and list of uploaded documents.                                        |

### 7. Notifications (`/api/v1/notifications`)

| Method | Endpoint         | Auth Required | Description                                              |
| ------ | ---------------- | ------------- | -------------------------------------------------------- |
| GET    | `/`              | Yes (USER)    | List notifications (newest first). Supports `?limit=20`. |
| GET    | `/unread-count`  | Yes (USER)    | Get count of unread notifications.                       |
| PATCH  | `/{id}/read`     | Yes (USER)    | Mark a specific notification as read.                    |
| POST   | `/mark-all-read` | Yes (USER)    | Mark all notifications as read.                          |

### 8. Referrals (`/api/v1/referrals`)

| Method | Endpoint            | Auth Required | Description                                                        |
| ------ | ------------------- | ------------- | ------------------------------------------------------------------ |
| GET    | `/my-code`          | Yes (USER)    | Get the user's referral code to share with friends.                |
| GET    | `/stats`            | Yes (USER)    | Get referral statistics (how many people signed up with the code). |
| GET    | `/history?limit=20` | Yes (USER)    | Get referral history.                                              |

### 9. Privacy Settings (`/api/v1/privacy`)

| Method | Endpoint    | Auth Required | Description                                                    |
| ------ | ----------- | ------------- | -------------------------------------------------------------- |
| GET    | `/settings` | Yes (USER)    | Get current privacy settings (e.g., show full name to others). |
| PATCH  | `/settings` | Yes (USER)    | Update privacy settings.                                       |

### 10. Device Management (`/api/v1/devices`)
*(⚠️ DEV NOTE: The exact OTP verification flow `[OTP_VERIFICATION_REQUIRED]` is currently disabled in the local `dev` profile to speed up flutter development. Any new deviceId sent to `/login` is automatically trusted without requiring `/request-otp`. Toggle `app.security.device-binding.enabled` to true in `application-dev.yml` if you specifically want to test the OTP flow locally).*

| Method | Endpoint       | Auth Required | Description                              |
| ------ | -------------- | ------------- | ---------------------------------------- |
| GET    | `/`            | Yes (USER)    | List all trusted devices.                |
| DELETE | `/{deviceId}`  | Yes (USER)    | Revoke a trusted device.                 |
| POST   | `/request-otp` | No            | Request OTP for new device verification. |
| POST   | `/verify-otp`  | No            | Verify OTP and register a new device.    |

### 11. Deposits (`/api/v1/deposits`) — Agent Only

| Method | Endpoint            | Auth Required | Description                               |
| ------ | ------------------- | ------------- | ----------------------------------------- |
| POST   | `/agent`            | Yes (AGENT)   | Agent deposits cash into a user's wallet. |
| GET    | `/history?limit=20` | Yes (AGENT)   | Agent deposit history.                    |

### 12. Withdrawals (`/api/v1/withdrawals`) — Agent Only

| Method | Endpoint            | Auth Required | Description                                |
| ------ | ------------------- | ------------- | ------------------------------------------ |
| POST   | `/agent`            | Yes (AGENT)   | Agent withdraws cash from a user's wallet. |
| GET    | `/history?limit=20` | Yes (AGENT)   | Agent withdrawal history.                  |

### 13. Admin (`/api/v1/admin`) — Admin Only

| Method | Endpoint                               | Auth Required | Description                            |
| ------ | -------------------------------------- | ------------- | -------------------------------------- |
| POST   | `/wallets/{walletId}/freeze`           | Yes (ADMIN)   | Freeze a wallet.                       |
| POST   | `/wallets/{walletId}/unfreeze`         | Yes (ADMIN)   | Unfreeze a wallet.                     |
| GET    | `/users?q=searchTerm`                  | Yes (ADMIN)   | Search users by name, email, or phone. |
| GET    | `/users/{userId}`                      | Yes (ADMIN)   | Get a user's full profile.             |
| POST   | `/users/{userId}/promote-agent`        | Yes (ADMIN)   | Promote a user to Agent role.          |
| GET    | `/transactions?userId=...&limit=50`    | Yes (ADMIN)   | View a user's transaction history.     |
| GET    | `/stats`                               | Yes (ADMIN)   | System-wide statistics.                |
| POST   | `/exchange/rates`                      | Yes (ADMIN)   | Set exchange rates.                    |
| GET    | `/fees?operationType=...&currency=...` | Yes (ADMIN)   | Get fee rules.                         |
| POST   | `/fees`                                | Yes (ADMIN)   | Create a fee rule.                     |
| GET    | `/kyc/pending`                         | Yes (ADMIN)   | List accounts pending KYC review.      |
| GET    | `/kyc/all`                             | Yes (ADMIN)   | List all KYC accounts.                 |
| GET    | `/kyc/{userId}`                        | Yes (ADMIN)   | Get KYC details for a user.            |
| POST   | `/kyc/{userId}/approve`                | Yes (ADMIN)   | Approve a user's KYC.                  |
| POST   | `/kyc/{userId}/pend`                   | Yes (ADMIN)   | Set KYC status back to pending.        |
| POST   | `/kyc/documents/{documentId}/approve`  | Yes (ADMIN)   | Approve a specific KYC document.       |

---

## What the Flutter App Should Do

### Target Platform

- **Android** (testing on a physical Android device)

### Primary Audience

The Flutter app is for **regular users** (not Agents, not Admins). It should allow a user to:

1. **Register** a new account with phone number, password, and full name.
2. **Login** and stay logged in (persist JWT tokens securely on the device).
3. **View their dashboard** showing wallet balances (YER, USD, SAR) and recent transactions.
4. **Send money** to other users (preview → confirm → execute flow).
5. **Pay bills** (select a biller, enter amount, preview → confirm → execute).
6. **Exchange currencies** between their own wallets (get quote → execute).
7. **Upload KYC documents** and check their verification status.
8. **View notifications** and mark them as read.
9. **View and share referral code**.
10. **Manage privacy settings**.
11. **View trusted devices** and revoke them.
12. **Change password** and **logout**.

### After the Flutter App

After the mobile app is complete, the next step is to build a **simple admin dashboard** (web-based) for the Admin role to approve KYC, manage users, and view system stats.

---

## Important Notes

- The backend is on Render's free tier, so the server **spins down after inactivity**. The first request after inactivity may take ~50 seconds to respond while the server wakes up. The app should handle this gracefully (show a loading indicator, don't timeout too quickly).
- All financial operations (transfers, bill payments, exchanges) require an **`Idempotency-Key` header** (a unique UUID) to prevent accidental duplicate transactions.
- Phone numbers must be in **Yemen format: `+967XXXXXXXXX`** (9 digits after +967).
- The Swagger UI at the live URL is the best way to explore and test individual endpoints interactively.
