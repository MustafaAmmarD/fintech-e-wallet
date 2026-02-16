package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.RegisterRequest;
import com.fintech.ewallet.identity.application.dto.RegisterResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.shared.exception.PhoneAlreadyExistsException;
import com.fintech.ewallet.shared.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Register a new user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.fintech.ewallet.wallet.application.CreateWalletUseCase createWalletUseCase;

    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        log.info("Registering new user with phone: {}", maskPhone(request.phoneNumber()));

        // 1. Check phone is not already registered
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new PhoneAlreadyExistsException(request.phoneNumber());
        }

        // 2. Hash password
        String passwordHash = passwordEncoder.encode(request.password());

        // 3. Generate referral code
        String referralCode = generateReferralCode();

        // 4. Create domain object
        User user = User.createNew(
                request.phoneNumber(),
                request.fullName(),
                passwordHash,
                request.email(),
                request.language(),
                referralCode);

        // 5. Persist
        User savedUser = userRepository.save(user);

        log.info("User registered successfully: {}", savedUser.getId());

        // 6. Create default wallets (YER, SAR, USD)
        // Note: This runs in the same transaction, so if it fails, user creation rolls
        // back.
        createWalletUseCase.createWalletsForUser(savedUser.getId());

        // 7. Return response (no JWT token — user must login separately)
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getPhoneNumber(),
                savedUser.getFullName(),
                savedUser.getReferralCode(),
                "Registration successful. Please login to continue.");
    }

    /**
     * Generate a unique 8-character referral code.
     */
    private String generateReferralCode() {
        return IdGenerator.newCompactId().substring(0, 8).toUpperCase();
    }

    /**
     * Mask phone number for logging (security).
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
