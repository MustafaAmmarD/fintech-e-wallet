package com.fintech.ewallet.fee.application;

import com.fintech.ewallet.fee.application.dto.FeeRuleResponse;
import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.fee.domain.FeeRule;
import com.fintech.ewallet.fee.domain.FeeRuleRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetFeeRulesUseCase {

    private final FeeRuleRepository feeRuleRepository;

    public List<FeeRuleResponse> execute(FeeOperation operationType, Currency currency) {
        return feeRuleRepository.findAllByOperationAndCurrency(operationType, currency)
                .stream()
                .map(this::toResponse)
                .toList();
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
