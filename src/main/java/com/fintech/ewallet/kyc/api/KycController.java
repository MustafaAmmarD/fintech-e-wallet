package com.fintech.ewallet.kyc.api;

import com.fintech.ewallet.kyc.application.GetKycStatusUseCase;
import com.fintech.ewallet.kyc.application.UploadBatchKycDocumentsUseCase;
import com.fintech.ewallet.kyc.application.dto.KycStatusResponse;
import com.fintech.ewallet.kyc.application.dto.UploadBatchDocumentRequest;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentResponse;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * KYC verification endpoints.
 */
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {

    private final UploadBatchKycDocumentsUseCase uploadBatchKycDocumentsUseCase;
    private final GetKycStatusUseCase getKycStatusUseCase;

    /**
     * Upload KYC documents for verification (Front ID, Back ID, Selfie).
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UploadDocumentResponse>> uploadDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestPart("idFront") MultipartFile idFront,
            @RequestPart("idBack") MultipartFile idBack,
            @RequestPart("selfie") MultipartFile selfie) {

        UploadBatchDocumentRequest request = new UploadBatchDocumentRequest(idFront, idBack, selfie);
        List<UploadDocumentResponse> response = uploadBatchKycDocumentsUseCase.execute(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user's KYC status and uploaded documents.
     */
    @GetMapping("/status")
    public ResponseEntity<KycStatusResponse> getKycStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {

        KycStatusResponse response = getKycStatusUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
}
