package com.fintech.ewallet.identity.application;

import com.fintech.ewallet.identity.application.dto.PromoteToAgentResponse;
import com.fintech.ewallet.identity.domain.User;
import com.fintech.ewallet.identity.domain.UserRepository;
import com.fintech.ewallet.identity.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Use case: promote a regular user to AGENT role.
 */
@Service
@RequiredArgsConstructor
public class PromoteToAgentUseCase {

    private final UserRepository userRepository;

    @Transactional
    public PromoteToAgentResponse execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserRole currentRole = user.getRole() == null ? UserRole.USER : user.getRole();

        if (currentRole == UserRole.ADMIN) {
            throw new IllegalStateException("Cannot promote an ADMIN user to AGENT");
        }

        if (currentRole == UserRole.AGENT) {
            return new PromoteToAgentResponse(
                    user.getId(),
                    user.getFullName(),
                    UserRole.AGENT.name(),
                    "User is already an agent");
        }

        user.setRole(UserRole.AGENT);
        user.setUpdatedAt(Instant.now());
        User savedUser = userRepository.save(user);

        return new PromoteToAgentResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getRole() != null ? savedUser.getRole().name() : UserRole.AGENT.name(),
                "User promoted to agent successfully");
    }
}
