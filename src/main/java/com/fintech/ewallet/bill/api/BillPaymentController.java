package com.fintech.ewallet.bill.api;

import com.fintech.ewallet.bill.application.ExecuteBillPaymentUseCase;
import com.fintech.ewallet.bill.application.GetBillHistoryUseCase;
import com.fintech.ewallet.bill.application.GetBillersUseCase;
import com.fintech.ewallet.bill.application.PreviewBillPaymentUseCase;
import com.fintech.ewallet.bill.application.dto.BillExecuteRequest;
import com.fintech.ewallet.bill.application.dto.BillExecuteResponse;
import com.fintech.ewallet.bill.application.dto.BillHistoryResponse;
import com.fintech.ewallet.bill.application.dto.BillPreviewRequest;
import com.fintech.ewallet.bill.application.dto.BillPreviewResponse;
import com.fintech.ewallet.bill.application.dto.BillerResponse;
import com.fintech.ewallet.bill.domain.BillerCategory;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillPaymentController {

    private final GetBillersUseCase getBillersUseCase;
    private final PreviewBillPaymentUseCase previewBillPaymentUseCase;
    private final ExecuteBillPaymentUseCase executeBillPaymentUseCase;
    private final GetBillHistoryUseCase getBillHistoryUseCase;

    /**
     * List all active billers. Can optionally be filtered by category (e.g.,
     * TELECOM).
     */
    @GetMapping("/billers")
    public ResponseEntity<List<BillerResponse>> getBillers(
            @RequestParam(required = false) BillerCategory category) {
        return ResponseEntity.ok(getBillersUseCase.execute(category));
    }

    /**
     * Preview a bill payment before executing. Calculates fees and checks the
     * user's wallet balance.
     */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BillPreviewResponse> previewBillPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody BillPreviewRequest request) {

        BillPreviewResponse response = previewBillPaymentUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute a bill payment. This atomic operation deducts funds, records ledger
     * entries, and contacts the biller.
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BillExecuteResponse> executeBillPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody BillExecuteRequest request) {

        BillExecuteResponse response = executeBillPaymentUseCase.execute(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * View history of bill payments made by the authenticated user.
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<BillHistoryResponse>> getBillHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {

        return ResponseEntity.ok(getBillHistoryUseCase.execute(userId));
    }
}
