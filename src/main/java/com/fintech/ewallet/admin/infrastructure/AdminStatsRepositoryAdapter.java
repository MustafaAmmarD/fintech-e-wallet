package com.fintech.ewallet.admin.infrastructure;

import com.fintech.ewallet.admin.domain.AdminStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class AdminStatsRepositoryAdapter implements AdminStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long getTotalUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public long getTotalTransactions() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT transaction_id) FROM ledger_entries WHERE reference_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'EXCHANGE')",
                Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public BigDecimal getTotalTransactionVolume() {
        BigDecimal volume = jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM ledger_entries WHERE entry_type = 'DEBIT' AND reference_type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'EXCHANGE')",
                BigDecimal.class);
        return volume != null ? volume : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalSystemFees() {
        BigDecimal fees = jdbcTemplate.queryForObject(
                "SELECT SUM(amount) FROM ledger_entries WHERE entry_type = 'CREDIT' AND reference_type = 'FEE'",
                BigDecimal.class);
        return fees != null ? fees : BigDecimal.ZERO;
    }
}
