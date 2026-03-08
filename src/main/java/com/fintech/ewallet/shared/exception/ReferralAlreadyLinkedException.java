package com.fintech.ewallet.shared.exception;

public class ReferralAlreadyLinkedException extends DomainException {

    public ReferralAlreadyLinkedException() {
        super("REFERRAL_ALREADY_LINKED", "User is already linked to a referral");
    }
}
