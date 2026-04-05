package com.fintech.ewallet.bill.application;

import com.fintech.ewallet.bill.application.dto.BillExecuteRequest;
import com.fintech.ewallet.bill.application.dto.BillExecuteResponse;
import com.fintech.ewallet.bill.domain.BillPayment;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillRepository;
import com.fintech.ewallet.notification.domain.NotificationSender;
import com.fintech.ewallet.notification.domain.NotificationType;
import com.fintech.ewallet.wallet.domain.SystemWallets;
import com.fintech.ewallet.wallet.domain.Currency;
import com.fintech.ewallet.wallet.domain.Wallet;
import com.fintech.ewallet.wallet.domain.WalletRepository;
import com.fintech.ewallet.wallet.domain.ReferenceType;
import com.fintech.ewallet.wallet.application.RecordLedgerEntryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecuteBillPaymentUseCase {

    private final BillRepository billRepository;
    private final WalletRepository walletRepository;
    private final RecordLedgerEntryUseCase recordLedgerEntryUseCase;
    private final MockBillerService mockBillerService;
    private final NotificationSender notificationSender;

    @Value("${biller.fee.flat-amount:50}")
    private BigDecimal flatFeeAmount;

    @Transactional
    public BillExecuteResponse execute(UUID userId, BillExecuteRequest request) {
        // 1. Validate Payment details (similar to preview)
        Biller biller = billRepository.findBillerByCode(request.billerCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid biller code"));

        if (!"ACTIVE".equals(biller.getStatus())) {
            throw new IllegalArgumentException("Biller is currently inactive");
        }

        Currency currency = Currency.valueOf(request.currency());
        if (!biller.getSupportedCurrency().equals(currency.name())) {
            throw new IllegalArgumentException("Biller only supports " + biller.getSupportedCurrency());
        }

        Wallet userWallet = walletRepository.findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() -> new IllegalArgumentException("User wallet not found"));

        BigDecimal totalDeducted = request.amount().add(flatFeeAmount);

        // 2. Generate a Reference Number
        String referenceNo = generateReference();
        UUID transactionId = UUID.randomUUID();

        // 3. Record Ledger Entries (Debit user, Credit biller, Credit fee)
        // This method also locks the wallets and guarantees zero-sum
        try {
            recordLedgerEntryUseCase.recordTransferWithFee(
                    userWallet.getId(),
                    biller.getWalletId(),
                    request.amount(),
                    SystemWallets.getFeeWallet(currency),
                    flatFeeAmount,
                    ReferenceType.BILL_PAYMENT,
                    transactionId,
                    "Bill Payment: " + biller.getName() + " - " + request.customerAccountNumber(),
                    "دفع فاتورة: " + (biller.getNameAr() != null ? biller.getNameAr() : biller.getName()) + " - " + request.customerAccountNumber());
        } catch (Exception e) {
            log.error("Failed to process ledger entries for bill payment: {}", e.getMessage());
            throw new IllegalStateException("Payment failed: " + M(e.getMessage()));
        }

        // 4. Contact External Biller API
        boolean isSuccess = mockBillerService.processPayment(biller.getCode(), request.customerAccountNumber(),
                request.amount());

        if (!isSuccess) {
            // Throwing an exception rolls back the transaction, undoing the ledger entries
            // seamlessly
            throw new IllegalStateException("External biller rejected the payment. Your money has not been deducted.");
        }

        // 5. Create Bill Payment Record
        BillPayment billPayment = BillPayment.builder()
                .id(UUID.randomUUID())
                .referenceNo(referenceNo)
                .userId(userId)
                .billerId(biller.getId())
                .customerAccountNumber(request.customerAccountNumber())
                .amount(request.amount())
                .feeAmount(flatFeeAmount)
                .totalDeducted(totalDeducted)
                .currency(currency.name())
                .status("COMPLETED")
                .transactionId(transactionId)
                .createdAt(Instant.now())
                .build();

        billRepository.savePayment(billPayment);

        // 6. Send Notification
        notificationSender.send(
                userId,
                NotificationType.BILL_PAYMENT_COMPLETED,
                "Bill Payment Successful",
                String.format("You have successfully paid %s %s to %s (Account: %s). Fee: %s %s. Reference: %s",
                        request.amount(), currency.name(), biller.getName(), request.customerAccountNumber(),
                        flatFeeAmount, currency.name(), referenceNo),
                "BILL_PAYMENT",
                billPayment.getId());

        return new BillExecuteResponse(
                billPayment.getId(),
                billPayment.getReferenceNo(),
                biller.getName(),
                biller.getCategory() != null ? biller.getCategory().name() : null,
                billPayment.getCustomerAccountNumber(),
                billPayment.getAmount(),
                billPayment.getFeeAmount(),
                billPayment.getTotalDeducted(),
                billPayment.getCurrency(),
                billPayment.getStatus(),
                billPayment.getCreatedAt());
    }

    private String generateReference() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault())
                .format(Instant.now());
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "BP-" + datePart + "-" + randomPart;
    }

    private String M(String message) {
        return message;
    }
}
