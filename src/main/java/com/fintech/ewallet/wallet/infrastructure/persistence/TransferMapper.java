package com.fintech.ewallet.wallet.infrastructure.persistence;

import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.P2PTransfer;
import com.fintech.ewallet.wallet.domain.TransferStatus;
import org.springframework.stereotype.Component;

/**
 * Hand-written mapper for P2PTransfer domain ↔ TransferJpaEntity.
 */
@Component
public class TransferMapper {

    public P2PTransfer toDomain(TransferJpaEntity entity) {
        if (entity == null)
            return null;
        return new P2PTransfer(
                entity.getId(),
                entity.getReferenceNo(),
                entity.getSenderUserId(),
                entity.getSenderWalletId(),
                entity.getRecipientUserId(),
                entity.getRecipientWalletId(),
                entity.getAmount(),
                entity.getFeeAmount(),
                entity.getTotalDeducted(),
                Currency.valueOf(entity.getCurrency()),
                TransferStatus.valueOf(entity.getStatus()),
                entity.getDescription(),
                entity.getTransactionId(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }

    public TransferJpaEntity toEntity(P2PTransfer transfer) {
        if (transfer == null)
            return null;
        return new TransferJpaEntity(
                transfer.getId(),
                transfer.getReferenceNo(),
                transfer.getSenderUserId(),
                transfer.getSenderWalletId(),
                transfer.getRecipientUserId(),
                transfer.getRecipientWalletId(),
                transfer.getAmount(),
                transfer.getFeeAmount(),
                transfer.getTotalDeducted(),
                transfer.getCurrency().name(),
                transfer.getStatus().name(),
                transfer.getDescription(),
                transfer.getTransactionId(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt());
    }
}
