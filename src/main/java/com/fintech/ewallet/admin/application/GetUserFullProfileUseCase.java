package com.fintech.ewallet.admin.application;

import com.fintech.ewallet.admin.application.dto.UserFullProfileResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.notification.application.dto.NotificationResponse;
import com.fintech.ewallet.notification.domain.Notification;
import com.fintech.ewallet.notification.domain.NotificationRepository;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.LedgerEntry;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetUserFullProfileUseCase {

        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final LedgerRepository ledgerRepository;
        private final NotificationRepository notificationRepository;
        private final ReferralRepository referralRepository;

        @Transactional(readOnly = true)
        public UserFullProfileResponse execute(UUID userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found"));

                // Wallets
                List<Wallet> wallets = walletRepository.findByUserId(userId);
                List<UserFullProfileResponse.WalletInfo> walletInfos = wallets.stream()
                                .map(w -> UserFullProfileResponse.WalletInfo.builder()
                                                .id(w.getId())
                                                .currency(w.getCurrency())
                                                .balance(w.getBalance())
                                                .status(w.getStatus().name())
                                                .createdAt(w.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());

                // Recent Transactions (top 10 across all user's wallets)
                List<LedgerEntry> allWalletEntries = new ArrayList<>();
                for (Wallet w : wallets) {
                        // we grab up to 10 from each wallet, then sort globally and limit to 10
                        allWalletEntries.addAll(ledgerRepository.findByWalletIdOrderByCreatedAtDesc(w.getId(), 10));
                }
                List<TransactionResponse> recentTx = allWalletEntries.stream()
                                .sorted(Comparator.comparing(LedgerEntry::getCreatedAt).reversed())
                                .limit(10)
                                .map(entry -> new TransactionResponse(
                                                entry.getId(),
                                                entry.getTransactionId(),
                                                entry.getEntryType(),
                                                entry.getAmount(),
                                                entry.getBalanceAfter(),
                                                entry.getReferenceType(),
                                                entry.getDescription(),
                                                entry.getCreatedAt()))
                                .collect(Collectors.toList());

                // Recent Notifications (top 5)
                List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 5);
                List<NotificationResponse> recentNotifs = notifications.stream()
                                .map(n -> new NotificationResponse(
                                                n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                                                n.getReferenceType(), n.getReferenceId(), n.isRead(), n.getCreatedAt()))
                                .collect(Collectors.toList());

                // Referral Data
                boolean wasReferred = referralRepository.findByRefereeId(userId).isPresent();
                long totalReferred = referralRepository.countByReferrerId(userId);

                return UserFullProfileResponse.builder()
                                .id(user.getId())
                                .phoneNumber(user.getPhoneNumber())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .kycStatus(user.getKycStatus())
                                .accountStatus(user.getAccountStatus())
                                .role(user.getRole())
                                .language(user.getLanguage())
                                .referralCode(user.getReferralCode())
                                .accountNumber(user.getAccountNumber())
                                .showFullName(user.isShowFullName())
                                .failedLoginAttempts(user.getFailedLoginAttempts())
                                .lastLoginAt(user.getLastLoginAt())
                                .lockedUntil(user.getLockedUntil())
                                .createdAt(user.getCreatedAt())
                                .wallets(walletInfos)
                                .recentTransactions(recentTx)
                                .recentNotifications(recentNotifs)
                                .wasReferred(wasReferred)
                                .totalPeopleReferred(totalReferred)
                                .build();
        }
}
