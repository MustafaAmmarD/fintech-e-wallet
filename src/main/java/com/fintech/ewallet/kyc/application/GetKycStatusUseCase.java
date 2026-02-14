package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.KycStatusResponse;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Use case: Get user's KYC status and uploaded documents.
 */
@Service
@RequiredArgsConstructor
public class GetKycStatusUseCase {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public KycStatusResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<KycDocument> documents = kycDocumentRepository.findByUserId(userId);

        List<KycStatusResponse.DocumentInfo> documentInfos = documents.stream()
                .map(doc -> new KycStatusResponse.DocumentInfo(
                        doc.getId(),
                        doc.getDocumentType(),
                        doc.getFileName(),
                        doc.getStatus(),
                        doc.getRejectionReason(),
                        doc.getUploadedAt()))
                .toList();

        return new KycStatusResponse(user.getKycStatus(), documentInfos);
    }
}
