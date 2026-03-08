package com.fintech.ewallet.referral.api;

import com.fintech.ewallet.referral.application.GetMyReferralCodeUseCase;
import com.fintech.ewallet.referral.application.GetReferralHistoryUseCase;
import com.fintech.ewallet.referral.application.GetReferralStatsUseCase;
import com.fintech.ewallet.referral.application.dto.MyReferralCodeResponse;
import com.fintech.ewallet.referral.application.dto.ReferralHistoryResponse;
import com.fintech.ewallet.referral.application.dto.ReferralStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
@Tag(name = "Referrals", description = "Referral program endpoints")
public class ReferralController {

    private final GetMyReferralCodeUseCase getMyReferralCodeUseCase;
    private final GetReferralStatsUseCase getReferralStatsUseCase;
    private final GetReferralHistoryUseCase getReferralHistoryUseCase;

    @GetMapping("/my-code")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my referral code", description = "Returns the authenticated user's referral code.")
    public ResponseEntity<MyReferralCodeResponse> myCode(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(getMyReferralCodeUseCase.execute(userId));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get referral stats", description = "Returns aggregate referral statistics for authenticated user.")
    public ResponseEntity<ReferralStatsResponse> stats(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(getReferralStatsUseCase.execute(userId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get referral history", description = "Returns referral history for authenticated user.")
    public ResponseEntity<List<ReferralHistoryResponse>> history(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(getReferralHistoryUseCase.execute(userId, limit));
    }
}
