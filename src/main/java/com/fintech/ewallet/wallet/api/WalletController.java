package com.fintech.ewallet.wallet.api;

import com.fintech.ewallet.wallet.application.GetAllWalletsUseCase;
import com.fintech.ewallet.wallet.application.GetBalanceUseCase;
import com.fintech.ewallet.wallet.application.GetTransactionHistoryUseCase;
import com.fintech.ewallet.wallet.application.TransferMoneyUseCase;
import com.fintech.ewallet.wallet.application.dto.BalanceResponse;
import com.fintech.ewallet.wallet.application.dto.TransferRequest;
import com.fintech.ewallet.wallet.application.dto.TransferResponse;
import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.application.dto.WalletSummary;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for wallet operations.
 */
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final GetAllWalletsUseCase getAllWalletsUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final TransferMoneyUseCase transferMoneyUseCase;
    private final WalletRepository walletRepository;

    /**
     * GET /api/v1/wallets
     * List all wallets for authenticated user.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<WalletSummary>> getAllWallets(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        List<WalletSummary> wallets = getAllWalletsUseCase.execute(userId);
        return ResponseEntity.ok(wallets);
    }

    /**
     * GET /api/v1/wallets/{walletId}
     * Get balance for a specific wallet.
     */
    @GetMapping("/{walletId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID walletId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        // Verify wallet ownership
        verifyWalletOwnership(walletId, userId);

        BalanceResponse response = getBalanceUseCase.execute(walletId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/wallets/{walletId}/transactions
     * Get transaction history for a wallet.
     */
    @GetMapping("/{walletId}/transactions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        // Verify wallet ownership
        verifyWalletOwnership(walletId, userId);

        // Limit max page size
        int limit = Math.min(size, 50);

        List<TransactionResponse> transactions = getTransactionHistoryUseCase.execute(walletId, limit);
        return ResponseEntity.ok(transactions);
    }

    /**
     * POST /api/v1/wallets/transfer
     * Transfer money from the authenticated user's wallet to another user wallet.
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate wallet transfer")
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransferResponse response = transferMoneyUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify that the authenticated user owns the requested wallet.
     */
    private void verifyWalletOwnership(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: wallet belongs to another user");
        }
    }
}
