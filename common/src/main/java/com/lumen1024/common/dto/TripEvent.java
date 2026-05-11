package com.lumen1024.common.dto;

import java.io.Serializable;

public record TripEvent(
    Long tripId,
    String event,
    Long passengerId,
    Long driverId,
    String message,
    Long recipientId,
    String recipientType
) implements Serializable {}