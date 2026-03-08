package com.fintech.ewallet.privacy.api;

import com.fintech.ewallet.privacy.application.GetPrivacySettingsUseCase;
import com.fintech.ewallet.privacy.application.UpdatePrivacySettingsUseCase;
import com.fintech.ewallet.privacy.application.dto.PrivacySettingsResponse;
import com.fintech.ewallet.privacy.application.dto.UpdatePrivacySettingsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
@Tag(name = "Privacy", description = "Privacy settings endpoints")
public class PrivacyController {

    private final GetPrivacySettingsUseCase getPrivacySettingsUseCase;
    private final UpdatePrivacySettingsUseCase updatePrivacySettingsUseCase;

    @GetMapping("/settings")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get privacy settings", description = "Returns the authenticated user's current privacy settings.")
    public ResponseEntity<PrivacySettingsResponse> getSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(getPrivacySettingsUseCase.execute(userId));
    }

    @PatchMapping("/settings")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update privacy settings", description = "Updates the authenticated user's privacy settings.")
    public ResponseEntity<PrivacySettingsResponse> updateSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdatePrivacySettingsRequest request) {
        return ResponseEntity.ok(updatePrivacySettingsUseCase.execute(userId, request));
    }
}
