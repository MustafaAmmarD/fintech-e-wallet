package com.fintech.ewallet.shared.privacy;

import com.fintech.ewallet.identity.domain.User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NameMaskingService {

    public String getDisplayName(User targetUser, UUID requesterId) {
        if (targetUser == null) {
            return "Unknown";
        }

        String fullName = normalizeName(targetUser.getFullName());
        if ("Unknown".equals(fullName)) {
            return fullName;
        }

        if (requesterId != null && requesterId.equals(targetUser.getId())) {
            return fullName;
        }

        if (targetUser.isShowFullName()) {
            return fullName;
        }

        return maskName(fullName);
    }

    private String normalizeName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Unknown";
        }
        return fullName.trim();
    }

    private String maskName(String fullName) {
        return Arrays.stream(fullName.split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + "***")
                .collect(Collectors.joining(" "));
    }
}
