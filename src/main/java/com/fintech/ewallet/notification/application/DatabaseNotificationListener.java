package com.fintech.ewallet.notification.application;

import com.fintech.ewallet.notification.domain.NotificationSender;
import com.fintech.ewallet.notification.domain.NotificationType;
import com.fintech.ewallet.shared.event.FinancialTransactionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Listens to shared system events and creates notifications via all active
 * channels.
 * 
 * In the future, if WhatsApp/Push modules are added, they would implement their
 * own
 * listeners or they would simply be injected here as additional
 * NotificationSenders.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseNotificationListener {

    // Spring injects all implementations of NotificationSender (e.g.,
    // CreateNotificationUseCase for DB)
    private final List<NotificationSender> notificationSenders;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFinancialTransactionCompleted(FinancialTransactionCompletedEvent event) {
        log.info("Received transaction completed event for notifications: ref={}/{}",
                event.referenceType(), event.referenceId());

        if (event.participantUserIds() == null || event.participantUserIds().isEmpty()) {
            return;
        }

        // We notify both sender and recipient differently
        for (UUID userId : event.participantUserIds()) {
            boolean isSender = userId.equals(event.initiatorId());

            try {
                notifyUser(userId, event, isSender);
            } catch (Exception ex) {
                log.error("Failed to process notification for user {}", userId, ex);
            }
        }
    }

    private boolean isExchange(String referenceType) {
        return "EXCHANGE".equalsIgnoreCase(referenceType);
    }

    private void notifyUser(UUID userId, FinancialTransactionCompletedEvent event, boolean isSender) {
        NotificationType type = resolveType(event.referenceType(), isSender);
        String title = resolveTitle(type);
        String message = buildMessage(type, event, isSender);

        // Send via ALL channels (currently only DB, future: WhatsApp/Push)
        for (NotificationSender sender : notificationSenders) {
            sender.send(userId, type, title, message, event.referenceType(), event.referenceId());
        }
    }

    private NotificationType resolveType(String referenceType, boolean isSender) {
        if (referenceType == null)
            return NotificationType.SYSTEM;

        return switch (referenceType.toUpperCase()) {
            case "TRANSFER" -> isSender ? NotificationType.TRANSFER_SENT : NotificationType.TRANSFER_RECEIVED;
            case "DEPOSIT" -> NotificationType.DEPOSIT_COMPLETED;
            case "WITHDRAWAL" -> NotificationType.WITHDRAWAL_COMPLETED;
            case "EXCHANGE" -> NotificationType.EXCHANGE_COMPLETED;
            default -> NotificationType.SYSTEM;
        };
    }

    private String resolveTitle(NotificationType type) {
        return switch (type) {
            case TRANSFER_RECEIVED -> "Transfer Received";
            case TRANSFER_SENT -> "Transfer Sent";
            case DEPOSIT_COMPLETED -> "Deposit Completed";
            case WITHDRAWAL_COMPLETED -> "Withdrawal Completed";
            case EXCHANGE_COMPLETED -> "Exchange Completed";
            default -> "System Notification";
        };
    }

    private String buildMessage(NotificationType type, FinancialTransactionCompletedEvent event, boolean isSender) {
        String amountStr = String.format("%,.2f %s", event.amount(), event.currency());

        return switch (type) {
            case TRANSFER_RECEIVED -> {
                String senderName = event.initiatorName() != null ? event.initiatorName() : "Another user";
                yield String.format("You received %s from %s", amountStr, senderName);
            }
            case TRANSFER_SENT -> String.format("You sent %s to the recipient", amountStr); // Recipient name lookup
                                                                                            // could be added to event
                                                                                            // if needed
            case DEPOSIT_COMPLETED -> String.format("%s deposited into your wallet", amountStr);
            case WITHDRAWAL_COMPLETED -> String.format("%s withdrawn from your wallet", amountStr);
            case EXCHANGE_COMPLETED -> "Currency exchange completed successfully";
            default -> "A financial transaction was completed on your account";
        };
    }
}
