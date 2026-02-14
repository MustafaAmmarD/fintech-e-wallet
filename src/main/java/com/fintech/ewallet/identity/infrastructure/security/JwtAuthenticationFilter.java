package com.fintech.ewallet.identity.infrastructure.security;

import com.fintech.ewallet.shared.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * JWT Authentication Filter.
 * <p>
 * Intercepts every HTTP request, extracts and validates the JWT token,
 * and sets the authentication context for Spring Security.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                // Parse and validate token
                Claims claims = jwtTokenProvider.parseToken(token);
                String tokenId = claims.getId();
                String tokenType = (String) claims.get("type");

                // Only accept access tokens (not refresh tokens)
                if (!"access".equals(tokenType)) {
                    log.warn("Non-access token attempted for authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Check blacklist
                if (tokenBlacklistService.isBlacklisted(tokenId)) {
                    log.warn("Blacklisted token attempted: {}", tokenId);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract user ID
                UUID userId = UUID.fromString(claims.getSubject());

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, // Principal = User ID
                        null, // Credentials (password) not needed
                        Collections.emptyList() // Authorities (roles) - add later if needed
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User authenticated: {}", userId);
            }

        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            // Don't throw — just let the request proceed unauthenticated
            // Spring Security will reject it if the endpoint requires auth
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }
}
