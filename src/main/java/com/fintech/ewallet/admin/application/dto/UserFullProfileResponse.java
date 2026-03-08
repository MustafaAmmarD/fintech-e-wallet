package com.fintech.ewallet.admin.application.dto;

import com.fintech.ewallet.identity.domain.AccountStatus;
import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.UserRole;
import com.fintech.ewallet.notification.application.dto.NotificationResponse;
import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserFullProfileResponse {

    private UUID id;
    private String phoneNumber;
    private String fullName;
    private String email;
    private KycStatus kycStatus;
    private AccountStatus accountStatus;
    private UserRole role;
    private String language;
    private String referralCode;
    private String accountNumber;
    private boolean showFullName;
    private int failedLoginAttempts;
    private Instant lastLoginAt;
    private Instant lockedUntil;
    private Instant createdAt;

    // Wallets
    private List<WalletInfo> wallets;

    // Referral info
    private boolean wasReferred;
    private long totalPeopleReferred;

    // Activity summaries (last few)
    private List<TransactionResponse> recentTransactions;
    private List<NotificationResponse> recentNotifications;

    @Data
    @Builder
    public static class WalletInfo {
        private UUID id;
        private Currency currency;
        private BigDecimal balance;
        private String status;
        private Instant createdAt;
    }
}
