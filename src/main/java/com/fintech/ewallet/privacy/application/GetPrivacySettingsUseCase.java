package com.fintech.ewallet.privacy.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.privacy.application.dto.PrivacySettingsResponse;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPrivacySettingsUseCase {

    private final UserRepository userRepository;

    public PrivacySettingsResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new PrivacySettingsResponse(
                user.getId(),
                user.isShowFullName());
    }
}
