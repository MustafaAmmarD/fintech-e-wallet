package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.LoginRequest;
import com.fintech.ewallet.identity.application.dto.LoginResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.identity.infrastructure.security.JwtTokenProvider;
import com.fintech.ewallet.shared.exception.AccountLockedException;
import com.fintech.ewallet.shared.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case: Login with phone + password.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse execute(LoginRequest request) {
        log.debug("Login attempt for phone: {}", maskPhone(request.phoneNumber()));

        // 1. Find user by phone
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. Check if account is locked
        if (user.isLocked()) {
            log.warn("Login blocked — account locked for user: {}", user.getId());
            throw new AccountLockedException();
        }

        // 3. Check if account is active
        if (!user.isActive()) {
            log.warn("Login blocked — account not active for user: {}", user.getId());
            throw new InvalidCredentialsException();
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            log.warn("Failed login attempt #{} for user: {}", user.getFailedLoginAttempts(), user.getId());
            throw new InvalidCredentialsException();
        }

        // 5. Successful login — reset failed attempts
        user.recordSuccessfulLogin();
        userRepository.save(user);

        // 6. Generate JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

        log.info("User logged in successfully: {}", user.getId());

        // 7. Build response
        return new LoginResponse(
                accessToken,
                refreshToken,
                expiresIn,
                new LoginResponse.UserInfo(
                        user.getId(),
                        user.getFullName(),
                        user.getPhoneNumber(),
                        user.getKycStatus().name()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6)
            return "***";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
