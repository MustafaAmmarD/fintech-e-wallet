package com.fintech.ewallet.wallet.domain;

public enum ReferenceType {
    TRANSFER, // User-to-user money transfer
    DEPOSIT, // Agent/bank to user (cash in)
    WITHDRAWAL, // User to agent/bank (cash out)
    EXCHANGE, // User currency exchange
    REFERRAL, // Referral reward transfer
    FEE, // System transaction fee
    BILL_PAYMENT // Bill payment
}
