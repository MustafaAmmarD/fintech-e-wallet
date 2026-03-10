# Phase 6: Automated Testing

> **Goal**: Write automated tests that verify our entire e-wallet backend works correctly — before every deployment, forever.
> **Estimated Duration**: Weeks 16–17
> **Prerequisite**: You should understand Java, Spring Boot, and our project structure (Phases 1–5).

---

## Phase 6 Overview

| Step | Title                      | What You'll Learn                                   | Status  |
| ---- | -------------------------- | --------------------------------------------------- | ------- |
| 6.1  | What Is Testing & Why?     | The mental model behind automated testing           | ✅ Done |
| 6.2  | Your First Unit Test       | JUnit 5, testing a pure utility class               | ✅ Done |
| 6.3  | Service Layer Unit Tests   | Mockito, testing use cases with mocked dependencies | ✅ Done |
| 6.4  | Integration Tests          | Testing with a real database (Testcontainers)       | ✅ Done |
| 6.5  | Controller Tests (API)     | Testing REST endpoints with MockMvc                 | ✅ Done |
| 6.6  | Running All Tests Together | Maven test command, CI pipeline readiness           | ✅ Done |

---

## 6.1 What Is Testing & Why?

### The Problem: "It Works on My Machine"

Imagine this scenario:

```
Day 1: You build the transfer feature. You test it manually in Swagger. It works! ✅
Day 2: You add bill payments. You test bills in Swagger. It works! ✅
Day 3: You realize bill payments accidentally broke transfers. 😱
       But you didn't notice because you didn't re-test transfers!
```

This is called a **regression** — a bug introduced by new code that breaks something that used to work.

### The Solution: Automated Tests

Instead of manually testing in Swagger every time, you write **code that tests your code**:

```java
@Test
void transferShouldDeductFromSenderAndCreditRecipient() {
    // Setup: Ahmed has 10,000 YER, Sara has 0 YER
    // Action: Ahmed transfers 1,000 YER to Sara
    // Verify: Ahmed now has 8,980 YER (1000 + 20 fee), Sara has 1,000 YER
}
```

Now every time you change _anything_ in the project, you run one command:

```bash
.\mvnw.cmd test
```

And in 30 seconds, the computer checks **every single feature** still works. If something broke, it tells you exactly what and where.

### Real-World Analogy

Think of tests like a **checklist for a pilot before takeoff**:

- ✅ Fuel level OK?
- ✅ Landing gear working?
- ✅ Engines responding?
- ✅ Navigation system OK?

The pilot doesn't skip this checklist thinking "it worked yesterday." They verify **every single time**. That's what automated tests do for your code.

### Types of Tests (The Testing Pyramid)

```
         ╱╲
        ╱  ╲         🔺 E2E (End-to-End) Tests
       ╱    ╲           Full browser testing. Slowest. We skip this for now.
      ╱──────╲
     ╱        ╲       🔶 Integration Tests
    ╱          ╲         Test multiple components together with a real database.
   ╱────────────╲        Example: "Can I actually save a transfer to Postgres?"
  ╱              ╲
 ╱                ╲    🟢 Unit Tests
╱──────────────────╲      Test ONE class in isolation. Fastest. Most numerous.
                           Example: "Does CalculateFeeUseCase return 50 YER?"
```

**We will write from bottom to top:**

1. First: **Unit Tests** (fast, test logic in isolation)
2. Then: **Integration Tests** (slower, test with real database)
3. Then: **Controller Tests** (test the HTTP API layer)

---

## 6.2 Your First Unit Test — Concepts

### What Is JUnit 5?

JUnit is the #1 testing framework for Java. It's already included in your Spring Boot project (via `spring-boot-starter-test`). You don't need to install anything.

A JUnit test is just a Java method with `@Test` on top:

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @Test
    void addingTwoNumbersShouldReturnTheirSum() {
        // Arrange — set up the inputs
        int a = 2;
        int b = 3;

        // Act — call the method you're testing
        int result = a + b;

        // Assert — verify the result is what you expected
        assertEquals(5, result);  // If result != 5, the test FAILS
    }
}
```

### The AAA Pattern (Arrange → Act → Assert)

Every test follows this pattern:

| Step        | What It Does                        | Example                         |
| ----------- | ----------------------------------- | ------------------------------- |
| **Arrange** | Set up the test data and conditions | Create a wallet with 10,000 YER |
| **Act**     | Call the method you want to test    | Execute a 1,000 YER transfer    |
| **Assert**  | Verify the outcome is correct       | Check wallet balance is 8,980   |

### What Is Mockito?

When you test `ExecuteTransferUseCase`, it depends on:

- `WalletRepository` (database access)
- `UserRepository` (database access)
- `RecordLedgerEntryUseCase` (another service)
- `CalculateFeeUseCase` (another service)

In a **unit test**, you don't want to connect to a real database. That's slow and fragile. Instead, you create **fake versions** of these dependencies called **mocks**:

```java
@ExtendWith(MockitoExtension.class)
class ExecuteTransferUseCaseTest {

