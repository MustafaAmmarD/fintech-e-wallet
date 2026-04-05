# Backend Changes & Flutter Integration Guide

> **Date:** April 4, 2026
> **Purpose:** This document tells the Flutter project exactly what the backend AI has changed, the exact JSON payloads to send, and what still needs to be built on the Flutter side.

---

## 1. What Changed on the Backend

### 1.1 CORS Enabled
- The backend now accepts requests from **any origin** (`*`). Flutter can call the API from localhost, emulator, or physical device without 403 errors.

### 1.2 Registration API — Extended KYC Fields
`POST /api/v1/auth/register` now accepts **5 additional fields** for KYC data collected during registration.

### 1.3 KYC Image Upload — Batch 3-Part Upload
`POST /api/v1/kyc/upload` now accepts **3 images at once** (`idFront`, `idBack`, `selfie`) via `multipart/form-data` instead of uploading one document at a time.

### 1.4 Device Binding — Disabled in Dev
The OTP device verification (`OTP_VERIFICATION_REQUIRED`) is **bypassed** in the local `dev` profile. Any `deviceId` sent during login is automatically trusted. No need to call `/devices/request-otp` or `/devices/verify-otp` during development.

> ⚠️ **Production still enforces OTP.** When you're ready to test the full OTP flow, set `app.security.device-binding.enabled: true` in `application-dev.yml`.

---

## 2. Exact API Payloads Flutter Must Send

### 2.1 Register — `POST /api/v1/auth/register`
**Content-Type:** `application/json`

```json
{
  "phoneNumber": "+967770000001",
  "password": "MySecure123!",
  "fullName": "مصطفى عمار",
  "email": "mustafa@example.com",
  "language": "ar",
  "referralCode": null,
  "englishFullName": "Mustafa Ammar",
  "gender": "male",
  "dateOfBirth": "1998-05-15",
  "idNumber": "1234567890",
  "maritalStatus": "single"
}
```

| Field             | Type   | Required? | Validation Rules                            |
|-------------------|--------|-----------|---------------------------------------------|
| `phoneNumber`     | String | ✅ Yes    | Must match `+967XXXXXXXXX` (9 digits)       |
| `password`        | String | ✅ Yes    | 8–50 characters                             |
| `fullName`        | String | ✅ Yes    | 2–100 characters (Arabic name)              |
| `email`           | String | ❌ No     | Valid email format if provided               |
| `language`        | String | ❌ No     | Defaults to `"ar"`                           |
| `referralCode`    | String | ❌ No     | Max 20 characters                           |
| `englishFullName` | String | ❌ No     | English version of the name                 |
| `gender`          | String | ❌ No     | `"male"` or `"female"`                      |
| `dateOfBirth`     | String | ❌ No     | Format: `"yyyy-MM-dd"`                      |
| `idNumber`        | String | ❌ No     | National ID number                          |
| `maritalStatus`   | String | ❌ No     | `"single"` or `"married"`                   |

**Success Response (200):**
```json
{
  "id": "uuid-here",
  "phoneNumber": "+967770000001",
  "fullName": "مصطفى عمار",
  "accountNumber": "123456789",
  "message": "User registered successfully. Complete KYC and admin approval to activate wallets."
}
```

---

### 2.2 Login — `POST /api/v1/auth/login`
**Content-Type:** `application/json`

```json
{
  "phoneNumber": "+967770000001",
  "password": "MySecure123!",
  "deviceId": "unique-device-uuid-from-flutter",
  "deviceName": "Samsung Galaxy S23"
}
```

| Field        | Type   | Required? | Notes                                                     |
|--------------|--------|-----------|-----------------------------------------------------------|
| `phoneNumber`| String | ✅ Yes    | Must match `+967XXXXXXXXX`                                |
| `password`   | String | ✅ Yes    | The user's password                                       |
| `deviceId`   | String | ✅ Yes    | A unique UUID per device (generate once, store locally)    |
| `deviceName` | String | ❌ No     | Human-readable name like "Mustafa's Phone"                |

> 💡 **`deviceId`**: Generate a UUID once on first app launch and persist it with `SharedPreferences` or `flutter_secure_storage`. Send this same ID on every login.

