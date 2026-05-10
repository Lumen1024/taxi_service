package com.lumen1024.notification_service.dto;

import com.lumen1024.notification_service.entity.NotificationStatus;
import com.lumen1024.notification_service.entity.NotificationTask;
import java.time.Instant;

public record NotificationResponse(
    Long id,
    Long tripId,
    String recipientType,
    Long recipientId,
    String message,
    NotificationStatus status,
    int attempts,
    boolean read,
    Instant createdAt
) {
    public static NotificationResponse from(NotificationTask task) {
        return new NotificationResponse(
            task.getId(),
            task.getTripId(),
            task.getRecipientType(),
            task.getRecipientId(),
            task.getMessage(),
            task.getStatus(),
            task.getAttempts(),
            task.isRead(),
            task.getCreatedAt()
        );
    }
}
