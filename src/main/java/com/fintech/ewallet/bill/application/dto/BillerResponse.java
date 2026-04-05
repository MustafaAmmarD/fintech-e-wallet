package com.fintech.ewallet.bill.application.dto;

import com.fintech.ewallet.bill.domain.Biller;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.UUID;

public record BillerResponse(
        UUID id,
        String code,
        String name,
        String category,
        String supportedCurrency) {

    public static BillerResponse fromEntity(Biller biller) {
        String localizedName = "ar".equals(LocaleContextHolder.getLocale().getLanguage()) 
                ? (biller.getNameAr() != null ? biller.getNameAr() : biller.getName()) 
                : biller.getName();

        return new BillerResponse(
                biller.getId(),
                biller.getCode(),
                localizedName,
                biller.getCategory() != null ? biller.getCategory().name() : null,
                biller.getSupportedCurrency());
    }
}
