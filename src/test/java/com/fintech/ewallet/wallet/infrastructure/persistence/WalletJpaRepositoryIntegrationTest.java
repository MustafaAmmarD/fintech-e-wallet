package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.identity.infrastructure.persistence.UserJpaEntity;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for WalletJpaRepository.
 *
 * KEY DIFFERENCE FROM UNIT TESTS:
 * - Unit tests (Steps 1-4): Used Mockito fakes. No database. Ran in ~1 second.
 * - This integration test: Connects to the REAL PostgreSQL database running in
 * Docker!
 * Flyway runs all migrations, then we insert and query REAL data.
 *
 * This test verifies that our JPA queries, column mappings, and SQL
 * all work correctly against a real PostgreSQL — not just in theory.
 *
 * Since the "wallets" table has a FOREIGN KEY to "users", we must create
 * a User record first using EntityManager, then create wallets for that user.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("integration")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/ewallet_dev",
        "spring.datasource.username=ewallet",
        "spring.datasource.password=ewallet_dev_password",
        "spring.docker.compose.enabled=false",
        "spring.flyway.enabled=true"
})
class WalletJpaRepositoryIntegrationTest {

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID testUserId;

    /**
     * Before each test: create a fresh test user in the "users" table.
     * This is required because the "wallets.user_id" column has a FOREIGN KEY
     * constraint.
     */
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        Instant now = Instant.now();

        // Create a test user directly via EntityManager (bypasses service layer)
        UserJpaEntity testUser = UserJpaEntity.builder()
                .id(testUserId)
                .phoneNumber("+967" + UUID.randomUUID().toString().substring(0, 9))
                .fullName("Test User")
                .passwordHash("$2a$10$test_hash_not_real")
                .email("test_" + testUserId + "@example.com")
                .kycStatus("NONE")
                .accountStatus("ACTIVE")
                .role("USER")
                .language("en")
                .referralCode("TEST" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .accountNumber(UUID.randomUUID().toString().substring(0, 9))
                .showFullName(true)
                .failedLoginAttempts(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        entityManager.persist(testUser);
        entityManager.flush();
    }

    // ========================================================================
    // TEST 1: Save and retrieve a wallet from the REAL database
    // ========================================================================
    @Test
    @DisplayName("Should save a wallet to the REAL database and retrieve it by ID")
    void shouldSaveAndFindById() {
        // Arrange
        WalletJpaEntity wallet = new WalletJpaEntity(
                UUID.randomUUID(), testUserId, Currency.YER,
                new BigDecimal("10000.0000"), WalletStatus.ACTIVE,
                Instant.now(), Instant.now());

        // Act: save to the REAL PostgreSQL
        walletJpaRepository.save(wallet);

        // Assert: retrieve it back
        Optional<WalletJpaEntity> found = walletJpaRepository.findById(wallet.getId());
        assertTrue(found.isPresent(), "Wallet should be found in the database");
        assertEquals(Currency.YER, found.get().getCurrency());
        assertEquals(0, new BigDecimal("10000.0000").compareTo(found.get().getBalance()));
        assertEquals(WalletStatus.ACTIVE, found.get().getStatus());
    }

    // ========================================================================
    // TEST 2: Find wallet by userId AND currency
    // ========================================================================
    @Test
    @DisplayName("Should find wallet by userId and currency")
    void shouldFindByUserIdAndCurrency() {
        // Arrange: create YER and USD wallets
        WalletJpaEntity yerWallet = new WalletJpaEntity(
                UUID.randomUUID(), testUserId, Currency.YER,
                new BigDecimal("5000.0000"), WalletStatus.ACTIVE,
                Instant.now(), Instant.now());
        WalletJpaEntity usdWallet = new WalletJpaEntity(
                UUID.randomUUID(), testUserId, Currency.USD,
                new BigDecimal("100.0000"), WalletStatus.ACTIVE,
                Instant.now(), Instant.now());
        walletJpaRepository.saveAll(List.of(yerWallet, usdWallet));

        // Act: search for the YER wallet only
        Optional<WalletJpaEntity> found = walletJpaRepository.findByUserIdAndCurrency(testUserId, Currency.YER);

        // Assert
        assertTrue(found.isPresent());
        assertEquals(Currency.YER, found.get().getCurrency());
        assertEquals(yerWallet.getId(), found.get().getId());
    }

    // ========================================================================
    // TEST 3: Find all wallets for a user
    // ========================================================================
    @Test
    @DisplayName("Should find all wallets belonging to a specific user")
    void shouldFindAllWalletsByUserId() {
        // Arrange: a second user for comparison
        UUID otherUserId = UUID.randomUUID();
        UserJpaEntity otherUser = UserJpaEntity.builder()
                .id(otherUserId)
                .phoneNumber("+967" + UUID.randomUUID().toString().substring(0, 9))
                .fullName("Other User")
                .passwordHash("$2a$10$test_hash")
                .kycStatus("NONE").accountStatus("ACTIVE").role("USER").language("en")
                .accountNumber(UUID.randomUUID().toString().substring(0, 9))
                .showFullName(true).failedLoginAttempts(0)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        entityManager.persist(otherUser);
        entityManager.flush();

        WalletJpaEntity w1 = new WalletJpaEntity(UUID.randomUUID(), testUserId, Currency.YER,
                BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());
        WalletJpaEntity w2 = new WalletJpaEntity(UUID.randomUUID(), testUserId, Currency.USD,
                BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());
        WalletJpaEntity w3 = new WalletJpaEntity(UUID.randomUUID(), otherUserId, Currency.YER,
                BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());

        walletJpaRepository.saveAll(List.of(w1, w2, w3));

        // Act
        List<WalletJpaEntity> userWallets = walletJpaRepository.findByUserId(testUserId);

        // Assert: only 2 wallets for testUserId (not the other user's)
        assertEquals(2, userWallets.size());
    }

    // ========================================================================
    // TEST 4: existsByUserIdAndCurrency
    // ========================================================================
    @Test
    @DisplayName("existsByUserIdAndCurrency should return true when wallet exists")
    void existsShouldReturnTrueWhenWalletExists() {
        walletJpaRepository.save(new WalletJpaEntity(UUID.randomUUID(), testUserId, Currency.YER,
                BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now()));

        assertTrue(walletJpaRepository.existsByUserIdAndCurrency(testUserId, Currency.YER));
        assertFalse(walletJpaRepository.existsByUserIdAndCurrency(testUserId, Currency.USD));
    }

    // ========================================================================
    // TEST 5: Empty result when no wallet found
    // ========================================================================
    @Test
    @DisplayName("Should return empty Optional when wallet does not exist")
    void shouldReturnEmptyWhenNotFound() {
        Optional<WalletJpaEntity> result = walletJpaRepository.findByUserIdAndCurrency(
                UUID.randomUUID(), Currency.YER);
        assertTrue(result.isEmpty());
    }
}
