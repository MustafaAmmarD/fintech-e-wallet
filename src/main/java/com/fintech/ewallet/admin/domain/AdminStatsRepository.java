package com.fintech.ewallet.admin.domain;

import java.math.BigDecimal;

public interface AdminStatsRepository {
    long getTotalUsers();

    long getTotalTransactions();

    BigDecimal getTotalTransactionVolume();

    BigDecimal getTotalSystemFees();
}
