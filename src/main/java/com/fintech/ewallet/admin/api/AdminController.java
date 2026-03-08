package com.fintech.ewallet.admin.api;

import com.fintech.ewallet.admin.application.FreezeWalletUseCase;
import com.fintech.ewallet.admin.application.GetAdminTransactionsUseCase;
import com.fintech.ewallet.admin.application.GetSystemStatsUseCase;
import com.fintech.ewallet.admin.application.GetSystemStatsUseCase.SystemStatsResponse;
import com.fintech.ewallet.admin.application.GetUserFullProfileUseCase;
import com.fintech.ewallet.admin.application.SearchUsersUseCase;
import com.fintech.ewallet.admin.application.UnfreezeWalletUseCase;
import com.fintech.ewallet.admin.application.dto.FreezeWalletRequest;
import com.fintech.ewallet.admin.application.dto.UnfreezeWalletRequest;
import com.fintech.ewallet.admin.application.dto.UserFullProfileResponse;
import com.fintech.ewallet.admin.application.dto.WalletActionResponse;
import com.fintech.ewallet.exchange.application.SetExchangeRateUseCase;
import com.fintech.ewallet.exchange.application.dto.SetExchangeRateRequest;
import com.fintech.ewallet.exchange.application.dto.SetExchangeRateResponse;
import com.fintech.ewallet.fee.application.CreateFeeRuleUseCase;
import com.fintech.ewallet.fee.application.GetFeeRulesUseCase;
import com.fintech.ewallet.fee.application.dto.CreateFeeRuleRequest;
import com.fintech.ewallet.fee.application.dto.FeeRuleResponse;
import com.fintech.ewallet.fee.domain.FeeOperation;
import com.fintech.ewallet.identity.application.PromoteToAgentUseCase;
import com.fintech.ewallet.identity.application.dto.PromoteToAgentResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.kyc.application.ApproveKycAccountUseCase;
import com.fintech.ewallet.kyc.application.ApproveKycDocumentUseCase;
import com.fintech.ewallet.kyc.application.GetKycAccountDetailsUseCase;
import com.fintech.ewallet.kyc.application.ListAllKycAccountsUseCase;
import com.fintech.ewallet.kyc.application.ListPendingKycAccountsUseCase;
import com.fintech.ewallet.kyc.application.PendKycAccountUseCase;
import com.fintech.ewallet.kyc.application.dto.AdminKycAccountDetailsResponse;
import com.fintech.ewallet.kyc.application.dto.AdminKycAccountSummaryResponse;
import com.fintech.ewallet.kyc.application.dto.ApproveKycAccountResponse;
import com.fintech.ewallet.kyc.application.dto.ApproveKycDocumentResponse;
import com.fintech.ewallet.kyc.application.dto.PendKycAccountResponse;
import com.fintech.ewallet.wallet.application.dto.TransactionResponse;
import com.fintech.ewallet.wallet.domain.Currency;
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

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Centralized Admin Operations")
public class AdminController {

    // --- New Phase 4 Admin Use Cases ---
    private final FreezeWalletUseCase freezeWalletUseCase;
    private final UnfreezeWalletUseCase unfreezeWalletUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final GetUserFullProfileUseCase getUserFullProfileUseCase;
    private final GetAdminTransactionsUseCase getAdminTransactionsUseCase;
    private final GetSystemStatsUseCase getSystemStatsUseCase;

    // --- Existing Admin Use Cases (from other domains) ---
    private final PromoteToAgentUseCase promoteToAgentUseCase;
    private final SetExchangeRateUseCase setExchangeRateUseCase;
    private final GetFeeRulesUseCase getFeeRulesUseCase;
    private final CreateFeeRuleUseCase createFeeRuleUseCase;
    private final ListPendingKycAccountsUseCase listPendingKycAccountsUseCase;
    private final ListAllKycAccountsUseCase listAllKycAccountsUseCase;
    private final GetKycAccountDetailsUseCase getKycAccountDetailsUseCase;
    private final ApproveKycAccountUseCase approveKycAccountUseCase;
    private final PendKycAccountUseCase pendKycAccountUseCase;
    private final ApproveKycDocumentUseCase approveKycDocumentUseCase;

