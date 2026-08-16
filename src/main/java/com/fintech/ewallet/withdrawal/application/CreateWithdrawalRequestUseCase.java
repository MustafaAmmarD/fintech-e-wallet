package com.fintech.ewallet.withdrawal.application;

import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import com.fintech.ewallet.withdrawal.application.dto.CreateWithdrawalRequestDto;
import com.fintech.ewallet.withdrawal.application.dto.WithdrawalRequestResponse;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequest;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestRepository;
import com.fintech.ewallet.withdrawal.domain.WithdrawalRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateWithdrawalRequestUseCase {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WalletRepository walletRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    private static final int MAX_PENDING_REQUESTS = 3;
    private static final int EXPIRY_MINUTES = 30;

    @Transactional
    public WithdrawalRequestResponse execute(UUID userId, CreateWithdrawalRequestDto request) {
        // 1. Check max active requests
        long pendingCount = withdrawalRequestRepository.countByUserIdAndStatus(userId, WithdrawalRequestStatus.PENDING);
        if (pendingCount >= MAX_PENDING_REQUESTS) {
            throw new IllegalStateException("Maximum of " + MAX_PENDING_REQUESTS + " active withdrawal requests allowed.");
        }

        // 2. Validate wallet and balance
        Wallet wallet = walletRepository.findById(request.walletId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Wallet does not belong to user");
        }

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active");
        }

        if (!wallet.getCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("Currency mismatch");
        }

        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }

        // 3. Generate unique code
        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

        // 4. Create and save entity
        WithdrawalRequest withdrawalRequest = new WithdrawalRequest(
                userId,
                wallet.getId(),
                request.amount(),
                request.currency(),
                code,
                expiresAt
        );

        WithdrawalRequest saved = withdrawalRequestRepository.save(withdrawalRequest);

        return new WithdrawalRequestResponse(
                saved.getId(),
                saved.getWithdrawalCode(),
                saved.getAmount(),
                saved.getCurrency(),
                saved.getStatus(),
                saved.getExpiresAt(),
                saved.getCreatedAt()
        );
    }

    private String generateUniqueCode() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            // Generate 6 digit numeric code
            int num = 100000 + secureRandom.nextInt(900000);
            String code = String.valueOf(num);
            if (withdrawalRequestRepository.findByWithdrawalCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate unique withdrawal code");
    }
}
