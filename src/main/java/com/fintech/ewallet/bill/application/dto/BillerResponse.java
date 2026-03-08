package com.fintech.ewallet.bill.application.dto;

import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillerCategory;

import java.util.UUID;

public record BillerResponse(
        UUID id,
        String code,
        String name,
        BillerCategory category,
        String supportedCurrency) {
    public static BillerResponse fromEntity(Biller biller) {
        return new BillerResponse(
                biller.getId(),
                biller.getCode(),
                biller.getName(),
                biller.getCategory(),
                biller.getSupportedCurrency());
    }
}
