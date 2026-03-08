package com.fintech.ewallet.notification.domain;

import java.util.UUID;

/**
 * Port for sending notifications via any channel (DB, Push, WhatsApp, SMS).
 */
public interface NotificationSender {

    /**
     * Sends a notification to the specified user.
     * 
     * @param userId        The recipient user ID
     * @param type          The notification category
     * @param title         Short title (e.g. "Transfer Received")
     * @param message       Full message body
     * @param referenceType Reference to the source entity (e.g. "TRANSFER")
     * @param referenceId   ID of the source entity
     */
    void send(UUID userId, NotificationType type, String title,
            String message, String referenceType, UUID referenceId);

}