    @Mock  // This creates a FAKE WalletRepository (not connected to any database)
    private WalletRepository walletRepository;

    @Mock  // Fake UserRepository
    private UserRepository userRepository;

    @InjectMocks  // Creates the REAL use case, but injects the fake dependencies
    private ExecuteTransferUseCase useCase;

    @Test
    void shouldRejectTransferWhenInsufficientBalance() {
        // Arrange: tell the mock what to return when called
        when(walletRepository.findByUserIdAndCurrency(any(), any()))
            .thenReturn(Optional.of(walletWith500YER));

        // Act & Assert: trying to transfer 1000 should throw an exception
        assertThrows(IllegalArgumentException.class, () -> {
            useCase.execute(userId, transferRequest(1000));
        });
    }
}
```

**Think of mocks like this:** Instead of calling a real bank to check a balance, you put a sticky note that says "balance = 500 YER." The test reads that sticky note instead of contacting the real bank.

### Where Do Tests Live?

```
src/
├── main/java/           ← Your production code (what we built in Phases 1-5)
│   └── com/fintech/ewallet/
│       ├── wallet/
│       ├── bill/
│       └── ...
└── test/java/           ← Your test code (what we will build in Phase 6)
    └── com/fintech/ewallet/
        ├── wallet/
        │   └── application/
        │       ├── CalculateFeeUseCaseTest.java
        │       ├── ExecuteTransferUseCaseTest.java
        │       └── RecordLedgerEntryUseCaseTest.java
        ├── bill/
        │   └── application/
        │       ├── ExecuteBillPaymentUseCaseTest.java
        │       └── PreviewBillPaymentUseCaseTest.java
        └── ...
```

The test directory mirrors the main directory exactly. Each test class tests one production class.

### How to Run Tests

```bash
# Run ALL tests
.\mvnw.cmd test

# Run a specific test class
.\mvnw.cmd test -Dtest=CalculateFeeUseCaseTest

# Run a specific test method
.\mvnw.cmd test -Dtest=CalculateFeeUseCaseTest#shouldReturn50YERForBillPayment
```

---

## 6.3 Service Layer Unit Tests — Plan

These are the most important tests. They verify our business logic works correctly.

### What We Will Test

| Test Class                      | What It Verifies                                          |
| ------------------------------- | --------------------------------------------------------- |
| `CalculateFeeUseCaseTest`       | Fee calculation: flat fees, percentage fees, min/max caps |
| `ExecuteTransferUseCaseTest`    | Transfer logic: balance checks, self-transfer prevention  |
| `PreviewBillPaymentUseCaseTest` | Bill preview: fee calculation, balance validation         |
| `ExecuteBillPaymentUseCaseTest` | Bill execution: wallet debits, biller credits, status     |
| `RecordLedgerEntryUseCaseTest`  | Ledger: zero-sum validation, correct entry creation       |
| `AccountNumberGeneratorTest`    | Luhn algorithm: valid/invalid number generation           |

### Example: Testing the Fee Calculator

```java
@ExtendWith(MockitoExtension.class)
class CalculateFeeUseCaseTest {

    @Mock
    private FeeRuleRepository feeRuleRepository;

    @InjectMocks
    private CalculateFeeUseCase calculateFeeUseCase;

    @Test
    @DisplayName("Bill payment should always charge flat 50 YER fee")
    void billPaymentShouldChargeFlatFee() {
        // Arrange
        FeeRule billFeeRule = new FeeRule(
            FeeOperation.BILL_PAYMENT, "YER",
            FeeType.FLAT, new BigDecimal("50"), null, null
        );
        when(feeRuleRepository.findByOperationAndCurrency(FeeOperation.BILL_PAYMENT, "YER"))
            .thenReturn(Optional.of(billFeeRule));

        // Act
        BigDecimal fee = calculateFeeUseCase.execute(
            FeeOperation.BILL_PAYMENT, "YER", new BigDecimal("2500")
        );

        // Assert
        assertEquals(new BigDecimal("50"), fee);
    }

