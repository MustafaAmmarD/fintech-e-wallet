package com.fintech.ewallet.kyc.api;

import com.fintech.ewallet.kyc.application.GetKycStatusUseCase;
import com.fintech.ewallet.kyc.application.UploadKycDocumentUseCase;
import com.fintech.ewallet.kyc.application.dto.KycStatusResponse;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentRequest;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentResponse;
import com.fintech.ewallet.kyc.domain.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * KYC verification endpoints.
 */
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final UploadKycDocumentUseCase uploadKycDocumentUseCase;
    private final GetKycStatusUseCase getKycStatusUseCase;

    /**
     * Upload a KYC document for verification.
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadDocumentResponse> uploadDocument(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam("file") MultipartFile file) {

        UploadDocumentRequest request = new UploadDocumentRequest(documentType, file);
        UploadDocumentResponse response = uploadKycDocumentUseCase.execute(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user's KYC status and uploaded documents.
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @AuthenticationPrincipal UUID userId) {

        KycStatusResponse response = getKycStatusUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
}
