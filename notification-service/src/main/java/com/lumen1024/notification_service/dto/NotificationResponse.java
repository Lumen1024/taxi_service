package com.lumen1024.notification_service.dto;

import com.lumen1024.notification_service.entity.NotificationType;
import com.lumen1024.notification_service.entity.Notification;
import java.time.Instant;

public record NotificationResponse(
    Long id,
    Long tripId,
    Long recipientId,
    NotificationType type,
    boolean read,
    Instant sentAt
) {
    public static NotificationResponse from(Notification task) {
        return new NotificationResponse(
            task.getId(),
            task.getTripId(),
            task.getRecipientId(),
            task.getType(),
            task.isRead(),
            task.getSentAt()
        );
    }
}
