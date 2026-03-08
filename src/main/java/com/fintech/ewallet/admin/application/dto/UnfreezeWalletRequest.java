package com.fintech.ewallet.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnfreezeWalletRequest {
    @NotBlank(message = "Reason for unfreezing wallet must be provided")
    private String reason;
}
