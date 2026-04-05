package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.UserProfileResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Use case: Get the authenticated user's own profile.
 */
@Service
@RequiredArgsConstructor
public class GetUserProfileUseCase {

    private final UserRepository userRepository;

    public UserProfileResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.getEnglishFullName(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getIdNumber(),
                user.getMaritalStatus(),
                user.getEmail(),
                user.getLanguage(),
                user.getAccountNumber(),
                user.getReferralCode(),
                user.isShowFullName(),
                user.getAccountStatus(),
                user.getKycStatus(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
