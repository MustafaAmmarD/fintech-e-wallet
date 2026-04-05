package com.fintech.ewallet.kyc.application.dto;

import org.springframework.web.multipart.MultipartFile;

public record UploadBatchDocumentRequest(
    MultipartFile idFront,
    MultipartFile idBack,
    MultipartFile selfie
) {}
