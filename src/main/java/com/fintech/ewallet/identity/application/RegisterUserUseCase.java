package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.RegisterRequest;
import com.fintech.ewallet.identity.application.dto.RegisterResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.referral.application.LinkReferralUseCase;
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
    private final LinkReferralUseCase linkReferralUseCase;

    @Transactional
    public RegisterResponse execute(RegisterRequest request) {
        log.info("Registering new user with phone: {}", maskPhone(request.phoneNumber()));

        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new PhoneAlreadyExistsException(request.phoneNumber());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        String referralCode = generateReferralCode();

        User user = User.createNew(
                request.phoneNumber(),
                request.fullName(),
                passwordHash,
                request.email(),
                request.language(),
                referralCode,
                request.englishFullName(),
                request.gender(),
                request.dateOfBirth(),
                request.idNumber(),
                request.maritalStatus());

        User savedUser = userRepository.save(user);
        linkReferralUseCase.execute(savedUser.getId(), request.referralCode());
        log.info("User registered successfully: {}", savedUser.getId());

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getPhoneNumber(),
                savedUser.getFullName(),
                savedUser.getAccountNumber(),
                "User registered successfully. Complete KYC and admin approval to activate wallets.");
    }

    private String generateReferralCode() {
        return IdGenerator.newCompactId().substring(0, 8).toUpperCase();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
