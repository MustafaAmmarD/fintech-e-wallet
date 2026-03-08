package com.fintech.ewallet.bill.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Simulates integration with an external biller API (like Yemen Mobile,
 * SabaFon, etc.)
 */
@Service
@Slf4j
public class MockBillerService {

    private final int failureRatePercentage;
    private final Random random = new Random();

    public MockBillerService(@Value("${biller.mock.failure-rate:0}") int failureRatePercentage) {
        this.failureRatePercentage = failureRatePercentage;
        log.info("MockBillerService initialized with failure rate: {}%", failureRatePercentage);
    }

    /**
     * Simulates processing a bill payment with the external provider.
     * 
     * @param billerCode            The external code of the biller.
     * @param customerAccountNumber The account/phone number to pay/top-up.
     * @param amount                The amount to pay.
     * @return true if successful, false if the external provider rejected it.
     */
    public boolean processPayment(String billerCode, String customerAccountNumber, java.math.BigDecimal amount) {
        log.info("Sending payment request to Biller: {} for Account: {}, Amount: {}", billerCode, customerAccountNumber,
                amount);

        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (failureRatePercentage > 0) {
            int chance = random.nextInt(100); // 0 to 99
            if (chance < failureRatePercentage) {
                log.warn("MockBillerService: Simulated external provider failure for {}", billerCode);
                return false; // Simulated failure
            }
        }

        log.info("MockBillerService: Payment successful for {}", billerCode);
        return true; // Success
    }
}
