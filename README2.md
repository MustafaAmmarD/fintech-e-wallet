# نظام المحفظة الرقمية الأساسي (Enterprise Digital Wallet Core)

> **نظام مالي بنكي (Core Banking System) مبني بمعايير Enterprise باستخدام Spring Boot & Kotlin.**
> يركز المشروع على دقة البيانات المالية (Data Integrity)، الأمان العالي (High Security)، وقابلية التوسع (Scalability) باستخدام مبادئ Clean Architecture و Domain-Driven Design.

---

## 📋 جدول المحتويات
1. [نظرة عامة وتقنية (Overview & Stack)](#1-نظرة-عامة-وتقنية)
2. [المعمارية الهندسية (System Architecture)](#2-المعمارية-الهندسية)
3. [التحليل المالي والهندسي للميزات (Feature Analysis)](#3-التحليل-المالي-والهندسي-للميزات)
    - [إدارة الهوية والأجهزة (Identity & Device Binding)](#أ-إدارة-الهوية-والأجهزة-identity--device-binding)
    - [المحافظ ودفتر الأستاذ (Wallets & Ledger)](#ب-المحافظ-ودفتر-الأستاذ-wallets--ledger)
    - [محرك التحويلات (Transfer Engine)](#ج-محرك-التحويلات-transfer-engine)
    - [المصارفة وتعدد العملات (Currency Exchange)](#د-المصارفة-وتعدد-العملات-currency-exchange)
4. [تصميم قاعدة البيانات (Database Schema)](#4-تصميم-قاعدة-البيانات)
5. [عقود الواجهة (API Contracts)](#5-عقود-الواجهة-api-contracts)
6. [دليل التطوير وإضافة الميزات (Developer Guide)](#6-دليل-التطوير-وإضافة-الميزات)

---

## 1. نظرة عامة وتقنية

يهدف هذا المشروع لبناء نواة نظام مدفوعات (Payment Gateway Core) يحل المشاكل المالية المعقدة مثل: تضارب العمليات (Race Conditions)، التعامل مع الكسور العشرية بدقة، ومنع الاحتيال عبر ربط الأجهزة.

### 🛠 التقنيات المستخدمة (Tech Stack)
* **اللغة:** Kotlin 1.9 (JVM 21) - للاستفادة من الـ Null Safety والـ Data Classes.
* **الإطار:** Spring Boot 3.2.
* **قاعدة البيانات:** PostgreSQL 16 (بيانات علائقية قوية).
* **الكاش:** Redis (لأسعار الصرف والـ Tokens).
* **الأمان:** Spring Security 6 + JWT + Device Fingerprinting.
* **إدارة البيانات:** Spring Data JPA + Flyway (للـ Migrations).
* **الجودة:** Ktlint (لفرض معايير الكود) + JUnit 5 (للاختبارات).
* **البيئة:** Docker Compose (لتشغيل النظام بضغطة زر).

---

## 2. المعمارية الهندسية

يتبع النظام معمارية **Modular Monolith** مع **Clean Architecture**. تم تقسيم النظام إلى وحدات (Features) مستقلة تماماً، كل وحدة تتكون من 5 طبقات معزولة.

### هيكلية المجلدات (Package Structure)
```text
src/main/kotlin/com/yemenfintech/wallet
├── common/                # (Kernel) أدوات مشتركة لا تعتمد على البزنس
├── features/              # (Modules) الوحدات الأساسية
│   ├── auth/              # تسجيل الدخول والأمان
│   ├── wallet/            # إدارة الأرصدة
│   ├── transaction/       # محرك التحويلات
│   │   ├── domain/        # (Layer 1) الكود النقي (Entities & Business Rules)
│   │   ├── application/   # (Layer 2) حالات الاستخدام (Use Cases)
│   │   ├── infrastructure/# (Layer 3) تنفيذ الداتابيس والمكتبات
│   │   └── presentation/  # (Layer 4) الـ Controllers و DTOs

قاعدة التبعية (Dependency Rule)

Domain ⬅️ Application ⬅️ Infrastructure

    الـ Domain لا يعرف شيئاً عن الـ Framework أو الداتابيس.

    هذا يضمن إمكانية تغيير قاعدة البيانات أو الإطار البرمجي دون كسر منطق العمل المالي.

3. التحليل المالي والهندسي للميزات
أ. إدارة الهوية والأجهزة (Identity & Device Binding)

نظام دخول لا يعتمد فقط على (اسم المستخدم/كلمة المرور)، بل يربط الحساب بالجهاز المادي (Hardware).

    المنظور الأمني: لمنع هجمات (Account Takeover)، يتم تسجيل Device ID عند التسجيل. أي محاولة دخول من جهاز جديد تتطلب إجراءات تحقق إضافية (OTP).

    المنظور الهندسي:

        استخدام JWT يحتوي على device_id في الـ Payload.

        استخدام Filter Chain للتحقق من تطابق جهاز المرسل مع الجهاز المسجل في التوكن قبل وصول الطلب للـ Controller.

ب. المحافظ ودفتر الأستاذ (Wallets & Ledger)

النظام لا يعتمد على عمود balance فقط، بل يستخدم نظام القيد المزدوج (Double-Entry Accounting).

    المنظور المالي:

        فصل العملات: كل عملة لها محفظة مستقلة (لا نخلط الدولار بالريال).

        محفظة النظام: النظام يمتلك محافظ خاصة (LIQUIDITY, FEES) لضمان أن مجموع الأموال في النظام دائماً = 0 (Zero Sum Game).

    المنظور الهندسي:

        جدول wallets للقراءة السريعة (Caching).

        جدول ledger_entries هو "مصدر الحقيقة" (Source of Truth) وهو جدول (Insert Only) لا يتم تعديل سجلاته أبداً.

ج. محرك التحويلات (Transfer Engine)

العمود الفقري للنظام، المسؤول عن نقل الأموال بين طرفين (P2P).

    المنظور المالي:

        التسوية (Settlement): ضمان انتقال الملكية بشكل قانوني ونهائي.

        الرسوم: احتساب الرسوم واقتطاعها في نفس العملية.

    المنظور الهندسي:

        Atomicity: استخدام @Transactional لضمان تنفيذ (الخصم + الإضافة + الرسوم) ككتلة واحدة أو التراجع عنها جميعاً.

        Concurrency: استخدام Pessimistic Locking (SELECT ... FOR UPDATE) لمنع الـ Race Conditions (سحب نفس الرصيد مرتين في نفس اللحظة).

        Idempotency: استخدام مفتاح فريد لكل طلب لمنع تكرار العملية عند انقطاع الشبكة.

د. المصارفة وتعدد العملات (Currency Exchange)

    المنظور المالي:

        مخاطر السعر: السعر يتغير لحظياً. نستخدم آلية "تثبيت السعر" (Quote & Lock) لمدة 30 ثانية.

    المنظور الهندسي:

        العملية تولد قيدين منفصلين في الـ Ledger (خصم من عملة A، إضافة لعملة B).

        استخدام BigDecimal دائماً لتجنب أخطاء التقريب في الكسور العشرية (Floating Point Errors).

4. تصميم قاعدة البيانات
مخطط العلاقات (ER Diagram Overview)
1. Users & Security
SQL

CREATE TABLE users (
    id UUID PRIMARY KEY,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    alternative_account_number VARCHAR(20) UNIQUE NOT NULL, -- رقم حساب داخلي
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_devices (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    device_id VARCHAR(255) NOT NULL, -- Hardware Fingerprint
    is_trusted BOOLEAN DEFAULT TRUE,
    UNIQUE(user_id, device_id)
);

2. Financial Core
SQL

CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    currency_code VARCHAR(3) NOT NULL, -- YER, SAR, USD
    balance DECIMAL(19, 4) DEFAULT 0.0000,
    version BIGINT DEFAULT 0, -- Optimistic Lock Version
    UNIQUE(user_id, currency_code)
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    reference_id VARCHAR(50) UNIQUE, -- Transaction Ref Number
    type VARCHAR(50), -- TRANSFER, EXCHANGE, BILL_PAY
    status VARCHAR(20), -- INITIATED, COMPLETED, FAILED, REVERSED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID REFERENCES transactions(id),
    wallet_id UUID REFERENCES wallets(id),
    amount DECIMAL(19, 4) NOT NULL, -- (+ Credit) or (- Debit)
    balance_after DECIMAL(19, 4) NOT NULL,
    type VARCHAR(20) -- DEBIT / CREDIT
);

5. عقود الواجهة (API Contracts)
🔄 طلب تحويل (P2P Transfer)

POST /api/v1/transfer

Request:
JSON

{
  "recipient_account": "192967789",
  "amount": "5000.00", // String to preserve precision
  "currency": "YER",
  "purpose": "Family Support"
}

Header Required: Idempotency-Key: uuid-v4

Success Response (200 OK):
JSON

{
  "success": true,
  "data": {
    "transaction_id": "tx_12345",
    "status": "COMPLETED",
    "fee_charged": "20.00",
    "timestamp": "2026-02-06T10:00:00Z"
  }
}

💱 طلب مصارفة (Exchange)

POST /api/v1/exchange

Request:
JSON

{
  "from_currency": "SAR",
  "to_currency": "YER",
  "amount_to_sell": "100.00",
  "expected_rate": "139.50" // حماية للمستخدم من تغير السعر المفاجئ
}

6. دليل التطوير وإضافة الميزات

لإضافة ميزة جديدة (مثل: دفع الفواتير)، اتبع الخطوات التالية بدقة للحفاظ على نظافة المعمارية:

    Phase 1: Domain (القلب):

        أنشئ مجلد features/bills/domain.

        عرف الكيانات (Bill) والواجهات (BillRepository interface) فقط. لا تستخدم أي مكتبات هنا.

    Phase 2: Application (المنطق):

        أنشئ مجلد features/bills/application.

        اكتب الـ Service (PayBillService) التي تنفذ المنطق (تحقق من الرصيد -> اخصم -> حدث حالة الفاتورة).

    Phase 3: Infrastructure (التنفيذ):

        أنشئ مجلد features/bills/infrastructure.

        نفذ الـ Repository باستخدام Spring Data JPA.

    Phase 4: Presentation (العرض):

        أنشئ الـ Controller (BillController) والـ DTOs.

    Phase 5: Configuration:

        أضف جداول الداتابيس في ملفات Flyway migration.

        تأكد من إعدادات الأمان (Security Config) للرابط الجديد.

تم التطوير بواسطة: [اسمك] الحالة: MVP (Minimum Viable Product)