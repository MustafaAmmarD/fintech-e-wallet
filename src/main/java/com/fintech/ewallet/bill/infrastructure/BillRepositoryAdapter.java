package com.fintech.ewallet.bill.infrastructure;

import com.fintech.ewallet.bill.domain.BillPayment;
import com.fintech.ewallet.bill.domain.BillRepository;
import com.fintech.ewallet.bill.domain.Biller;
import com.fintech.ewallet.bill.domain.BillerCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BillRepositoryAdapter implements BillRepository {

    private final BillerJpaRepository billerJpaRepository;
    private final BillPaymentJpaRepository billPaymentJpaRepository;

    @Override
    public List<Biller> findAllActiveBillers() {
        return billerJpaRepository.findByStatus("ACTIVE").stream()
                .map(BillMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Biller> findActiveBillersByCategory(BillerCategory category) {
        return billerJpaRepository.findByStatusAndCategory("ACTIVE", category.name()).stream()
                .map(BillMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Biller> findBillerById(UUID id) {
        return billerJpaRepository.findById(id).map(BillMapper::toDomain);
    }

    @Override
    public Optional<Biller> findBillerByCode(String code) {
        return billerJpaRepository.findByCode(code).map(BillMapper::toDomain);
    }

    @Override
    public BillPayment savePayment(BillPayment payment) {
        BillPaymentJpaEntity entity = BillMapper.toEntity(payment);
        BillPaymentJpaEntity saved = billPaymentJpaRepository.save(entity);
        return BillMapper.toDomain(saved);
    }

    @Override
    public Optional<BillPayment> findPaymentById(UUID id) {
        return billPaymentJpaRepository.findById(id).map(BillMapper::toDomain);
    }

    @Override
    public List<BillPayment> findPaymentsByUserId(UUID userId) {
        return billPaymentJpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BillMapper::toDomain)
                .collect(Collectors.toList());
    }
}
