package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.kyc.application.dto.UploadBatchDocumentRequest;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentRequest;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentResponse;
import com.fintech.ewallet.kyc.domain.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadBatchKycDocumentsUseCase {

    private final UploadKycDocumentUseCase uploadKycDocumentUseCase;

    @Transactional
    public List<UploadDocumentResponse> execute(UUID userId, UploadBatchDocumentRequest request) {
        log.info("Batch uploading KYC documents for user: {}", userId);

        UploadDocumentResponse idFrontRes = uploadKycDocumentUseCase.execute(userId, 
            new UploadDocumentRequest(DocumentType.ID_FRONT, request.idFront()));
            
        UploadDocumentResponse idBackRes = uploadKycDocumentUseCase.execute(userId, 
            new UploadDocumentRequest(DocumentType.ID_BACK, request.idBack()));
            
        UploadDocumentResponse selfieRes = uploadKycDocumentUseCase.execute(userId, 
            new UploadDocumentRequest(DocumentType.SELFIE, request.selfie()));

        return List.of(idFrontRes, idBackRes, selfieRes);
    }
}
