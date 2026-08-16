package com.fintech.ewallet.wallet.api;

import com.fintech.ewallet.wallet.application.CancelTransferUseCase;
import com.fintech.ewallet.wallet.application.ExecuteTransferUseCase;
import com.fintech.ewallet.wallet.application.GetTransferDetailUseCase;
import com.fintech.ewallet.wallet.application.GetTransferHistoryUseCase;
import com.fintech.ewallet.wallet.application.PreviewTransferUseCase;
import com.fintech.ewallet.wallet.application.ReceiveTransferUseCase;
import com.fintech.ewallet.wallet.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for P2P transfer operations.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "P2P Transfer endpoints")
public class TransferController {

    private final PreviewTransferUseCase previewTransferUseCase;
    private final ExecuteTransferUseCase executeTransferUseCase;
    private final GetTransferDetailUseCase getTransferDetailUseCase;
    private final GetTransferHistoryUseCase getTransferHistoryUseCase;
    private final ReceiveTransferUseCase receiveTransferUseCase;
    private final CancelTransferUseCase cancelTransferUseCase;

    /**
     * POST /api/v1/transfers/preview
     * Validate and show a preview of the transfer (without executing it).
     */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Preview a P2P transfer", description = "Validates the transfer and returns fee breakdown without moving money.")
    public ResponseEntity<TransferPreviewResponse> preview(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody TransferPreviewRequest request) {
        TransferPreviewResponse response = previewTransferUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/transfers/execute
     * Execute a confirmed P2P transfer.
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Execute a P2P transfer", description = "Re-validates and executes the transfer, creating ledger entries and a transfer record.")
    public ResponseEntity<ExecuteTransferResponse> execute(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate transfer execution")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ExecuteTransferRequest request) {
        ExecuteTransferResponse response = executeTransferUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/transfers/{id}
     * Get details of a specific transfer.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get transfer details", description = "Returns details of a specific transfer. Only visible to sender or recipient.")
    public ResponseEntity<TransferDetailResponse> getTransferDetail(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        TransferDetailResponse response = getTransferDetailUseCase.execute(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/transfers/history?limit=20
     * List the authenticated user's transfer history (sent + received).
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get transfer history", description = "Lists all transfers (sent and received) for the authenticated user.")
    public ResponseEntity<List<TransferDetailResponse>> getTransferHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<TransferDetailResponse> history = getTransferHistoryUseCase.execute(userId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/v1/transfers/receive
     * Claim a pending transfer using its transfer number and process number.
     */
    @PostMapping("/receive")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Receive a pending transfer", description = "Claim a pending transfer using its transfer number.")
    public ResponseEntity<Void> receive(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate requests")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReceiveTransferRequest request) {
        receiveTransferUseCase.execute(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/transfers/cancel
     * Cancel a pending transfer that the user sent.
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel a pending transfer", description = "Cancel a pending transfer that has not been claimed yet.")
    public ResponseEntity<Void> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", required = true, description = "Unique key to prevent duplicate requests")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CancelTransferRequest request) {
        cancelTransferUseCase.execute(userId, request);
        return ResponseEntity.ok().build();
    }
}
