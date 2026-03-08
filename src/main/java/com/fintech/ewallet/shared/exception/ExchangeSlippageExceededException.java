package com.fintech.ewallet.shared.exception;

import java.math.BigDecimal;

public class ExchangeSlippageExceededException extends DomainException {

    public ExchangeSlippageExceededException(BigDecimal slippageBps, BigDecimal maxAllowedBps) {
        super(
                "EXCHANGE_SLIPPAGE_EXCEEDED",
                "Rate changed too much. Slippage "
                        + slippageBps.toPlainString()
                        + " bps exceeds max allowed "
                        + maxAllowedBps.toPlainString()
                        + " bps.");
    }
}
