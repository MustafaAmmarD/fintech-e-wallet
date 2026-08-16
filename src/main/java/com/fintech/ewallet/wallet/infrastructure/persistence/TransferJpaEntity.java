package com.fintech.ewallet.wallet.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "reference_no", nullable = false, unique = true, length = 20)
    private String referenceNo;

    @Column(name = "sender_user_id", nullable = true)
    private UUID senderUserId;

    @Column(name = "sender_wallet_id", nullable = true)
    private UUID senderWalletId;
    
    @Column(name = "sender_phone_number", nullable = true, length = 20)
    private String senderPhoneNumber;

    @Column(name = "recipient_user_id", nullable = true)
    private UUID recipientUserId;

    @Column(name = "recipient_wallet_id", nullable = true)
    private UUID recipientWalletId;
    
    @Column(name = "target_phone_number", nullable = true, length = 20)
    private String targetPhoneNumber;
    
    @Column(name = "cancel_reason", nullable = true, length = 255)
    private String cancelReason;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "total_deducted", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeducted;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 500)
    private String description;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
