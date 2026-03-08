package com.fintech.ewallet.bill.infrastructure;

import com.fintech.ewallet.bill.domain.BillPayment;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillerCategory;

public class BillMapper {

    public static Biller toDomain(BillerJpaEntity entity) {
        if (entity == null)
            return null;
        return Biller.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .category(BillerCategory.valueOf(entity.getCategory()))
                .supportedCurrency(entity.getSupportedCurrency())
                .walletId(entity.getWalletId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static BillerJpaEntity toEntity(Biller domain) {
        if (domain == null)
            return null;
        return BillerJpaEntity.builder()
                .id(domain.getId())
                .code(domain.getCode())
                .name(domain.getName())
                .category(domain.getCategory().name())
                .supportedCurrency(domain.getSupportedCurrency())
                .walletId(domain.getWalletId())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public static BillPayment toDomain(BillPaymentJpaEntity entity) {
        if (entity == null)
            return null;
        return BillPayment.builder()
                .id(entity.getId())
                .referenceNo(entity.getReferenceNo())
                .userId(entity.getUserId())
                .billerId(entity.getBillerId())
                .customerAccountNumber(entity.getCustomerAccountNumber())
                .amount(entity.getAmount())
                .feeAmount(entity.getFeeAmount())
                .totalDeducted(entity.getTotalDeducted())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .transactionId(entity.getTransactionId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static BillPaymentJpaEntity toEntity(BillPayment domain) {
        if (domain == null)
            return null;
        return BillPaymentJpaEntity.builder()
                .id(domain.getId())
                .referenceNo(domain.getReferenceNo())
                .userId(domain.getUserId())
                .billerId(domain.getBillerId())
                .customerAccountNumber(domain.getCustomerAccountNumber())
                .amount(domain.getAmount())
                .feeAmount(domain.getFeeAmount())
                .totalDeducted(domain.getTotalDeducted())
                .currency(domain.getCurrency())
                .status(domain.getStatus())
                .transactionId(domain.getTransactionId())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
