package com.fintech.ewallet.deposit.api;

import com.fintech.ewallet.deposit.application.AgentDepositUseCase;
import com.fintech.ewallet.deposit.application.GetDepositHistoryUseCase;
import com.fintech.ewallet.deposit.application.dto.AgentDepositRequest;
import com.fintech.ewallet.deposit.application.dto.AgentDepositResponse;
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
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
@Tag(name = "Deposits", description = "Agent cash-in operations")
public class DepositController {

    private final AgentDepositUseCase agentDepositUseCase;
    private final GetDepositHistoryUseCase getDepositHistoryUseCase;

    @PostMapping("/agent")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Create agent deposit", description = "Agent deposits cash into a user's wallet by account number.")
    public ResponseEntity<AgentDepositResponse> deposit(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID agentId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate agent deposit")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AgentDepositRequest request) {

        AgentDepositResponse response = agentDepositUseCase.execute(agentId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Get agent deposit history", description = "Returns recent deposits performed by the authenticated agent.")
    public ResponseEntity<List<AgentDepositResponse>> getHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID agentId,
            @RequestParam(defaultValue = "20") int limit) {

        List<AgentDepositResponse> history = getDepositHistoryUseCase.execute(agentId, limit);
        return ResponseEntity.ok(history);
    }
}
