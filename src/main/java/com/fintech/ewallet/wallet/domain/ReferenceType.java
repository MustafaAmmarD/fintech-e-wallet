package com.fintech.ewallet.wallet.domain;

public enum ReferenceType {
    TRANSFER, // User-to-user money transfer
    DEPOSIT, // Agent/bank → user (cash in)
    WITHDRAWAL, // User → agent/bank (cash out)
    FEE // System transaction fee
}
