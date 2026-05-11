package com.lumen1024.notification_service.repository;

import com.lumen1024.notification_service.entity.NotificationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByRecipientIdOrderBySentAtDesc(Long recipientId);
}
