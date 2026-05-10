package com.lumen1024.notification_service.controller;

import com.lumen1024.notification_service.dto.NotificationResponse;
import com.lumen1024.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NotificationResponse> markAsRead(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
