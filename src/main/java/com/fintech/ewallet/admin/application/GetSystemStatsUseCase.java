package com.fintech.ewallet.admin.application;

import com.fintech.ewallet.admin.domain.AdminStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetSystemStatsUseCase {

    private final AdminStatsRepository adminStatsRepository;

    public SystemStatsResponse execute() {
        return new SystemStatsResponse(
                adminStatsRepository.getTotalUsers(),
                adminStatsRepository.getTotalTransactions(),
                adminStatsRepository.getTotalTransactionVolume(),
                adminStatsRepository.getTotalSystemFees());
    }

    public record SystemStatsResponse(
            long totalUsers,
            long totalTransactions,
            BigDecimal totalTransactionVolume,
            BigDecimal totalSystemFees) {
    }
}
