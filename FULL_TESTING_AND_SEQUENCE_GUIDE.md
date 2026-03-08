# Full Testing and App Sequence Guide

## 1. Purpose

This guide is the current source of truth for:

1. How to start the project end-to-end.
2. How to test all available API areas.
3. How requests move through the app (the sequence).

This guide matches the current code in `src/main/java/com/fintech/ewallet`.

## 2. Current Stack and Sequence

### 2.1 Runtime Components

- Spring Boot app on `http://localhost:8080`
- PostgreSQL on `localhost:5432` (container: `ewallet-postgres`)
- Redis on `localhost:6379` (container: `ewallet-redis`)

Important: `docker-compose.yml` starts PostgreSQL and Redis only. The app is started with Maven.

### 2.2 High-Level Request Sequence

For an authenticated endpoint:

1. Client sends HTTP request.
2. Spring Security chain runs (`shared/config/SecurityConfig.java`).
3. JWT filter validates token (`identity/infrastructure/security/JwtAuthenticationFilter.java`).
4. Controller handles transport layer (`*/api/*Controller.java`).
5. Use case executes business flow (`*/application/*UseCase.java`).
6. Domain repository interface is used (`*/domain/*Repository.java`).
7. Infrastructure adapter persists/loads data (`*/infrastructure/persistence/*Adapter.java`).
8. PostgreSQL/Redis respond.
9. DTO response is returned to client.

### 2.3 Startup Sequence

1. `EwalletApplication` bootstraps Spring.
2. Dev profile loads (`application-dev.yml`).
3. Docker Compose integration checks services.
4. Flyway validates/applies migrations.
5. JPA + Redis beans initialize.
6. Security filter chain initializes.
7. Tomcat starts on port `8080`.

## 3. Prerequisites

- Java 21
- Docker Desktop
- PowerShell

Check versions:

```powershell
java -version
docker --version
```

## 4. Start Everything

### 4.1 Start PostgreSQL and Redis

```powershell
docker compose up -d
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### 4.2 Start Spring Boot (dev profile)

Set `JAVA_HOME` explicitly so Maven does not pick an older JDK:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:SPRING_PROFILES_ACTIVE="dev"
.\mvnw.cmd spring-boot:run
```

### 4.3 Verify App Health

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP"}
```

### 4.4 API Docs

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## 5. Endpoint Map (Current)

### 5.1 Public Endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/devices/request-otp`
- `POST /api/v1/devices/verify-otp`
- `GET /actuator/health`
- `GET /swagger-ui.html`
- `GET /api-docs`

### 5.2 Protected Endpoints (Require `Authorization: Bearer <accessToken>`)

- `POST /api/v1/auth/logout`
- `GET /api/v1/wallets`
- `GET /api/v1/wallets/{walletId}`
- `GET /api/v1/wallets/{walletId}/transactions?size=10`
- `GET /api/v1/devices`
- `DELETE /api/v1/devices/{deviceId}`
- `POST /api/v1/kyc/upload` (multipart)
- `GET /api/v1/kyc/status`
- `GET /api/v1/kyc/admin/accounts/pending`
- `GET /api/v1/kyc/admin/accounts`
- `GET /api/v1/kyc/admin/accounts/{userId}`
- `POST /api/v1/kyc/admin/accounts/{userId}/approve`
- `POST /api/v1/kyc/admin/accounts/{userId}/pend`

## 6. End-to-End Testing Flow (PowerShell)

Use this exact sequence for smoke testing.

### 6.1 Test Variables

```powershell
$base = "http://localhost:8080"
$phone = "+967777777777"
$password = "Password123!"
$deviceId = "device-win-01"
```

### 6.2 Register User

```powershell
$registerBody = @{
  phoneNumber = $phone
  password = $password
  fullName = "Test User"
  email = "test@example.com"
  language = "en"
} | ConvertTo-Json

$register = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/auth/register" `
  -ContentType "application/json" `
  -Body $registerBody

$register
```

Expected:

- HTTP `200 OK`
- Response contains `id`, `phoneNumber`, `fullName`, `message`
- Wallets are not auto-created at registration; activation is deferred to KYC verification + admin approval

### 6.3 Login and Capture Tokens

```powershell
$loginBody = @{
  phoneNumber = $phone
  password = $password
  deviceId = $deviceId
  deviceName = "Windows Dev Machine"
} | ConvertTo-Json

$login = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$accessToken = $login.accessToken
$refreshToken = $login.refreshToken
$authHeader = @{ Authorization = "Bearer $accessToken" }
```

Expected:

- HTTP `200 OK`
- `accessToken`, `refreshToken`, `expiresIn`, and `user`

### 6.4 List Wallets

```powershell
$wallets = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/wallets" `
  -Headers $authHeader

$wallets
```

Expected:

- HTTP `200 OK`
- Array with at least three wallets (`YER`, `SAR`, `USD`)

### 6.5 Wallet Balance and History

```powershell
$walletId = $wallets[0].walletId

$walletBalance = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/wallets/$walletId" `
  -Headers $authHeader

$walletHistory = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/wallets/$walletId/transactions?size=10" `
  -Headers $authHeader

$walletBalance
$walletHistory
```

Expected:

- HTTP `200 OK`
- New accounts usually return empty or near-empty history
- `size` is capped server-side to max `50`

### 6.6 Refresh Token

The refresh endpoint expects the refresh token in the `Authorization` header.

```powershell
$refreshHeaders = @{ Authorization = "Bearer $refreshToken" }
$refresh = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/auth/refresh" `
  -Headers $refreshHeaders

