package com.fintech.ewallet.privacy.application;

import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.privacy.application.dto.PrivacySettingsResponse;
import com.fintech.ewallet.privacy.application.dto.UpdatePrivacySettingsRequest;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdatePrivacySettingsUseCase {

    private final UserRepository userRepository;

    @Transactional
    public PrivacySettingsResponse execute(UUID userId, UpdatePrivacySettingsRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setShowFullName(Boolean.TRUE.equals(request.showFullName()));
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);

        return new PrivacySettingsResponse(
                savedUser.getId(),
                savedUser.isShowFullName());
    }
}
