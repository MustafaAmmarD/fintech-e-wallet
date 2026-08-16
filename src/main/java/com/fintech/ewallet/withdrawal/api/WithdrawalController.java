package com.fintech.ewallet.withdrawal.api;

import com.fintech.ewallet.withdrawal.application.AgentWithdrawUseCase;
import com.fintech.ewallet.withdrawal.application.CancelWithdrawalRequestUseCase;
import com.fintech.ewallet.withdrawal.application.CreateWithdrawalRequestUseCase;
import com.fintech.ewallet.withdrawal.application.GetMyWithdrawalRequestsUseCase;
import com.fintech.ewallet.withdrawal.application.GetWithdrawalHistoryUseCase;
import com.fintech.ewallet.withdrawal.application.RedeemWithdrawalRequestUseCase;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawRequest;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawResponse;
import com.fintech.ewallet.withdrawal.application.dto.CreateWithdrawalRequestDto;
import com.fintech.ewallet.withdrawal.application.dto.RedeemWithdrawalDto;
import com.fintech.ewallet.withdrawal.application.dto.WithdrawalRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/withdrawals")
@RequiredArgsConstructor
@Tag(name = "Withdrawals", description = "User and Agent cash-out operations")
public class WithdrawalController {

    private final AgentWithdrawUseCase agentWithdrawUseCase;
    private final GetWithdrawalHistoryUseCase getWithdrawalHistoryUseCase;
    private final CreateWithdrawalRequestUseCase createWithdrawalRequestUseCase;
    private final GetMyWithdrawalRequestsUseCase getMyWithdrawalRequestsUseCase;
    private final CancelWithdrawalRequestUseCase cancelWithdrawalRequestUseCase;
    private final RedeemWithdrawalRequestUseCase redeemWithdrawalRequestUseCase;

    @PostMapping("/agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Create agent withdrawal", description = "Agent withdraws cash from a user's wallet by account number.")
    public ResponseEntity<AgentWithdrawResponse> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID agentId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate agent withdrawal")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AgentWithdrawRequest request) {

        AgentWithdrawResponse response = agentWithdrawUseCase.execute(agentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Get agent withdrawal history", description = "Returns recent withdrawals performed by the authenticated agent.")
    public ResponseEntity<List<AgentWithdrawResponse>> getHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID agentId,
            @RequestParam(defaultValue = "20") int limit) {

        List<AgentWithdrawResponse> history = getWithdrawalHistoryUseCase.execute(agentId, limit);
        return ResponseEntity.ok(history);
    }

    // --- User Withdrawal Request Endpoints ---

    @PostMapping("/request")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create withdrawal request", description = "User requests cash withdrawal and receives a 6-digit OTP code.")
    public ResponseEntity<WithdrawalRequestResponse> createRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateWithdrawalRequestDto request) {
        
        WithdrawalRequestResponse response = createWithdrawalRequestUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my withdrawal requests", description = "Returns user's withdrawal requests.")
    public ResponseEntity<List<WithdrawalRequestResponse>> getMyRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        
        List<WithdrawalRequestResponse> requests = getMyWithdrawalRequestsUseCase.execute(userId);
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/request/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel withdrawal request", description = "User cancels a pending withdrawal request.")
    public ResponseEntity<Void> cancelRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        
        cancelWithdrawalRequestUseCase.execute(userId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Agent Redeem Endpoint ---

    @PostMapping("/agent/redeem")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Redeem user withdrawal code", description = "Agent redeems a user's 6-digit OTP code and finalizes withdrawal.")
    public ResponseEntity<AgentWithdrawResponse> redeem(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID agentId,
            @Valid @RequestBody RedeemWithdrawalDto request) {
        
        AgentWithdrawResponse response = redeemWithdrawalRequestUseCase.execute(agentId, request);
        return ResponseEntity.ok(response);
    }
}