    // ==========================================
    // 1. Wallets (Freeze / Unfreeze)
    // ==========================================

    @PostMapping("/wallets/{walletId}/freeze")
    public ResponseEntity<WalletActionResponse> freezeWallet(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID walletId,
            @Valid @RequestBody FreezeWalletRequest request) {
        return ResponseEntity.ok(freezeWalletUseCase.execute(adminId, walletId, request));
    }

    @PostMapping("/wallets/{walletId}/unfreeze")
    public ResponseEntity<WalletActionResponse> unfreezeWallet(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID walletId,
            @Valid @RequestBody UnfreezeWalletRequest request) {
        return ResponseEntity.ok(unfreezeWalletUseCase.execute(adminId, walletId, request));
    }

    // ==========================================
    // 2. Users (Search & Profile)
    // ==========================================

    @GetMapping("/users/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam(required = false, defaultValue = "") String q) {
        return ResponseEntity.ok(searchUsersUseCase.execute(q));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<UserFullProfileResponse> getUserFullProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(getUserFullProfileUseCase.execute(userId));
    }

    @PostMapping("/users/{userId}/promote-agent")
    public ResponseEntity<PromoteToAgentResponse> promoteToAgent(@PathVariable UUID userId) {
        return ResponseEntity.ok(promoteToAgentUseCase.execute(userId));
    }

    // ==========================================
    // 3. Transactions & System Stats
    // ==========================================

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(getAdminTransactionsUseCase.execute(userId, Math.min(limit, 100)));
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<SystemStatsResponse> getStatsSummary() {
        return ResponseEntity.ok(getSystemStatsUseCase.execute());
    }

    // ==========================================
    // 4. Exchange Rates
    // ==========================================

    @PostMapping("/exchange-rates")
    public ResponseEntity<SetExchangeRateResponse> setExchangeRate(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @Valid @RequestBody SetExchangeRateRequest request) {
        return ResponseEntity.ok(setExchangeRateUseCase.execute(adminId, request));
    }

    // ==========================================
    // 5. Fee Rules
    // ==========================================

    @GetMapping("/fees/rules")
    public ResponseEntity<List<FeeRuleResponse>> getFeeRules(
            @RequestParam FeeOperation operationType,
            @RequestParam Currency currency) {
        return ResponseEntity.ok(getFeeRulesUseCase.execute(operationType, currency));
    }

    @PostMapping("/fees/rules")
    public ResponseEntity<FeeRuleResponse> createFeeRule(@Valid @RequestBody CreateFeeRuleRequest request) {
        return ResponseEntity.ok(createFeeRuleUseCase.execute(request));
    }

    // ==========================================
    // 6. KYC Management
    // ==========================================

    @GetMapping("/kyc/accounts/pending")
    public ResponseEntity<List<AdminKycAccountSummaryResponse>> getPendingKycAccounts() {
        return ResponseEntity.ok(listPendingKycAccountsUseCase.execute());
    }

    @GetMapping("/kyc/accounts")
    public ResponseEntity<List<AdminKycAccountSummaryResponse>> getAllKycAccounts() {
        return ResponseEntity.ok(listAllKycAccountsUseCase.execute());
    }

    @GetMapping("/kyc/accounts/{userId}")
    public ResponseEntity<AdminKycAccountDetailsResponse> getKycAccountDetails(@PathVariable UUID userId) {
        return ResponseEntity.ok(getKycAccountDetailsUseCase.execute(userId));
    }

    @PostMapping("/kyc/accounts/{userId}/approve")
    public ResponseEntity<ApproveKycAccountResponse> approveKycAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(approveKycAccountUseCase.execute(userId, adminId));
    }

    @PostMapping("/kyc/accounts/{userId}/pend")
    public ResponseEntity<PendKycAccountResponse> pendKycAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(pendKycAccountUseCase.execute(userId, adminId));
    }

    @PostMapping("/kyc/documents/{documentId}/approve")
    public ResponseEntity<ApproveKycDocumentResponse> approveKycDocument(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID adminId,
            @PathVariable UUID documentId) {
        return ResponseEntity.ok(approveKycDocumentUseCase.execute(documentId, adminId));
    }
}
