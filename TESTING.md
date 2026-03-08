# E-Wallet Testing Guide

This guide details how to test the E-Wallet application using Docker and cURL (or Postman).

## Prerequisites

- **Docker Desktop** installed and running.
- **Java 21** (if running the app locally without Docker).
- **cURL** or **Postman**.

---

## 1. Environment Setup

Start the database (PostgreSQL) and cache (Redis) using Docker Compose:

```bash
docker-compose up -d
```

Check if services are running:

```bash
docker ps
```

You should see `ewallet-postgres` and `ewallet-redis`.

---

## 2. Running the Application

> [!IMPORTANT]
> The application must run with the `dev` profile to connect to PostgreSQL.

**On Windows (PowerShell):**

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw spring-boot:run
```

**On Mac/Linux:**

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

The app will start on `http://localhost:8080`.

You should see logs showing:

- Flyway migrating the database
- Hibernate creating beans
- Application started successfully

---

## 3. Testing Scenarios

### Scenario A: User Registration (Phase 1 + 2.1)

Register a new user. This should **automatically create 3 wallets** (YER, SAR, USD) for them.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test@example.com",
    "password": "Password123!",
    "phoneNumber": "+967777777777",
    "fullName": "Test User",
    "deviceId": "device-123",
    "deviceType": "ANDROID"
  }'
```

**Expected Response (200 OK):**

```json
{
  "id": "...",
  "username": "testuser1",
  "email": "test@example.com",
  "enabled": true
}
```

---

### Scenario B: User Login (Phase 1.3)

Login to get a JWT token. You will need this token for all subsequent requests.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+967777000001",
    "password": "Password123!",
    "deviceId": "device-ahmed-001"
  }'
```

**Expected Response (200 OK):**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1Ni...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

> [!IMPORTANT]
> **Copy the `accessToken`!** You must use it in the `Authorization` header for the next steps.
> Replace `<TOKEN>` in the commands below with your actual token.

---

### Scenario C: List My Wallets (Phase 2.5)

Verify that YER, SAR, and USD wallets were created.

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer <TOKEN>"
```

**Expected Response (200 OK):**

```json
[
  {
    "walletId": "...",
    "currency": "YER",
    "balance": 0.0,
    "status": "ACTIVE"
  },
  {
    "walletId": "...",
    "currency": "SAR",
    "balance": 0.0,
    "status": "ACTIVE"
  },
  {
    "walletId": "...",
    "currency": "USD",
    "balance": 0.0,
    "status": "ACTIVE"
  }
]
```

> [!NOTE]
> Copy one of the `walletId` values (e.g., the YER wallet) for the next step.

---

### Scenario D: Get Specific Wallet Balance (Phase 2.5)

Check the balance of a specific wallet (optimized cached read).

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/wallets/<WALLET_ID> \
  -H "Authorization: Bearer <TOKEN>"
```

**Expected Response (200 OK):**

```json
{
  "walletId": "...",
  "currency": "YER",
  "balance": 0.0,
  "status": "ACTIVE"
}
```

---

### Scenario E: Transaction History (Phase 2.5)

View transaction history. Since this is a new user, it should be empty.

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/wallets/<WALLET_ID>/transactions \
  -H "Authorization: Bearer <TOKEN>"
```

**Expected Response (200 OK):**

```json
[]
```

---

### Scenario F: Register a Second User (Phase 3.1)

To test P2P transfers, you need two users. Register a second user:

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "sara_test",
    "email": "sara@test.com",
    "password": "Password123!",
    "phoneNumber": "+967777000002",
    "fullName": "Sara Mohammed",
    "deviceId": "device-sara-001",
    "deviceType": "IOS"
  }'
```

**Expected Response (200 OK):**

```json
{
  "id": "6c7ed944-d8dd-4f94-a168-5848decc504c",
  "phoneNumber": "+967777000002",
  "fullName": "Sara Mohammed",
  "accountNumber": "893857755",
  "message": "User registered successfully. Complete KYC and admin approval to activate wallets."
}
```

> [!IMPORTANT]
> **Copy the `accountNumber`!** You need it to send money to this user.
> Both users must complete KYC and get approved before transfers will work (wallets need to exist and be ACTIVE).

---

### Scenario G: Preview a P2P Transfer (Phase 3.1)

Preview a transfer to see the fee breakdown before sending. Use User A's token.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/transfers/preview \
  -H "Authorization: Bearer <TOKEN_USER_A>" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientAccountNumber": "<USER_B_ACCOUNT_NUMBER>",
    "amount": 1000,
    "currency": "YER"
  }'
