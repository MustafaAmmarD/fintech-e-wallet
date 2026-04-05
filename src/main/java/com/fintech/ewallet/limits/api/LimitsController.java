package com.fintech.ewallet.limits.api;

import com.fintech.ewallet.limits.application.GetTransactionLimitsUseCase;
import com.fintech.ewallet.limits.application.dto.LimitResponse;
import com.fintech.ewallet.limits.domain.UserTier;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for viewing transaction limits.
 */
@RestController
@RequestMapping("/api/v1/limits")
@RequiredArgsConstructor
public class LimitsController {

    private final GetTransactionLimitsUseCase getTransactionLimitsUseCase;

    /**
     * GET /api/v1/limits
     * Returns all active transaction limits for the standard (BASIC) user tier.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LimitResponse>> getLimits(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        // All regular users are on BASIC tier by default
        return ResponseEntity.ok(getTransactionLimitsUseCase.execute(UserTier.BASIC));
    }
}
