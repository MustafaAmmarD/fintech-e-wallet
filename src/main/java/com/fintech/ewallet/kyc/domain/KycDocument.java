package com.fintech.ewallet.kyc.domain;

import com.fintech.ewallet.identity.domain.KycStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * KYC Document domain entity.
 * <p>
 * Represents an uploaded identity verification document.
 */
public class KycDocument {

    private UUID id;
    private UUID userId;
    private DocumentType documentType;
    private String filePath;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private KycStatus status;
    private String rejectionReason;
    private Instant uploadedAt;
    private Instant reviewedAt;
    private UUID reviewedBy;

    // Public no-arg constructor for JPA/MapStruct
    public KycDocument() {
    }

    /**
     * Factory method: Upload a new KYC document.
     */
    public static KycDocument upload(
            UUID userId,
            DocumentType documentType,
            String filePath,
            String fileName,
            String mimeType,
            Long fileSize) {

        KycDocument document = new KycDocument();
        document.id = UUID.randomUUID();
        document.userId = userId;
        document.documentType = documentType;
        document.filePath = filePath;
        document.fileName = fileName;
        document.mimeType = mimeType;
        document.fileSize = fileSize;
        document.status = KycStatus.PENDING;
        document.uploadedAt = Instant.now();
        return document;
    }

    /**
     * Approve this document.
     */
    public void approve(UUID adminId) {
        this.status = KycStatus.VERIFIED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = null;
    }

    /**
     * Reject this document with a reason.
     */
    public void reject(UUID adminId, String reason) {
        this.status = KycStatus.REJECTED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
        this.rejectionReason = reason;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public KycStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    // Setters for JPA/MapStruct
    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setStatus(KycStatus status) {
        this.status = status;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
