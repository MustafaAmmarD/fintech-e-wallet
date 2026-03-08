package com.fintech.ewallet.notification.api;

import com.fintech.ewallet.notification.application.GetNotificationsUseCase;
import com.fintech.ewallet.notification.application.GetUnreadCountUseCase;
import com.fintech.ewallet.notification.application.MarkAllReadUseCase;
import com.fintech.ewallet.notification.application.MarkReadUseCase;
import com.fintech.ewallet.notification.application.dto.NotificationResponse;
import com.fintech.ewallet.notification.application.dto.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification endpoints")
public class NotificationController {

    private final GetNotificationsUseCase getNotificationsUseCase;
    private final GetUnreadCountUseCase getUnreadCountUseCase;
    private final MarkReadUseCase markReadUseCase;
    private final MarkAllReadUseCase markAllReadUseCase;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List notifications", description = "Returns notifications for authenticated user ordered by newest first.")
    public ResponseEntity<List<NotificationResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(getNotificationsUseCase.execute(userId, limit));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get unread count", description = "Returns unread notifications count for authenticated user.")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(getUnreadCountUseCase.execute(userId));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark one notification as read", description = "Marks a specific notification as read for authenticated user.")
    public ResponseEntity<NotificationResponse> markRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(markReadUseCase.execute(userId, id));
    }

    @PostMapping("/mark-all-read")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for authenticated user.")
    public ResponseEntity<UnreadCountResponse> markAllRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(markAllReadUseCase.execute(userId));
    }
}
