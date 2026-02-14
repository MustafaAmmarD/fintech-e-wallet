package com.fintech.ewallet.kyc.application.dto;

import com.fintech.ewallet.kyc.domain.DocumentType;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request to upload a KYC document.
 */
public record UploadDocumentRequest(
        @NotNull(message = "Document type is required") DocumentType documentType,

        @NotNull(message = "File is required") MultipartFile file) {
}
