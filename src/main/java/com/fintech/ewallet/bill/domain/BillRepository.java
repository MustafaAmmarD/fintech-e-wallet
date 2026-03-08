package com.fintech.ewallet.bill.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository {
    List<Biller> findAllActiveBillers();

    List<Biller> findActiveBillersByCategory(BillerCategory category);

    Optional<Biller> findBillerById(UUID id);

    Optional<Biller> findBillerByCode(String code);

    BillPayment savePayment(BillPayment payment);

    Optional<BillPayment> findPaymentById(UUID id);

    List<BillPayment> findPaymentsByUserId(UUID userId);
}
