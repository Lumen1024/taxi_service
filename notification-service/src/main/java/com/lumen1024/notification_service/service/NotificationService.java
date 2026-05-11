package com.lumen1024.notification_service.service;

import com.lumen1024.common.dto.TripEvent;
import com.lumen1024.notification_service.dto.NotificationResponse;
import com.lumen1024.notification_service.entity.Notification;
import com.lumen1024.notification_service.entity.NotificationType;
import com.lumen1024.notification_service.repository.NotificationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Map<String, NotificationType> EVENT_TYPE_MAP = Map.of(
        "TRIP_CREATED", NotificationType.TRIP_ASSIGNED,
        "TRIP_STARTED", NotificationType.TRIP_STARTED,
        "TRIP_COMPLETED", NotificationType.TRIP_COMPLETED,
        "TRIP_CANCELLED", NotificationType.TRIP_CANCELLED
    );

    private final NotificationTaskRepository notificationTaskRepository;

    @RabbitListener(queues = "trip.events")
    @Transactional
    public void onTripEvent(TripEvent event) {
        NotificationType type = EVENT_TYPE_MAP.getOrDefault(event.event(), null);
        if (type == null) {
            log.warn("Unknown trip event type: {}", event.event());
            return;
        }

        log.info("Received trip event: tripId={}, type={}, recipientId={}",
            event.tripId(), type, event.recipientId());

        Notification task = Notification.builder()
            .tripId(event.tripId())
            .recipientId(event.recipientId())
            .type(type)
            .build();

        notificationTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationTaskRepository.findByRecipientIdOrderBySentAtDesc(userId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification task = notificationTaskRepository.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!task.getRecipientId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        task.setRead(true);
        notificationTaskRepository.save(task);
        return NotificationResponse.from(task);
    }
}
