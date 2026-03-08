package com.fintech.ewallet.bill.application;

import com.fintech.ewallet.bill.application.dto.BillPreviewRequest;
import com.fintech.ewallet.bill.application.dto.BillPreviewResponse;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillRepository;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.WalletStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreviewBillPaymentUseCase {

    private final BillRepository billRepository;
    private final WalletRepository walletRepository;

    @Value("${biller.fee.flat-amount:50}")
    private BigDecimal flatFeeAmount;

    @Transactional(readOnly = true)
    public BillPreviewResponse execute(UUID userId, BillPreviewRequest request) {
        // 1. Find Biller
        Biller biller = billRepository.findBillerByCode(request.billerCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid biller code: " + request.billerCode()));

        if (!"ACTIVE".equals(biller.getStatus())) {
            throw new IllegalArgumentException("Biller is currently inactive");
        }

        // 2. Validate Currency (Must match biller's supported currency)
        if (!biller.getSupportedCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("Biller only supports currency: " + biller.getSupportedCurrency());
        }
        Currency currency = Currency.valueOf(request.currency());

        // 3. Find User Wallet
        Wallet userWallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new IllegalArgumentException("User does not have a " + currency + " wallet"));

        if (userWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Your " + currency + " wallet is not active");
        }

        // 4. Check Balance
        BigDecimal totalDeducted = request.amount().add(flatFeeAmount);
        if (userWallet.getBalance().compareTo(totalDeducted) < 0) {
            throw new IllegalArgumentException("Insufficient funds. You need " + totalDeducted + " " + currency
                    + " (including " + flatFeeAmount + " fee)");
        }

        // 5. Return Preview
        return new BillPreviewResponse(
                biller.getName(),
                biller.getCategory(),
                request.customerAccountNumber(),
                request.amount(),
                flatFeeAmount,
                totalDeducted,
                request.currency(),
                userWallet.getBalance().subtract(totalDeducted));
    }
}
