package com.fintech.ewallet.referral.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.referral.application.dto.MyReferralCodeResponse;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyReferralCodeUseCase {

    private final UserRepository userRepository;

    public MyReferralCodeResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new MyReferralCodeResponse(user.getReferralCode());
    }
}
