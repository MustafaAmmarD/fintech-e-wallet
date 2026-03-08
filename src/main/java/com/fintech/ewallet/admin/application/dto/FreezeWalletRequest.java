package com.fintech.ewallet.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreezeWalletRequest {
    @NotBlank(message = "Reason for freezing wallet must be provided")
    private String reason;
}