    @Test
    @DisplayName("Transfer fee should be 2% with min 1 and max 500")
    void transferFeeShouldBePercentageWithCaps() {
        // ... similar test for transfer fees
    }
}
```

---

## 6.4 Integration Tests — Concepts

> [!IMPORTANT]
> Integration tests verify that your code works with a **real PostgreSQL database** — not a fake one.

### What Is Testcontainers?

Testcontainers is a Java library that **automatically starts a Docker container** (in our case, PostgreSQL) just for your tests. When the test finishes, the container is destroyed.

```
Test starts → Testcontainers spins up a fresh PostgreSQL → Flyway runs migrations →
Your test inserts data → Verifies queries work → Container is destroyed
```

**Why is this amazing?**

- You don't need to set up a separate test database
- Every test run starts with a **completely clean database**
- It tests the REAL SQL queries, not fake ones

### Example: Testing the Wallet Repository

```java
@SpringBootTest
@Testcontainers
class WalletRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("ewallet_test");

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void shouldFindWalletByUserIdAndCurrency() {
        // This test runs against a REAL PostgreSQL database!
        Optional<Wallet> wallet = walletRepository.findByUserIdAndCurrency(userId, "YER");
        assertTrue(wallet.isPresent());
        assertEquals("YER", wallet.get().getCurrency());
    }
}
```

---

## 6.5 Controller Tests (API Layer) — Concepts

> [!NOTE]
> Controller tests verify that your REST API returns the correct HTTP status codes, JSON structure, and handles authentication properly.

### What Is MockMvc?

`MockMvc` simulates HTTP requests without starting the server:

```java
@WebMvcTest(BillPaymentController.class)
class BillPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecuteBillPaymentUseCase executeBillPaymentUseCase;

    @Test
    @WithMockUser(roles = "USER")
    void executeBillPaymentShouldReturn200() throws Exception {
        // Arrange
        when(executeBillPaymentUseCase.execute(any(), any()))
            .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bills/execute")
                .header("Idempotency-Key", "test-key-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "billerCode": "YEMEN_MOBILE",
                        "customerAccountNumber": "771234567",
                        "amount": 2500.00,
                        "currency": "YER"
                    }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/bills/execute"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## 6.6 Running All Tests Together

Once all tests are written, a single command runs everything:

```bash
.\mvnw.cmd test
```

Output will look like:

```
[INFO] Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

If ANY test fails, the build fails — meaning you **cannot deploy** broken code.

---

## Completed Test Results

### Step 1: `AccountNumberGeneratorTest` — Pure Unit Test ✅

> **File:** `src/test/java/com/fintech/ewallet/shared/util/AccountNumberGeneratorTest.java`

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.101 s
```

| #   | Test Name                                 | What It Verifies                                     |
| --- | ----------------------------------------- | ---------------------------------------------------- |
| 1   | `generatedNumberShouldBeNineDigits`       | Generated numbers are exactly 9 digits               |
| 2   | `generatedNumberShouldPassLuhnCheck`      | Generated numbers pass Luhn validation               |
| 3   | `hundredGeneratedNumbersShouldAllBeValid` | 100 random numbers all pass validation (stress test) |
| 4   | `nullShouldBeInvalid`                     | `null` is rejected                                   |
| 5   | `emptyStringShouldBeInvalid`              | `""` is rejected                                     |
| 6   | `wrongLengthShouldBeInvalid`              | 8-digit and 10-digit numbers are rejected            |
| 7   | `nonNumericShouldBeInvalid`               | Letters and special characters are rejected          |
| 8   | `corruptedLastDigitShouldBeInvalid`       | Corrupting the check digit makes it invalid          |

**Test Type:** Pure unit test — no Spring Boot, no mocks, no database.

---

### Step 2: `CalculateFeeUseCaseTest` — Unit Test with Mockito ✅

> **File:** `src/test/java/com/fintech/ewallet/fee/application/CalculateFeeUseCaseTest.java`

```
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.893 s
```

| #   | Test Name                                  | What It Verifies                                      |
| --- | ------------------------------------------ | ----------------------------------------------------- |
| 1   | `shouldApplyFlatFeeFromDatabaseRule`       | Flat fee rule from DB overrides defaults              |
| 2   | `shouldUseFallbackPercentageForTransfer`   | 2% fallback when no DB rule exists                    |
| 3   | `transferFallbackFeeShouldRespectMinimum`  | Minimum 1 YER fee is enforced                         |
| 4   | `transferFallbackFeeShouldRespectMaximum`  | Maximum 500 YER fee is enforced                       |
| 5   | `depositShouldHaveZeroFeeByDefault`        | Deposits are free                                     |
| 6   | `withdrawalShouldHaveZeroFeeByDefault`     | Withdrawals are free                                  |
| 7   | `nullOperationShouldThrowException`        | Null operation type throws `IllegalArgumentException` |
| 8   | `zeroAmountShouldThrowException`           | Zero/negative amount throws exception                 |
| 9   | `shouldApplyPercentageFeeFromDatabaseRule` | 1.5% rule with min/max from DB works                  |

**Test Type:** Mockito unit test — `FeeRuleRepository` is mocked (no database).

---

### Step 3: `ExecuteTransferUseCaseTest` — Unit Test with Mockito ✅

> **File:** `src/test/java/com/fintech/ewallet/wallet/application/ExecuteTransferUseCaseTest.java`

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.123 s
```

| #   | Test Name                            | What It Verifies                                                 |
| --- | ------------------------------------ | ---------------------------------------------------------------- |
| 1   | `shouldExecuteTransferSuccessfully`  | Full happy path: fee calculated, ledger recorded, transfer saved |
| 2   | `shouldRejectSelfTransfer`           | Cannot send money to yourself                                    |
| 3   | `shouldRejectIfSenderWalletNotFound` | Rejects if sender has no wallet in that currency                 |
| 4   | `shouldRejectIfSenderWalletIsFrozen` | Rejects if wallet status is FROZEN                               |
| 5   | `shouldRejectIfSystemWalletUsed`     | System fee wallets cannot be used for P2P                        |

**Test Type:** Mockito unit test — 6 dependencies mocked (UserRepo, WalletRepo, LedgerUseCase, TransferRepo, FeeUseCase, NameMaskingService).

---

### Step 4: `ExecuteBillPaymentUseCaseTest` — Unit Test with Mockito ✅

> **File:** `src/test/java/com/fintech/ewallet/bill/application/ExecuteBillPaymentUseCaseTest.java`

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.428 s
```

| #   | Test Name                                   | What It Verifies                                         |
| --- | ------------------------------------------- | -------------------------------------------------------- |
| 1   | `shouldExecuteBillPaymentSuccessfully`      | Full happy path: ledger, biller API, notification sent   |
| 2   | `shouldRejectIfBillerIsInactive`            | Inactive billers are immediately rejected                |
| 3   | `shouldThrowExceptionIfExternalBillerFails` | External API failure = no payment saved, no notification |
| 4   | `shouldRejectWrongCurrency`                 | Cannot pay YER-only biller with USD                      |

**Test Type:** Mockito unit test — 5 dependencies mocked. Used `ReflectionTestUtils` to inject the `@Value` fee amount.

---

### Cumulative Test Summary (Steps 1–4)

| Test Class                      | Tests  | Time      | Type         |
| ------------------------------- | ------ | --------- | ------------ |
| `AccountNumberGeneratorTest`    | 8      | 0.101s    | Pure Unit    |
| `CalculateFeeUseCaseTest`       | 9      | 0.893s    | Mockito Unit |
| `ExecuteTransferUseCaseTest`    | 5      | 1.123s    | Mockito Unit |
| `ExecuteBillPaymentUseCaseTest` | 4      | 1.428s    | Mockito Unit |
| **Total**                       | **26** | **~3.5s** |              |

---

### Step 5: `WalletJpaRepositoryIntegrationTest` — Integration Test ✅

> **File:** `src/test/java/com/fintech/ewallet/wallet/infrastructure/persistence/WalletJpaRepositoryIntegrationTest.java`

```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.439 s
```

| #   | Test Name                                | What It Verifies                                                  |
| --- | ---------------------------------------- | ----------------------------------------------------------------- |
| 1   | `shouldSaveAndFindById`                  | Save a wallet to REAL PostgreSQL and retrieve by ID               |
| 2   | `shouldFindByUserIdAndCurrency`          | Find the correct wallet when user has multiple currencies         |
| 3   | `shouldFindAllWalletsByUserId`           | Returns only the target user's wallets, not other users'          |
| 4   | `existsShouldReturnTrueWhenWalletExists` | Boolean existence check works for both present and absent wallets |
| 5   | `shouldReturnEmptyWhenNotFound`          | Returns `Optional.empty()` for non-existent wallets               |

**Test Type:** Integration test — uses real PostgreSQL database (from Docker Compose). Uses `@DataJpaTest` (loads only JPA layer, not full app). Creates `UserJpaEntity` first due to foreign key constraints.

### Cumulative Test Summary (Steps 1–5)

| Test Class                           | Tests  | Time    | Type         |
| ------------------------------------ | ------ | ------- | ------------ |
| `AccountNumberGeneratorTest`         | 8      | 0.101s  | Pure Unit    |
| `CalculateFeeUseCaseTest`            | 9      | 0.893s  | Mockito Unit |
| `ExecuteTransferUseCaseTest`         | 5      | 1.123s  | Mockito Unit |
| `ExecuteBillPaymentUseCaseTest`      | 4      | 1.428s  | Mockito Unit |
| `WalletJpaRepositoryIntegrationTest` | 5      | 5.439s  | Integration  |
| **Total**                            | **31** | **~9s** |              |

---

### Step 6: `BillPaymentControllerTest` — Controller Test (MockMvc) ✅

> **File:** `src/test/java/com/fintech/ewallet/bill/api/BillPaymentControllerTest.java`

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.95 s
```

| #   | Test Name                                           | What It Verifies                                                                    |
| --- | --------------------------------------------------- | ----------------------------------------------------------------------------------- |
| 1   | `getBillersShouldReturn200`                         | GET /billers returns 200 with JSON list of billers                                  |
| 2   | `executeBillPaymentShouldReturn200`                 | POST /execute returns 200 with correct JSON fields (status, amount, fee, reference) |
| 3   | `executeBillPaymentWithoutIdempotencyKeyShouldFail` | POST /execute without required `Idempotency-Key` header returns 400                 |
| 4   | `shouldReturn403WhenNotAuthenticated`               | Unauthenticated user gets 403 Forbidden on protected endpoint                       |

**Test Type:** Controller test — uses `@SpringBootTest` + `@AutoConfigureMockMvc` + `@TestConfiguration` with `@Primary` Mockito beans. Simulates real HTTP requests without starting a web server.

### Cumulative Test Summary (Steps 1–6)

| Test Class                           | Tests  | Time     | Type         |
| ------------------------------------ | ------ | -------- | ------------ |
| `AccountNumberGeneratorTest`         | 8      | 0.101s   | Pure Unit    |
| `CalculateFeeUseCaseTest`            | 9      | 0.893s   | Mockito Unit |
| `ExecuteTransferUseCaseTest`         | 5      | 1.123s   | Mockito Unit |
| `ExecuteBillPaymentUseCaseTest`      | 4      | 1.428s   | Mockito Unit |
| `WalletJpaRepositoryIntegrationTest` | 5      | 5.439s   | Integration  |
| `BillPaymentControllerTest`          | 4      | 11.95s   | Controller   |
| **Total**                            | **35** | **~21s** |              |

---

## Key Vocabulary Summary

| Term               | What It Means                                                    |
| ------------------ | ---------------------------------------------------------------- |
| **JUnit 5**        | The testing framework (like Spring Boot is the app framework)    |
| **@Test**          | Annotation that marks a method as a test                         |
| **Assert**         | Check if a result is correct (`assertEquals`, `assertTrue`, etc) |
| **Mock**           | A fake object that pretends to be a real dependency              |
| **Mockito**        | The library that creates mocks                                   |
| **@Mock**          | Creates a fake version of a class                                |
| **@InjectMocks**   | Creates the real class but injects the fakes into it             |
| **Testcontainers** | Spins up a real Docker database just for testing                 |
| **MockMvc**        | Simulates HTTP requests to test controllers                      |
| **Regression**     | A bug caused by new code that breaks old functionality           |
| **AAA Pattern**    | Arrange → Act → Assert (the structure of every test)             |
| **Test Pyramid**   | Many unit tests, fewer integration tests, fewest E2E tests       |

---

## Dependencies We Need

These go in `pom.xml` (most are already included by Spring Boot):

```xml
<!-- Already included by spring-boot-starter-test -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <!-- Includes: JUnit 5, Mockito, AssertJ, MockMvc, Spring Test -->
</dependency>

<!-- NEW: For real database testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.5</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.5</version>
    <scope>test</scope>
</dependency>
```
