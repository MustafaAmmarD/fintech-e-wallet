package com.fintech.ewallet.bill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Biller {
    private UUID id;
    private String code;
    private String name;
    private BillerCategory category;
    private String supportedCurrency;
    private UUID walletId;
    private String status;
    private Instant createdAt;
}