**Success Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": "uuid-here",
    "fullName": "مصطفى عمار",
    "phoneNumber": "+967770000001",
    "kycStatus": "NONE"
  }
}
```

---

### 2.3 KYC Image Upload — `POST /api/v1/kyc/upload`
**Content-Type:** `multipart/form-data`
**Auth:** `Authorization: Bearer <accessToken>` (must be logged in)

Send **3 image files** as form parts:

| Part Name | Type           | Description                |
|-----------|----------------|----------------------------|
| `idFront` | File (image)   | Front of the ID card       |
| `idBack`  | File (image)   | Back of the ID card        |
| `selfie`  | File (image)   | Selfie photo of the user   |

**Dart example using `dio`:**
```dart
final formData = FormData.fromMap({
  'idFront': await MultipartFile.fromFile(idFrontPath, filename: 'id_front.jpg'),
  'idBack':  await MultipartFile.fromFile(idBackPath,  filename: 'id_back.jpg'),
  'selfie':  await MultipartFile.fromFile(selfiePath,  filename: 'selfie.jpg'),
});

final response = await dio.post(
  '$baseUrl/api/v1/kyc/upload',
  data: formData,
  options: Options(headers: {'Authorization': 'Bearer $accessToken'}),
);
```

**Accepted file types:** JPEG, PNG, PDF — Max **5 MB** each.

**Success Response (201):** Returns a list of 3 uploaded document records.

---

## 3. What Flutter Still Needs to Implement

### ✅ Already supported by backend (Flutter just needs to connect):

| Feature                        | Endpoint                          | Method |
|--------------------------------|-----------------------------------|--------|
| View wallet balances           | `/api/v1/wallets/`                | GET    |
| View transaction history       | `/api/v1/wallets/{id}/transactions` | GET  |
| Preview a transfer             | `/api/v1/transfers/preview`       | POST   |
| Execute a transfer             | `/api/v1/transfers/execute`       | POST   |
| List billers                   | `/api/v1/bills/billers`           | GET    |
| Preview bill payment           | `/api/v1/bills/preview`           | POST   |
| Execute bill payment           | `/api/v1/bills/execute`           | POST   |
| Get exchange rates             | `/api/v1/exchange/rates`          | GET    |
| Get exchange quote             | `/api/v1/exchange/quote`          | GET    |
| Execute exchange               | `/api/v1/exchange/execute`        | POST   |
| Check KYC status               | `/api/v1/kyc/status`              | GET    |
| List notifications             | `/api/v1/notifications/`          | GET    |
| Get unread notification count  | `/api/v1/notifications/unread-count` | GET |
| Mark notification as read      | `/api/v1/notifications/{id}/read` | PATCH  |
| Get referral code              | `/api/v1/referrals/my-code`       | GET    |
| Get privacy settings           | `/api/v1/privacy/settings`        | GET    |
| Update privacy settings        | `/api/v1/privacy/settings`        | PATCH  |
| List trusted devices           | `/api/v1/devices/`                | GET    |
| Revoke a device                | `/api/v1/devices/{deviceId}`      | DELETE |
| Change password                | `/api/v1/auth/change-password`    | POST   |
| Refresh token                  | `/api/v1/auth/refresh`            | POST   |
| Logout                         | `/api/v1/auth/logout`             | POST   |

### 🔧 Not yet built on backend (tell me if you need these):

| Feature                        | Status      | Notes                                      |
|--------------------------------|-------------|---------------------------------------------|
| OTP verification UI flow       | Backend ready, Flutter UI needed | 3-step flow: request-otp → verify-otp → retry login |
| Push notifications (FCM)       | Not started | Need to add Firebase Cloud Messaging        |
| Profile update endpoint        | Not started | If Flutter needs to edit user profile fields |

---

## 4. Important Reminders for Flutter

1. **Base URL (Local Dev):** `http://<YOUR_PC_IP>:8080` (e.g., `http://192.168.1.10:8080`)
2. **Base URL (Production):** `https://fintech-e-wallet.onrender.com`
3. **All protected endpoints** require: `Authorization: Bearer <accessToken>`
4. **Financial operations** (transfers, bills, exchanges) require an `Idempotency-Key: <UUID>` header to prevent duplicates.
5. **Phone format:** Always `+967XXXXXXXXX` (9 digits after +967).
6. **Android cleartext:** For local HTTP testing, set `android:usesCleartextTraffic="true"` in `AndroidManifest.xml`.
7. **Render cold starts:** The live server may take ~50 seconds on first request after inactivity.
