package com.lumen1024.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notification_tasks", indexes = {
    @Index(name = "idx_notif_recipient", columnList = "recipientId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    @Column(nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        sentAt = Instant.now();
    }
}
