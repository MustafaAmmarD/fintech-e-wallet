package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.ApproveKycDocumentResponse;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.InvalidDocumentException;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import com.fintech.ewallet.wallet.application.CreateWalletUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case: Approve a KYC document and activate user wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproveKycDocumentUseCase {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final CreateWalletUseCase createWalletUseCase;

    @Transactional
    public ApproveKycDocumentResponse execute(UUID documentId, UUID adminId) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC document not found"));

        if (document.getStatus() != KycStatus.PENDING) {
            throw new InvalidDocumentException("Only pending KYC documents can be approved");
        }

        User user = userRepository.findById(document.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        document.approve(adminId);
        kycDocumentRepository.save(document);

        user.updateKycStatus(KycStatus.VERIFIED);
        userRepository.save(user);

        createWalletUseCase.createWalletsForUser(user.getId());

        log.info("KYC approved for user {} by reviewer {}; wallets activated", user.getId(), adminId);

        return new ApproveKycDocumentResponse(
                document.getId(),
                user.getId(),
                KycStatus.VERIFIED,
                "KYC approved. Wallets are now activated.");
    }
}
