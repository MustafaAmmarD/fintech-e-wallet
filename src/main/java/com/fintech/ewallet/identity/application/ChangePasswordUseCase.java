package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.ChangePasswordRequest;
import com.fintech.ewallet.identity.application.dto.ChangePasswordResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ChangePasswordResponse execute(UUID userId, ChangePasswordRequest request) {
        // Validate new passwords match
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Prevent reusing current password
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from your current password");
        }

        // Update password hash
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return new ChangePasswordResponse("Password changed successfully");
    }
}
