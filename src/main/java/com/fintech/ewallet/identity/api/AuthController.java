package com.fintech.ewallet.identity.api;

import com.fintech.ewallet.identity.application.LoginUseCase;
import com.fintech.ewallet.identity.application.LogoutUseCase;
import com.fintech.ewallet.identity.application.RefreshTokenUseCase;
import com.fintech.ewallet.identity.application.RegisterUserUseCase;
import com.fintech.ewallet.identity.application.dto.LoginRequest;
import com.fintech.ewallet.identity.application.dto.LoginResponse;
import com.fintech.ewallet.identity.application.dto.RefreshTokenResponse;
import com.fintech.ewallet.identity.application.dto.RegisterRequest;
import com.fintech.ewallet.identity.application.dto.RegisterResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication endpoints (public + protected).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    /**
     * Register a new user.
     * Does NOT return an access token (user must login separately).
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with phone and password.
     * Returns JWT access and refresh tokens.
     * Phase 1.4: Now includes device binding.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        LoginResponse response = loginUseCase.execute(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token using a valid refresh token.
     * Implements token rotation (old refresh token is invalidated).
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = extractToken(authHeader);
        RefreshTokenResponse response = refreshTokenUseCase.execute(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout by blacklisting both access and refresh tokens.
     * Requires authentication.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UUID userId) {

        String accessToken = extractToken(authHeader);
        // Optionally extract refresh token from request body if needed
        logoutUseCase.execute(accessToken, null, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract token from "Bearer <token>" header.
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
