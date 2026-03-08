package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.KycStatus;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.AdminKycAccountSummaryResponse;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Use case: List all accounts currently pending KYC verification.
 */
@Service
@RequiredArgsConstructor
public class ListPendingKycAccountsUseCase {

    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public List<AdminKycAccountSummaryResponse> execute() {
        return userRepository.findByKycStatus(KycStatus.PENDING).stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toSummary)
                .filter(summary -> summary.pendingDocuments() > 0)
                .toList();
    }

    private AdminKycAccountSummaryResponse toSummary(User user) {
        List<com.fintech.ewallet.kyc.domain.KycDocument> docs = kycDocumentRepository.findByUserId(user.getId());
        long pendingDocs = docs.stream()
                .filter(doc -> doc.getStatus() == KycStatus.PENDING)
                .count();

        return new AdminKycAccountSummaryResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getEmail(),
                user.getKycStatus(),
                user.getAccountStatus(),
                docs.size(),
                pendingDocs,
                user.getCreatedAt());
    }
}
