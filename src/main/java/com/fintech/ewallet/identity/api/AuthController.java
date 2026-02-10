package com.fintech.ewallet.identity.api;

import com.fintech.ewallet.identity.application.LoginUseCase;
import com.fintech.ewallet.identity.application.RegisterUserUseCase;
import com.fintech.ewallet.identity.application.dto.LoginRequest;
import com.fintech.ewallet.identity.application.dto.LoginResponse;
import com.fintech.ewallet.identity.application.dto.RegisterRequest;
import com.fintech.ewallet.identity.application.dto.RegisterResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;

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
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
