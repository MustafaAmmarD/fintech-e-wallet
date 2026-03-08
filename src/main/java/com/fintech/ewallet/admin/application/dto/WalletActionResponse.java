package com.fintech.ewallet.admin.application.dto;

import java.util.UUID;

public record WalletActionResponse(UUID walletId, String status, String message) {
}
