package com.fintech.ewallet.identity.api;

import com.fintech.ewallet.identity.application.ChangePasswordUseCase;
import com.fintech.ewallet.identity.application.GetUserProfileUseCase;
import com.fintech.ewallet.identity.application.dto.UserProfileResponse;
import com.fintech.ewallet.identity.application.LoginUseCase;
import com.fintech.ewallet.identity.application.LogoutUseCase;
import com.fintech.ewallet.identity.application.RefreshTokenUseCase;
import com.fintech.ewallet.identity.application.RegisterUserUseCase;
import com.fintech.ewallet.identity.application.dto.ChangePasswordRequest;
import com.fintech.ewallet.identity.application.dto.ChangePasswordResponse;
import com.fintech.ewallet.identity.application.dto.LoginRequest;
import com.fintech.ewallet.identity.application.dto.LoginResponse;
import com.fintech.ewallet.identity.application.dto.RefreshTokenResponse;
import com.fintech.ewallet.identity.application.dto.RegisterRequest;
import com.fintech.ewallet.identity.application.dto.RegisterResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    private final ChangePasswordUseCase changePasswordUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;

    /**
     * Get the authenticated user's own full profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(getUserProfileUseCase.execute(userId));
    }

    /**
     * Register a new user.
     * Does NOT return an access token (user must login separately).
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = registerUserUseCase.execute(request);
        return ResponseEntity.ok(response);
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
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {

        String accessToken = extractToken(authHeader);
        // Optionally extract refresh token from request body if needed
        logoutUseCase.execute(accessToken, null, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change password for the authenticated user.
     * Requires current password for verification.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(changePasswordUseCase.execute(userId, request));
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
