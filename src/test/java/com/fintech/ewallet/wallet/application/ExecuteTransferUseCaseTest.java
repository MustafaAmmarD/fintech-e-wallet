package com.fintech.ewallet.wallet.application;

import com.fintech.ewallet.fee.application.CalculateFeeUseCase;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.privacy.NameMaskingService;
import com.fintech.ewallet.wallet.application.dto.ExecuteTransferRequest;
import com.fintech.ewallet.wallet.application.dto.ExecuteTransferResponse;
import com.fintech.ewallet.wallet.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExecuteTransferUseCase.
 *
 * This use case is complex because it depends on 6 different repositories and
 * services.
 * We mock all of them to test the transfer core logic in complete isolation!
 */
@ExtendWith(MockitoExtension.class)
class ExecuteTransferUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private CalculateFeeUseCase calculateFeeUseCase;
    @Mock
    private NameMaskingService nameMaskingService;

    @InjectMocks
    private ExecuteTransferUseCase executeTransferUseCase;

    // Test Data
    private UUID senderId;
    private UUID recipientId;
    private User sender;
    private User recipient;
    private Wallet senderWallet;
    private Wallet recipientWallet;
    private ExecuteTransferRequest request;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        senderId = UUID.randomUUID();
        recipientId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        sender = new User();
        sender.setId(senderId);
        sender.setFullName("Sender");
        sender.setPhoneNumber("+967770000001");
        sender.setAccountNumber("111111111");

        recipient = new User();
        recipient.setId(recipientId);
        recipient.setFullName("Recipient");
        recipient.setPhoneNumber("+967770000002");
        recipient.setAccountNumber("222222222");

        senderWallet = new Wallet(senderId, Currency.YER);
        recipientWallet = new Wallet(recipientId, Currency.YER);

        request = new ExecuteTransferRequest("222222222", new BigDecimal("1000"), Currency.YER, "Test transfer");
    }

    // ========================================================================
    // TEST 1: The Happy Path - Successful Transfer
    // ========================================================================
    @Test
    @DisplayName("Should successfully execute transfer and return response")
    void shouldExecuteTransferSuccessfully() {
        // Arrange
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByAccountNumber(request.recipientAccountNumber())).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserIdAndCurrency(senderId, Currency.YER)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdAndCurrency(recipientId, Currency.YER))
                .thenReturn(Optional.of(recipientWallet));

        // Fee returns 20 YER
        BigDecimal feeAmount = new BigDecimal("20");
        when(calculateFeeUseCase.execute(any(), eq(Currency.YER), eq(new BigDecimal("1000")))).thenReturn(feeAmount);

        // Ledger entry returns a transaction ID
        when(recordLedgerEntryUseCase.recordTransferWithFee(
                eq(senderWallet.getId()), eq(recipientWallet.getId()), eq(new BigDecimal("1000")),
                any(), eq(feeAmount), any(), any(), eq("Test transfer")))
                .thenReturn(transactionId);

        // Transfer saving Mock
        P2PTransfer savedTransfer = new P2PTransfer(
                senderId, senderWallet.getId(), recipientId, recipientWallet.getId(),
                new BigDecimal("1000"), feeAmount, Currency.YER, "Test transfer", transactionId);

        when(transferRepository.save(any(P2PTransfer.class))).thenReturn(savedTransfer);
        when(nameMaskingService.getDisplayName(recipient, senderId)).thenReturn("R***");

        // Act
        ExecuteTransferResponse response = executeTransferUseCase.execute(senderId, request);

        // Assert
        assertNotNull(response);
        assertEquals(new BigDecimal("1000"), response.amount());
        assertEquals(new BigDecimal("20"), response.feeAmount());
        assertEquals(new BigDecimal("1020"), response.totalDeducted());
        assertEquals("R***", response.recipientDisplayName());

        // Verify that the ledger and repository were actually called exactly once
        verify(recordLedgerEntryUseCase, times(1)).recordTransferWithFee(any(), any(), any(), any(), any(), any(),
                any(), any());
        verify(transferRepository, times(1)).save(any(P2PTransfer.class));
    }

    // ========================================================================
    // TEST 2: Self Transfer is Rejected
    // ========================================================================
    @Test
    @DisplayName("Should reject transfer if sender and recipient are the same")
    void shouldRejectSelfTransfer() {
        // Arrange
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        // Recipient lookup returns the SAME user
        when(userRepository.findByAccountNumber(request.recipientAccountNumber())).thenReturn(Optional.of(sender));

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executeTransferUseCase.execute(senderId, request));

        assertEquals("Cannot transfer to yourself", ex.getMessage());
        // Verify ledger was NEVER called
        verify(recordLedgerEntryUseCase, never()).recordTransferWithFee(any(), any(), any(), any(), any(), any(), any(),
                any());
    }

    // ========================================================================
    // TEST 3: Sender/Recipient Wallet Missing
    // ========================================================================
    @Test
    @DisplayName("Should reject transfer if sender wallet not found")
    void shouldRejectIfSenderWalletNotFound() {
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByAccountNumber(request.recipientAccountNumber())).thenReturn(Optional.of(recipient));

        // Sender wallet missing
        when(walletRepository.findByUserIdAndCurrency(senderId, Currency.YER)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executeTransferUseCase.execute(senderId, request));

        assertEquals("You don't have a YER wallet", ex.getMessage());
    }

    // ========================================================================
    // TEST 4: Wallets MUST be Active
    // ========================================================================
    @Test
    @DisplayName("Should reject transfer if sender wallet is frozen")
    void shouldRejectIfSenderWalletIsFrozen() {
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByAccountNumber(request.recipientAccountNumber())).thenReturn(Optional.of(recipient));

        senderWallet.freeze(); // Freeze the wallet!
        when(walletRepository.findByUserIdAndCurrency(senderId, Currency.YER)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserIdAndCurrency(recipientId, Currency.YER))
                .thenReturn(Optional.of(recipientWallet));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> executeTransferUseCase.execute(senderId, request));

        assertEquals("Your wallet is not active", ex.getMessage());
    }

    // ========================================================================
    // TEST 5: System Wallets Protection
    // ========================================================================
    @Test
    @DisplayName("Should reject transfer if system wallet is used")
    void shouldRejectIfSystemWalletUsed() {
        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByAccountNumber(request.recipientAccountNumber())).thenReturn(Optional.of(recipient));

        // Override sender wallet ID with a protected system wallet ID
        Wallet badSenderWallet = new Wallet(SystemWallets.getFeeWallet(Currency.YER), senderId, Currency.YER,
                BigDecimal.ZERO, WalletStatus.ACTIVE, Instant.now(), Instant.now());

        when(walletRepository.findByUserIdAndCurrency(senderId, Currency.YER)).thenReturn(Optional.of(badSenderWallet));
        when(walletRepository.findByUserIdAndCurrency(recipientId, Currency.YER))
                .thenReturn(Optional.of(recipientWallet));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> executeTransferUseCase.execute(senderId, request));

        assertEquals("System wallets cannot be used in P2P transfers", ex.getMessage());
    }
}
