package com.fintech.ewallet.shared.exception;

public class InvalidReferralCodeException extends DomainException {

    public InvalidReferralCodeException(String referralCode) {
        super("INVALID_REFERRAL_CODE", "Invalid referral code: " + referralCode);
    }
}
