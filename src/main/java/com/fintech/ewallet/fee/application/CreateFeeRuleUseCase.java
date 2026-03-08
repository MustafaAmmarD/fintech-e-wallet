package com.fintech.ewallet.fee.application;

import com.fintech.ewallet.fee.application.dto.CreateFeeRuleRequest;
import com.fintech.ewallet.fee.application.dto.FeeRuleResponse;
import com.fintech.ewallet.fee.domain.FeeRule;
import com.fintech.ewallet.fee.domain.FeeRuleRepository;
import com.fintech.ewallet.fee.domain.FeeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreateFeeRuleUseCase {

    private final FeeRuleRepository feeRuleRepository;

    @Transactional
    public FeeRuleResponse execute(CreateFeeRuleRequest request) {
        validateRequest(request);

        if (request.replaceExisting()) {
            feeRuleRepository.deactivateAll(request.operationType(), request.currency());
        }

        FeeRule feeRule = FeeRule.create(
                request.operationType(),
                request.currency(),
                request.feeType(),
                request.rate(),
                request.flatAmount(),
                request.minAmount(),
                request.maxAmount(),
                request.minFee(),
                request.maxFee(),
                true);

        FeeRule saved = feeRuleRepository.save(feeRule);
        return toResponse(saved);
    }

    private void validateRequest(CreateFeeRuleRequest request) {
        if (request.maxAmount() != null && request.maxAmount().compareTo(request.minAmount()) <= 0) {
            throw new IllegalArgumentException("Max amount must be greater than min amount");
        }

        if (request.maxFee() != null && request.maxFee().compareTo(request.minFee()) < 0) {
            throw new IllegalArgumentException("Max fee must be greater than or equal to min fee");
        }

        if (request.feeType() == FeeType.PERCENTAGE) {
            if (request.rate() == null || request.rate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Rate is required for percentage fee and must be >= 0");
            }
            if (request.rate().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Rate must be <= 1");
            }
            if (request.flatAmount() != null) {
                throw new IllegalArgumentException("Flat amount must be null for percentage fee");
            }
        } else {
            if (request.flatAmount() == null || request.flatAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Flat amount is required for flat fee and must be >= 0");
            }
            if (request.rate() != null) {
                throw new IllegalArgumentException("Rate must be null for flat fee");
            }
        }
    }

    private FeeRuleResponse toResponse(FeeRule feeRule) {
        return new FeeRuleResponse(
                feeRule.getId(),
                feeRule.getOperationType(),
                feeRule.getCurrency(),
                feeRule.getFeeType(),
                feeRule.getRate(),
                feeRule.getFlatAmount(),
                feeRule.getMinAmount(),
                feeRule.getMaxAmount(),
                feeRule.getMinFee(),
                feeRule.getMaxFee(),
                feeRule.isActive(),
                feeRule.getCreatedAt(),
                feeRule.getUpdatedAt());
    }
}
