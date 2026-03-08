package com.fintech.ewallet.bill.application;

import com.fintech.ewallet.bill.application.dto.BillHistoryResponse;
import com.fintech.ewallet.bill.domain.BillPayment;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetBillHistoryUseCase {

    private final BillRepository billRepository;

    public List<BillHistoryResponse> execute(java.util.UUID userId) {
        List<BillPayment> payments = billRepository.findPaymentsByUserId(userId);

        // Optimize by fetching billers once
        Map<java.util.UUID, Biller> billers = billRepository.findAllActiveBillers().stream()
                .collect(Collectors.toMap(Biller::getId, b -> b));

        return payments.stream().map(payment -> {
            Biller biller = billers.get(payment.getBillerId());
            String billerName = biller != null ? biller.getName() : "Unknown Biller";

            return new BillHistoryResponse(
                    payment.getId(),
                    payment.getReferenceNo(),
                    billerName,
                    payment.getCustomerAccountNumber(),
                    payment.getAmount(),
                    payment.getFeeAmount(),
                    payment.getTotalDeducted(),
                    payment.getCurrency(),
                    payment.getStatus(),
                    payment.getCreatedAt());
        }).toList();
    }
}
