package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.referral.domain.Referral;
import com.fintech.ewallet.referral.domain.ReferralRepository;
import com.fintech.ewallet.shared.exception.InvalidReferralCodeException;
import com.fintech.ewallet.shared.exception.ReferralAlreadyLinkedException;
import com.fintech.ewallet.shared.exception.SelfReferralNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkReferralUseCase {

    private final UserRepository userRepository;
    private final ReferralRepository referralRepository;

    @Transactional
    public void execute(UUID refereeId, String providedReferralCode) {
        String referralCode = normalize(providedReferralCode);
        if (referralCode == null) {
            return;
        }

        if (referralRepository.findByRefereeId(refereeId).isPresent()) {
            throw new ReferralAlreadyLinkedException();
        }

        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new InvalidReferralCodeException(referralCode));

        if (referrer.getId().equals(refereeId)) {
            throw new SelfReferralNotAllowedException();
        }

        Referral referral = Referral.create(
                referrer.getId(),
                refereeId,
                referralCode);
        referralRepository.save(referral);
    }

    private String normalize(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
