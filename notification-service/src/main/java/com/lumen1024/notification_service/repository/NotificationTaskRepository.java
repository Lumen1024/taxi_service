package com.lumen1024.notification_service.repository;

import com.lumen1024.notification_service.entity.NotificationStatus;
import com.lumen1024.notification_service.entity.NotificationTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    List<NotificationTask> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM NotificationTask t WHERE t.status = :status ORDER BY t.createdAt ASC LIMIT 1")
    Optional<NotificationTask> findNextPending(@Param("status") NotificationStatus status);
}
