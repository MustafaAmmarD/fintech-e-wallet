package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.PendKycAccountResponse;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: Set an account's KYC status to PENDING.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendKycAccountUseCase {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    @Transactional
    public PendKycAccountResponse execute(UUID userId, UUID reviewerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int movedDocuments = 0;
        for (var document : kycDocumentRepository.findByUserId(userId)) {
            if (document.getStatus() != KycStatus.PENDING) {
                document.markPendingReview();
                kycDocumentRepository.save(document);
                movedDocuments++;
            }
        }

        user.updateKycStatus(KycStatus.PENDING);
        userRepository.save(user);

        log.info("Account {} moved to KYC_PENDING by reviewer {}; documents reset to pending: {}",
                userId, reviewerId, movedDocuments);

        return new PendKycAccountResponse(
                userId,
                KycStatus.PENDING,
                "Account moved to pending verification.");
    }
}
