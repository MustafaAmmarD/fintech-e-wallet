package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.AdminKycAccountDetailsResponse;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Use case: Get full KYC/account details for one user.
 */
@Service
@RequiredArgsConstructor
public class GetKycAccountDetailsUseCase {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public AdminKycAccountDetailsResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<AdminKycAccountDetailsResponse.DocumentInfo> documents = kycDocumentRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(KycDocument::getUploadedAt).reversed())
                .map(doc -> new AdminKycAccountDetailsResponse.DocumentInfo(
                        doc.getId(),
                        doc.getDocumentType(),
                        doc.getFileName(),
                        doc.getMimeType(),
                        doc.getFileSize(),
                        doc.getStatus(),
                        doc.getRejectionReason(),
                        doc.getUploadedAt(),
                        doc.getReviewedAt(),
                        doc.getReviewedBy()))
                .toList();

        return new AdminKycAccountDetailsResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getEmail(),
                user.getKycStatus(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                documents);
    }
}
