package com.fintech.ewallet.bill.application;

import com.fintech.ewallet.bill.application.dto.BillExecuteRequest;
import com.fintech.ewallet.bill.application.dto.BillExecuteResponse;
import com.fintech.ewallet.bill.domain.BillPayment;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillerCategory;
import com.fintech.ewallet.bill.domain.BillRepository;
import com.fintech.ewallet.notification.domain.NotificationSender;
import com.fintech.ewallet.notification.domain.NotificationType;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExecuteBillPaymentUseCase.
 *
 * This test uses Mockito to isolate the use case from the database and the
 * MockBillerService.
 * It ensures that the ledger is recorded, external API is called, and
 * notifications are sent.
 */
@ExtendWith(MockitoExtension.class)
class ExecuteBillPaymentUseCaseTest {

    @Mock
    private BillRepository billRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    @Mock
    private MockBillerService mockBillerService;
    @Mock
    private NotificationSender notificationSender;

    @InjectMocks
    private ExecuteBillPaymentUseCase executeBillPaymentUseCase;

    private UUID userId;
    private Wallet userWallet;
    private Biller biller;
    private BillExecuteRequest request;

    @BeforeEach
    void setUp() {
        // Since @Value("${biller.fee.flat-amount:50}") is a Spring annotation, Mockito
        // won't inject it automatically.
        // We use ReflectionTestUtils to manually set this field for our isolated unit
        // test.
        ReflectionTestUtils.setField(executeBillPaymentUseCase, "flatFeeAmount", new BigDecimal("50"));

        userId = UUID.randomUUID();

        userWallet = new Wallet(userId, Currency.YER);
        // We simulate that the user actually has funds. (Not strictly needed for this
        // test since the ledger does the debiting, but good practice)

        biller = Biller.builder()
                .id(UUID.randomUUID())
                .code("YEMEN_MOBILE")
                .name("Yemen Mobile")
                .category(BillerCategory.TELECOM)
                .supportedCurrency("YER")
                .walletId(UUID.randomUUID())
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();

        request = new BillExecuteRequest("YEMEN_MOBILE", "770000000", new BigDecimal("1000"), "YER");
    }

    // ========================================================================
    // TEST 1: The Happy Path - Successful Bill Payment
    // ========================================================================
    @Test
    @DisplayName("Should execute bill payment successfully")
    void shouldExecuteBillPaymentSuccessfully() {
        // Arrange
        when(billRepository.findBillerByCode("YEMEN_MOBILE")).thenReturn(Optional.of(biller));
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.YER)).thenReturn(Optional.of(userWallet));

        // Ledger entry succeeds
        UUID transactionId = UUID.randomUUID();
        when(recordLedgerEntryUseCase.recordTransferWithFee(
                eq(userWallet.getId()), eq(biller.getWalletId()), eq(new BigDecimal("1000")),
                any(), eq(new BigDecimal("50")), any(), any(), anyString(), anyString()))
                .thenReturn(transactionId);

        // External Biller API succeeds
        when(mockBillerService.processPayment("YEMEN_MOBILE", "770000000", new BigDecimal("1000")))
                .thenReturn(true);

        // Act
        BillExecuteResponse response = executeBillPaymentUseCase.execute(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals("Yemen Mobile", response.billerName());
        assertEquals(new BigDecimal("1000"), response.amount());
        assertEquals(new BigDecimal("50"), response.feeAmount());
        assertEquals(new BigDecimal("1050"), response.totalDeducted()); // 1000 + 50
        assertEquals("COMPLETED", response.status());

        // Verify the interactions
        verify(billRepository, times(1)).savePayment(any(BillPayment.class));
        verify(notificationSender, times(1)).send(eq(userId), eq(NotificationType.BILL_PAYMENT_COMPLETED), anyString(),
                anyString(), anyString(), any());
    }

    // ========================================================================
    // TEST 2: Inactive Biller Rejection
    // ========================================================================
    @Test
    @DisplayName("Should reject payment if biller is inactive")
    void shouldRejectIfBillerIsInactive() {
        // Arrange
        biller.setStatus("INACTIVE");
        when(billRepository.findBillerByCode("YEMEN_MOBILE")).thenReturn(Optional.of(biller));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executeBillPaymentUseCase.execute(userId, request));

        assertEquals("Biller is currently inactive", ex.getMessage());

        // Verify ledger and external API were NEVER called
        verify(recordLedgerEntryUseCase, never()).recordTransferWithFee(any(), any(), any(), any(), any(), any(), any(),
                any(), any());
        verify(mockBillerService, never()).processPayment(anyString(), anyString(), any());
    }

    // ========================================================================
    // TEST 3: External API Failure (Rollback Simulation)
    // ========================================================================
    @Test
    @DisplayName("Should throw exception if external biller API fails")
    void shouldThrowExceptionIfExternalBillerFails() {
        // Arrange
        when(billRepository.findBillerByCode("YEMEN_MOBILE")).thenReturn(Optional.of(biller));
        when(walletRepository.findByUserIdAndCurrency(userId, Currency.YER)).thenReturn(Optional.of(userWallet));

        // Ledger passes
        when(recordLedgerEntryUseCase.recordTransferWithFee(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        // But External API FAILS!
        when(mockBillerService.processPayment("YEMEN_MOBILE", "770000000", new BigDecimal("1000")))
                .thenReturn(false);

        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> executeBillPaymentUseCase.execute(userId, request));

        assertEquals("External biller rejected the payment. Your money has not been deducted.", ex.getMessage());

        // Verify no payment was saved to DB and no notification was sent
        verify(billRepository, never()).savePayment(any());
        verify(notificationSender, never()).send(any(), any(), anyString(), anyString(), anyString(), any());
    }

    // ========================================================================
    // TEST 4: Wrong Currency
    // ========================================================================
    @Test
    @DisplayName("Should reject payment if user tries to pay with wrong currency")
    void shouldRejectWrongCurrency() {
        // Arrange
        when(billRepository.findBillerByCode("YEMEN_MOBILE")).thenReturn(Optional.of(biller));

        // Request uses USD, but Biller only supports YER
        BillExecuteRequest wrongCurrencyRequest = new BillExecuteRequest("YEMEN_MOBILE", "770000000",
                new BigDecimal("1000"), "USD");

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executeBillPaymentUseCase.execute(userId, wrongCurrencyRequest));

        assertEquals("Biller only supports YER", ex.getMessage());
    }
}
