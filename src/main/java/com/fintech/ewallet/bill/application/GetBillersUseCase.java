package com.fintech.ewallet.bill.application;

import com.fintech.ewallet.bill.application.dto.BillerResponse;
import com.fintech.ewallet.bill.domain.BillerCategory;
import com.fintech.ewallet.bill.domain.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetBillersUseCase {

    private final BillRepository billRepository;

    public List<BillerResponse> execute(BillerCategory category) {
        if (category != null) {
            return billRepository.findActiveBillersByCategory(category)
                    .stream()
                    .map(BillerResponse::fromEntity)
                    .toList();
        }
        return billRepository.findAllActiveBillers()
                .stream()
                .map(BillerResponse::fromEntity)
                .toList();
    }
}
