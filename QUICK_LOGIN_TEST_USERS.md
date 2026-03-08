# Quick Login Test Users

Use this file for fast copy/paste during Swagger or API testing.



docker compose up -d
.\mvnw.cmd -DskipTests spring-boot:run




## Base URL

- `http://localhost:8080`

## Shared Password

- `Password123!`

---

## Admin

- Full Name: `Admin User`
- Role: `ADMIN`
- Phone: `+967770900001`
- Account Number: `305330789`
- Suggested Device ID: `admin-device-01`
305330789

Login body:

```json
{
  "phoneNumber": "+967770900001",
  "password": "Password123!",
  "deviceId": "admin-device-01",
  "deviceName": "Swagger Admin Device"
}
```

---

## Agent

- Full Name: `Agent User`
- Role: `AGENT`
- Phone: `+967770900002`
- Account Number: `130224157`
- Suggested Device ID: `agent-device-01`

130224157

Login body:

```json
{
  "phoneNumber": "+967770900002",
  "password": "Password123!",
  "deviceId": "agent-device-01",
  "deviceName": "Swagger Agent Device"
}
```

---

## Customer User

- Full Name: `Customer User`
- Role: `USER`
- Phone: `+967770900003`
- Account Number: `724585799`
- Suggested Device ID: `customer-device-01`

724585799

Login body:

```json
{
  "phoneNumber": "+967770900003",
  "password": "NewPassword456!",
  "deviceId": "customer-device-01",
  "deviceName": "Swagger Customer Device"
}
```

---

## Extra Test Agent (Idempotency)

- Full Name: `Idempotency Agent`
- Role: `AGENT`
- Phone: `+967771112233`
- Account Number: `653998708`
- Suggested Device ID: `idempotency-agent-device-01`

653998708

Login body:

```json
{
  "phoneNumber": "+967771112233",
  "password": "Password123!",
  "deviceId": "idempotency-agent-device-01",
  "deviceName": "Swagger Idempotency Agent Device"
}
```

---

## If Login Fails with OTP Required

1. Call `POST /api/v1/devices/request-otp` with:

```json
{
  "phoneNumber": "+967770900002"
}
```

2. Read OTP from `app_runtime.log`.
3. Call `POST /api/v1/devices/verify-otp` with:

```json
{
  "phoneNumber": "+967770900002",
  "otpCode": "123456",
  "deviceId": "agent-device-01"
}
```


{
  "phoneNumber": "+967770555302",
  "password": "Password123!",
  "deviceId": "mustafaLab",
  "deviceName": "Mustafa Laptop"
}

731131009