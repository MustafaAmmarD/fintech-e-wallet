package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.RefreshTokenResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.identity.infrastructure.security.JwtTokenProvider;
import com.fintech.ewallet.shared.exception.InvalidCredentialsException;
import com.fintech.ewallet.shared.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Use case: Refresh access token using a valid refresh token.
 * <p>
 * Implements refresh token rotation: invalidates the old refresh token
 * and issues a new pair (access + refresh).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public RefreshTokenResponse execute(String refreshToken) {
        try {
            // 1. Parse and validate refresh token
            Claims claims = jwtTokenProvider.parseToken(refreshToken);
            String tokenType = (String) claims.get("type");

            if (!"refresh".equals(tokenType)) {
                throw new InvalidCredentialsException();
            }

            String tokenId = claims.getId();
            UUID userId = UUID.fromString(claims.getSubject());

            // 2. Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(tokenId)) {
                log.warn("Attempted to use blacklisted refresh token: {}", tokenId);
                throw new InvalidCredentialsException();
            }

            // 3. Load user
            User user = userRepository.findById(userId)
                    .orElseThrow(InvalidCredentialsException::new);

            // 4. Check user is still active
            if (!user.isActive()) {
                log.warn("Inactive user attempted token refresh: {}", userId);
                throw new InvalidCredentialsException();
            }

            // 5. Blacklist old refresh token (rotation)
            Date expiration = claims.getExpiration();
            Duration ttl = Duration.between(Instant.now(), expiration.toInstant());
            if (!ttl.isNegative()) {
                tokenBlacklistService.blacklistToken(tokenId, ttl);
            }

            // 6. Generate new tokens
            String newAccessToken = jwtTokenProvider.generateAccessToken(user);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
            long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs();

            log.info("Token refreshed for user: {}", userId);

            return new RefreshTokenResponse(newAccessToken, newRefreshToken, expiresIn);

        } catch (Exception e) {
            log.debug("Token refresh failed: {}", e.getMessage());
            throw new InvalidCredentialsException();
        }
    }
}
