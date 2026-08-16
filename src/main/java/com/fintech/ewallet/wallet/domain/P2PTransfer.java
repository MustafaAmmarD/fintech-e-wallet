package com.fintech.ewallet.wallet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * P2PTransfer domain entity — represents a completed, pending, or reversed money transfer
 * between two user wallets, or involving external/unregistered users.
 */
public class P2PTransfer {

    private final UUID id;
    private final String referenceNo;
    private UUID senderUserId;
    private UUID senderWalletId;
    private UUID recipientUserId;
    private UUID recipientWalletId;
    
    private final String senderPhoneNumber;
    private final String targetPhoneNumber;
    private String cancelReason;

    private final BigDecimal amount;
    private final BigDecimal feeAmount;
    private final BigDecimal totalDeducted;
    private final Currency currency;
    private TransferStatus status;
    private final String description;
    private final UUID transactionId; // Links to ledger_entries
    private final Instant createdAt;
    private Instant completedAt;

    // Standard constructor for new completed transfer (existing logic)
    public P2PTransfer(
            UUID senderUserId,
            UUID senderWalletId,
            UUID recipientUserId,
            UUID recipientWalletId,
            BigDecimal amount,
            BigDecimal feeAmount,
            Currency currency,
            String description,
            UUID transactionId) {
        this.id = UUID.randomUUID();
        this.referenceNo = generateReferenceNo();
        this.senderUserId = senderUserId;
        this.senderWalletId = senderWalletId;
        this.recipientUserId = recipientUserId;
        this.recipientWalletId = recipientWalletId;
        this.senderPhoneNumber = null;
        this.targetPhoneNumber = null;
        this.cancelReason = null;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.totalDeducted = amount.add(feeAmount);
        this.currency = currency;
        this.status = TransferStatus.COMPLETED;
        this.description = description;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
        this.completedAt = Instant.now();
    }
    
    // Constructor for new pending/external transfer
    public P2PTransfer(
            UUID senderUserId,
            UUID senderWalletId,
            String senderPhoneNumber,
            UUID recipientUserId,
            UUID recipientWalletId,
            String targetPhoneNumber,
            BigDecimal amount,
            BigDecimal feeAmount,
            Currency currency,
            String description,
            UUID transactionId,
            TransferStatus initialStatus) {
        this.id = UUID.randomUUID();
        this.referenceNo = generateReferenceNo();
        this.senderUserId = senderUserId;
        this.senderWalletId = senderWalletId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.recipientUserId = recipientUserId;
        this.recipientWalletId = recipientWalletId;
        this.targetPhoneNumber = targetPhoneNumber;
        this.cancelReason = null;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.totalDeducted = amount.add(feeAmount);
        this.currency = currency;
        this.status = initialStatus;
        this.description = description;
        this.transactionId = transactionId;
        this.createdAt = Instant.now();
        if (initialStatus == TransferStatus.COMPLETED) {
            this.completedAt = Instant.now();
        } else {
            this.completedAt = null;
        }
    }

    // Reconstruct from DB
    public P2PTransfer(
            UUID id,
            String referenceNo,
            UUID senderUserId,
            UUID senderWalletId,
            String senderPhoneNumber,
            UUID recipientUserId,
            UUID recipientWalletId,
            String targetPhoneNumber,
            String cancelReason,
            BigDecimal amount,
            BigDecimal feeAmount,
            BigDecimal totalDeducted,
            Currency currency,
            TransferStatus status,
            String description,
            UUID transactionId,
            Instant createdAt,
            Instant completedAt) {
        this.id = id;
        this.referenceNo = referenceNo;
        this.senderUserId = senderUserId;
        this.senderWalletId = senderWalletId;
        this.senderPhoneNumber = senderPhoneNumber;
        this.recipientUserId = recipientUserId;
        this.recipientWalletId = recipientWalletId;
        this.targetPhoneNumber = targetPhoneNumber;
        this.cancelReason = cancelReason;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.totalDeducted = totalDeducted;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    // Business logic

    public void reverse() {
        if (this.status != TransferStatus.COMPLETED) {
            throw new IllegalStateException("Only completed transfers can be reversed");
        }
        this.status = TransferStatus.REVERSED;
    }
    
    public void receive(UUID recipientUserId, UUID recipientWalletId) {
        if (this.status != TransferStatus.PENDING && this.status != TransferStatus.UNCLAIMED) {
            throw new IllegalStateException("Transfer is not in a receivable state");
        }
        this.recipientUserId = recipientUserId;
        this.recipientWalletId = recipientWalletId;
        this.status = TransferStatus.COMPLETED;
        this.completedAt = Instant.now();
    }
    
    public void cancel(String reason) {
        if (this.status != TransferStatus.PENDING && this.status != TransferStatus.UNCLAIMED) {
            throw new IllegalStateException("Transfer is not in a cancellable state");
        }
        this.status = TransferStatus.CANCELLED;
        this.cancelReason = reason;
        this.completedAt = Instant.now();
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public UUID getSenderUserId() {
        return senderUserId;
    }

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public String getSenderPhoneNumber() {
        return senderPhoneNumber;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public UUID getRecipientWalletId() {
        return recipientWalletId;
    }

    public String getTargetPhoneNumber() {
        return targetPhoneNumber;
    }
    
    public String getCancelReason() {
        return cancelReason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public BigDecimal getTotalDeducted() {
        return totalDeducted;
    }

    public Currency getCurrency() {
        return currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    // Generate human-readable reference number (e.g., TRF-20260223-A1B2C3)
    private static String generateReferenceNo() {
        String datePart = java.time.LocalDate.now().toString().replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TRF-" + datePart + "-" + randomPart;
    }
}
