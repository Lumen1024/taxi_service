package com.lumen1024.notification_service.service;

import com.lumen1024.common.dto.TripEvent;
import com.lumen1024.notification_service.dto.NotificationResponse;
import com.lumen1024.notification_service.entity.NotificationStatus;
import com.lumen1024.notification_service.entity.NotificationTask;
import com.lumen1024.notification_service.repository.NotificationTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationTaskRepository notificationTaskRepository;

    @RabbitListener(queues = "trip.events")
    @Transactional
    public void onTripEvent(TripEvent event) {
        log.info("Received trip event: tripId={}, event={}, recipientId={}",
            event.tripId(), event.event(), event.recipientId());

        NotificationTask task = NotificationTask.builder()
            .tripId(event.tripId())
            .recipientType(event.recipientType())
            .recipientId(event.recipientId())
            .message(event.message())
            .status(NotificationStatus.PENDING)
            .build();

        notificationTaskRepository.save(task);
        log.info("Notification task created: id={}", task.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationTaskRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(NotificationResponse::from)
            .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        NotificationTask task = notificationTaskRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!task.getRecipientId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        task.setRead(true);
        notificationTaskRepository.save(task);
        return NotificationResponse.from(task);
    }
}