```

**Expected Response (200 OK):**

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

> [!NOTE]
> No money was moved — this is just a preview. The sender's balance is unchanged.

---

### Scenario H: Execute a P2P Transfer (Phase 3.1)

Confirm the transfer to actually move money. Re-sends the same details plus an optional description.

**Request:**

```bash
curl -X POST http://localhost:8080/api/v1/transfers/execute \
  -H "Authorization: Bearer <TOKEN_USER_A>" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientAccountNumber": "<USER_B_ACCOUNT_NUMBER>",
    "amount": 1000,
    "currency": "YER",
    "description": "Rent payment - February"
  }'
```

**Expected Response (200 OK):**

```json
{
  "transferId": "...",
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

> [!IMPORTANT]
> **Copy the `transferId`** — you'll need it for Scenario I.
> After this, check Scenario C again — User A's YER wallet balance should have decreased by 1020 YER.

---

### Scenario I: View Transfer Details (Phase 3.1)

Get the details of the transfer you just made.

**Request:**

```bash
curl -X GET http://localhost:8080/api/v1/transfers/<TRANSFER_ID> \
  -H "Authorization: Bearer <TOKEN_USER_A>"
```

**Expected Response (200 OK):**

```json
{
  "transferId": "...",
  "referenceNo": "TRF-20260223-A1B2C3",
  "senderUserId": "...",
  "senderDisplayName": "Test User",
  "recipientUserId": "...",
  "recipientDisplayName": "Sara Mohammed",
  "amount": 1000.0,
  "feeAmount": 20.0,
  "totalDeducted": 1020.0,
  "currency": "YER",
  "status": "COMPLETED",
  "description": "Rent payment - February",
  "createdAt": "...",
  "completedAt": "..."
}
```

> [!NOTE]
> Both User A (sender) and User B (recipient) can view this transfer. Any other user will get a 403 error.

---

### Scenario J: View Transfer History (Phase 3.1)

List all transfers (sent and received) for the authenticated user.

**Request:**

```bash
curl -X GET "http://localhost:8080/api/v1/transfers/history?limit=20" \
  -H "Authorization: Bearer <TOKEN_USER_A>"
```

**Expected Response (200 OK):**

```json
[
  {
    "transferId": "...",
    "referenceNo": "TRF-20260223-A1B2C3",
    "senderUserId": "...",
    "senderDisplayName": "Test User",
    "recipientUserId": "...",
    "recipientDisplayName": "Sara Mohammed",
    "amount": 1000.0,
    "feeAmount": 20.0,
    "totalDeducted": 1020.0,
    "currency": "YER",
    "status": "COMPLETED",
    "description": "Rent payment - February",
    "createdAt": "...",
    "completedAt": "..."
  }
]
```

> [!TIP]
> Log in as User B and call the same endpoint — you'll see the same transfer, but from the recipient's perspective.

---

### Scenario K: Verify Balances After Transfer (Phase 3.1)

After the transfer, verify the balances are correct:

1. **User A's YER wallet**: Should be decreased by the **total deducted** (amount + fee).
2. **User B's YER wallet**: Should be increased by the **amount** (excluding fee).

Use Scenario C to check both users' wallets.

---

## 4. Troubleshooting

**Common Issues:**

1. **Connection Refused:**
   - Ensure Docker containers are running (`docker ps`).
   - Ensure the app is running.

2. **401 Unauthorized:**
   - Your token might have expired (15 mins). Log in again to get a new one.
   - Ensure you are sending the header exactly as `Authorization: Bearer <token>`.

3. **403 Forbidden:**
   - You are trying to access a wallet or transfer that doesn't belong to the logged-in user.
   - Check the `walletId` / `transferId` you are using.

4. **"Recipient not found":**
   - The account number is incorrect or the user doesn't exist.
   - Account numbers are 9 digits with Luhn validation — check for typos.

5. **"Insufficient balance":**
   - The sender doesn't have enough balance to cover amount + fee.
   - Remember: a 1000 YER transfer costs 1020 YER (1000 + 20 fee).

6. **"Wallet not found for currency":**
   - Both sender and recipient must have wallets in the requested currency.
   - Wallets are created after KYC approval. Make sure both users are approved.

---

## 5. What's Next? (Phase 3.2+)

Phase 3.1 (P2P Transfers) is complete! Coming next:

- **3.2 Deposits/Withdrawals** — Simulated agent deposits and bank withdrawals
- **3.3 Currency Exchange** — YER ↔ SAR ↔ USD with exchange rates
- **3.4 Fee Configuration** — Dynamic fee tiers from database
- **3.5 Idempotency** — Prevent duplicate transactions
