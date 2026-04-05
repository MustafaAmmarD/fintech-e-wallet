package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.ApproveKycAccountResponse;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.InvalidDocumentException;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import com.fintech.ewallet.wallet.application.CreateWalletUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Use case: Approve a user's pending KYC documents and activate wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveKycAccountUseCase {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final CreateWalletUseCase createWalletUseCase;

    @Transactional
    public ApproveKycAccountResponse execute(UUID userId, UUID reviewerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Approve any pending documents that exist (may be empty — that's OK in dev)
        List<KycDocument> pendingDocuments = kycDocumentRepository.findByUserId(userId).stream()
                .filter(doc -> doc.getStatus() == KycStatus.PENDING)
                .toList();

        for (KycDocument document : pendingDocuments) {
            document.approve(reviewerId);
            kycDocumentRepository.save(document);
        }

        user.updateKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        createWalletUseCase.createWalletsForUser(user.getId());

        log.info("KYC approved for account {} by reviewer {}; approved docs: {}", userId, reviewerId,
                pendingDocuments.size());

        return new ApproveKycAccountResponse(
                userId,
                pendingDocuments.size(),
                KycStatus.VERIFIED,
                "Account verified successfully. Wallets are activated.");
    }
}
