package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.infrastructure.security.JwtTokenProvider;
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
 * Use case: Logout by blacklisting the user's tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Logout by blacklisting both access and refresh tokens.
     *
     * @param accessToken  The access token to invalidate
     * @param refreshToken The refresh token to invalidate (may be null)
     */
    public void execute(String accessToken, String refreshToken, UUID userId) {
        // Blacklist access token
        if (accessToken != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(accessToken);
                String tokenId = claims.getId();
                Date expiration = claims.getExpiration();
                Duration ttl = Duration.between(Instant.now(), expiration.toInstant());

                if (!ttl.isNegative()) {
                    tokenBlacklistService.blacklistToken(tokenId, ttl);
                }
            } catch (Exception e) {
                log.debug("Failed to blacklist access token: {}", e.getMessage());
            }
        }

        // Blacklist refresh token
        if (refreshToken != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(refreshToken);
                String tokenId = claims.getId();
                Date expiration = claims.getExpiration();
                Duration ttl = Duration.between(Instant.now(), expiration.toInstant());

                if (!ttl.isNegative()) {
                    tokenBlacklistService.blacklistToken(tokenId, ttl);
                }
            } catch (Exception e) {
                log.debug("Failed to blacklist refresh token: {}", e.getMessage());
            }
        }

        log.info("User logged out: {}", userId);
    }
}
