package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.shared.event.FinancialTransactionCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralTransactionListener {

    private final CompleteReferralUseCase completeReferralUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFinancialTransactionCompleted(FinancialTransactionCompletedEvent event) {
        if (event == null || event.participantUserIds() == null || event.participantUserIds().isEmpty()) {
            return;
        }

        for (UUID userId : event.participantUserIds()) {
            try {
                completeReferralUseCase.executeIfEligible(userId);
            } catch (Exception ex) {
                log.error("Failed to complete referral after financial transaction for userId={}", userId, ex);
            }
        }
    }
}
