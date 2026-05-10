package com.lumen1024.notification_service.dto;

public record TripEvent(
    Long tripId,
    String event,
    Long passengerId,
    Long driverId,
    String message,
    Long recipientId,
    String recipientType
) {}
