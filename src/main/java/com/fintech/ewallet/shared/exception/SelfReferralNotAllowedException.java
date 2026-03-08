package com.fintech.ewallet.shared.exception;

public class SelfReferralNotAllowedException extends DomainException {

    public SelfReferralNotAllowedException() {
        super("SELF_REFERRAL_NOT_ALLOWED", "Self referral is not allowed");
    }
}
