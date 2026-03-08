package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.referral.domain.Referral;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import com.fintech.ewallet.wallet.application.CreateWalletUseCase;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.LedgerRepository;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompleteReferralUseCase {

    private static final BigDecimal REFERRER_REWARD = new BigDecimal("500");
    private static final BigDecimal REFEREE_REWARD = new BigDecimal("200");

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CreateWalletUseCase createWalletUseCase;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final LedgerRepository ledgerRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean executeIfEligible(UUID refereeId) {
        Referral referral = referralRepository.findPendingByRefereeId(refereeId).orElse(null);
        if (referral == null) {
            return false;
        }

        User referee = userRepository.findById(refereeId).orElse(null);
        if (referee == null || referee.getKycStatus() != KycStatus.VERIFIED) {
            log.debug("Referral not eligible yet (missing verified KYC). refereeId={}", refereeId);
            return false;
        }

        if (!ledgerRepository.existsFinancialTransactionForUser(refereeId)) {
            log.debug("Referral not eligible yet (no financial transaction). refereeId={}", refereeId);
            return false;
        }

        User referrer = userRepository.findById(referral.getReferrerId())
                .orElseThrow(() -> new IllegalStateException("Referrer user not found: " + referral.getReferrerId()));

        createWalletUseCase.createWalletsForUser(referrer.getId());
        createWalletUseCase.createWalletsForUser(referee.getId());

        Wallet referrerYerWallet = walletRepository.findByUserIdAndCurrency(referrer.getId(), Currency.YER)
                .orElseThrow(() -> new IllegalStateException("Referrer doesn't have a YER wallet"));
        Wallet refereeYerWallet = walletRepository.findByUserIdAndCurrency(referee.getId(), Currency.YER)
                .orElseThrow(() -> new IllegalStateException("Referee doesn't have a YER wallet"));

        recordLedgerEntryUseCase.recordDoubleEntry(
                SystemWallets.getLiquidityWallet(Currency.YER),
                referrerYerWallet.getId(),
                REFERRER_REWARD,
                ReferenceType.REFERRAL,
                referral.getId(),
                "Referral reward for referrer");

        recordLedgerEntryUseCase.recordDoubleEntry(
                SystemWallets.getLiquidityWallet(Currency.YER),
                refereeYerWallet.getId(),
                REFEREE_REWARD,
                ReferenceType.REFERRAL,
                referral.getId(),
                "Referral reward for referee");

        referral.markRewarded(REFERRER_REWARD, REFEREE_REWARD);
        referralRepository.save(referral);

        log.info("Referral completed. referralId={}, referrerId={}, refereeId={}",
                referral.getId(), referrer.getId(), referee.getId());
        return true;
    }
}