$newAccessToken = $refresh.accessToken
$newRefreshToken = $refresh.refreshToken
```

Expected:

- HTTP `200 OK`
- New token pair returned
- Old refresh token is invalidated (rotation)

### 6.7 List Devices

```powershell
$devices = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/devices" `
  -Headers $authHeader

$devices
```

### 6.8 Request and Verify OTP for New Device

Request OTP:

```powershell
$otpRequestBody = @{ phoneNumber = $phone } | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/devices/request-otp" `
  -ContentType "application/json" `
  -Body $otpRequestBody
```

In this project state, OTP is logged by the app (not sent by SMS). Check runtime logs and copy the 6-digit code.

Verify OTP:

```powershell
$otpCode = "123456"  # Replace with value from app logs
$verifyOtpBody = @{
  phoneNumber = $phone
  otpCode = $otpCode
  deviceId = "device-win-02"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/devices/verify-otp" `
  -ContentType "application/json" `
  -Body $verifyOtpBody
```

### 6.9 KYC Upload and Status

Allowed file types: `image/jpeg`, `image/png`, `application/pdf`  
Maximum file size: `5 MB`

```powershell
$kycFilePath = ".\images\sample-id.jpg"  # Adjust to an existing file

$kycUpload = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/kyc/upload?documentType=PASSPORT" `
  -Headers $authHeader `
  -Form @{ file = Get-Item $kycFilePath }

$kycStatus = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/kyc/status" `
  -Headers $authHeader

$kycUpload
$kycStatus
```

Stored files go under `./uploads/kyc/<userId>/`.

### 6.9.1 Admin Approval (Activates Wallets)

```powershell
$userId = $register.id

$pendingAccounts = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/kyc/admin/accounts/pending" `
  -Headers $authHeader

$accountDetails = Invoke-RestMethod -Method Get `
  -Uri "$base/api/v1/kyc/admin/accounts/$userId" `
  -Headers $authHeader

$approve = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/kyc/admin/accounts/$userId/approve" `
  -Headers $authHeader

$pendingAccounts
$accountDetails
$approve
```

Expected:

- HTTP `200 OK`
- User KYC becomes `VERIFIED`
- Wallets are activated/created after approval

### 6.9.2 Move Account Back to Pending

```powershell
$userId = $register.id

$pend = Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/kyc/admin/accounts/$userId/pend" `
  -Headers $authHeader

$pend
```

Expected:

- HTTP `200 OK`
- User KYC becomes `PENDING`

### 6.10 Logout

```powershell
Invoke-RestMethod -Method Post `
  -Uri "$base/api/v1/auth/logout" `
  -Headers $authHeader
```

Expected:

- HTTP `204 No Content`
- Access token is blacklisted

## 7. Sequence by Feature

### 7.1 Registration Sequence

1. `POST /api/v1/auth/register` reaches `AuthController.register`.
2. `RegisterUserUseCase` validates uniqueness by phone number.
3. Password is hashed with BCrypt.
4. User is persisted through repository adapter + JPA.
5. Wallet provisioning is deferred until KYC approval.
6. API returns `200` with `RegisterResponse`.

### 7.2 Login Sequence

1. `POST /api/v1/auth/login` reaches `AuthController.login`.
2. `LoginUseCase` verifies user state and password.
3. Trusted device is created or updated.
4. JWT access token and refresh token are generated.
5. API returns token pair plus user snapshot.

### 7.3 Authenticated Wallet Read Sequence

1. Client sends `Authorization: Bearer <accessToken>`.
2. `JwtAuthenticationFilter` parses token and sets principal (`UUID userId`).
3. Wallet controller receives `@AuthenticationPrincipal`.
4. Use case reads wallet data via repository adapter.
5. Response DTO is returned.

### 7.4 KYC Upload Sequence

1. Authenticated multipart request reaches `KycController.uploadDocument`.
2. `UploadKycDocumentUseCase` validates file type and size.
3. File is saved locally by `LocalFileStorageService`.
4. KYC document metadata is persisted.
5. User KYC status becomes `PENDING`.

### 7.5 KYC Approval Sequence

1. Admin loads pending queue via `GET /api/v1/kyc/admin/accounts/pending`.
2. Admin opens full profile via `GET /api/v1/kyc/admin/accounts/{userId}`.
3. Admin approves account via `POST /api/v1/kyc/admin/accounts/{userId}/approve`.
4. `ApproveKycAccountUseCase` marks pending documents `VERIFIED` with reviewer ID.
5. User KYC status becomes `VERIFIED` and `CreateWalletUseCase` activates wallets.

## 8. Error Format and Common Status Codes

Errors are returned in a standard structure (`ApiErrorResponse`):

```json
{
  "timestamp": "2026-02-19T17:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/v1/auth/register"
}
```

Common outcomes:

- `200 OK`: successful read/login/refresh
- `201 Created`: successful register and KYC upload
- `204 No Content`: successful logout or device revoke
- `400 Bad Request`: business/validation errors
- `401 Unauthorized`: missing or invalid access token
- `500 Internal Server Error`: unexpected server failure

## 9. Troubleshooting

### 9.1 `release version 21 not supported`

Cause: Maven is using an older JDK than your shell.

Fix:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
.\mvnw.cmd -v
```

Verify Maven now reports Java 21 before running `spring-boot:run`.

### 9.2 Docker Services Running but App Cannot Connect

Check:

```powershell
docker ps
```

You should see:

- `ewallet-postgres` on `5432`
- `ewallet-redis` on `6379`

### 9.3 Swagger Not Loading

Confirm app health first:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Then open:

- `http://localhost:8080/swagger-ui.html`

## 10. Stop and Clean Up

Stop app:

- If running in current terminal, press `Ctrl+C`.

Stop containers:

```powershell
docker compose down
```

Remove container volumes (full reset):

```powershell
docker compose down -v
```
