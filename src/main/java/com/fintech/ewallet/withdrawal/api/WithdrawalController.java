package com.fintech.ewallet.withdrawal.api;

import com.fintech.ewallet.withdrawal.application.AgentWithdrawUseCase;
import com.fintech.ewallet.withdrawal.application.GetWithdrawalHistoryUseCase;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawRequest;
import com.fintech.ewallet.withdrawal.application.dto.AgentWithdrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "Withdrawals", description = "Agent cash-out operations")
public class WithdrawalController {

    private final AgentWithdrawUseCase agentWithdrawUseCase;
    private final GetWithdrawalHistoryUseCase getWithdrawalHistoryUseCase;

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
}
