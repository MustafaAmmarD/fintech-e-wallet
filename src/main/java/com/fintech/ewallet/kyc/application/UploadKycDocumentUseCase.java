package com.fintech.ewallet.kyc.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentRequest;
import com.fintech.ewallet.kyc.application.dto.UploadDocumentResponse;
import com.fintech.ewallet.kyc.domain.FileStorageService;
import com.fintech.ewallet.kyc.domain.KycDocument;
import com.fintech.ewallet.kyc.domain.KycDocumentRepository;
import com.fintech.ewallet.shared.exception.InvalidDocumentException;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Use case: Upload a KYC document for verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadKycDocumentUseCase {

        private final KycDocumentRepository kycDocumentRepository;
        private final UserRepository userRepository;
        private final FileStorageService fileStorageService;

        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        private static final List<String> ALLOWED_MIME_TYPES = List.of(
                        "image/jpeg",
                        "image/png",
                        "application/pdf");

        @Transactional
        public UploadDocumentResponse execute(UUID userId, UploadDocumentRequest request) {
                // 1. Validate file
                validateFile(request);

                // 2. Store file
                String filePath;
                try {
                        filePath = fileStorageService.storeFile(
                                        request.file().getBytes(),
                                        request.file().getOriginalFilename(),
                                        userId);
                } catch (IOException e) {
                        log.error("Failed to store KYC document for user {}", userId, e);
                        throw new InvalidDocumentException("Failed to store document: " + e.getMessage());
                }

                // 3. Create KycDocument entity
                KycDocument document = KycDocument.upload(
                                userId,
                                request.documentType(),
                                filePath,
                                request.file().getOriginalFilename(),
                                request.file().getContentType(),
                                request.file().getSize());

                // 4. Save to database
                kycDocumentRepository.save(document);

                // 5. Update user KYC status to PENDING (awaiting review)
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                user.updateKycStatus(com.fintech.ewallet.identity.domain.KycStatus.PENDING);
                userRepository.save(user);

                log.info("KYC document uploaded for user {}: {} ({})",
                                userId, request.documentType(), request.file().getOriginalFilename());

                return new UploadDocumentResponse(
                                document.getId(),
                                document.getDocumentType(),
                                document.getFileName(),
                                document.getStatus(),
                                document.getUploadedAt());
        }

        private void validateFile(UploadDocumentRequest request) {
                if (request.file().isEmpty()) {
                        throw new InvalidDocumentException("File is empty");
                }

                // Check file size
                if (request.file().getSize() > MAX_FILE_SIZE) {
                        throw new InvalidDocumentException(
                                        String.format("File size exceeds maximum allowed size of %d MB",
                                                        MAX_FILE_SIZE / (1024 * 1024)));
                }

                // Check MIME type
                String contentType = request.file().getContentType();
                if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
                        throw new InvalidDocumentException(
                                        "Invalid file type. Allowed types: JPEG, PNG, PDF");
                }
        }
}
