package com.lumen1024.trip_service.dto;

import com.lumen1024.trip_service.entity.Trip;
import com.lumen1024.trip_service.entity.TripStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record TripResponse(
    Long id,

    Long passengerId,
    Long driverId,
    TripStatus status,

    String origin,
    String destination,

    BigDecimal price,
    Integer rating,

    Instant createdAt,
    Instant updatedAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
            trip.getId(),
            trip.getPassengerId(),
            trip.getDriverId(),
            trip.getStatus(),
            trip.getOrigin(),
            trip.getDestination(),
            trip.getPrice(),
            trip.getRating(),
            trip.getCreatedAt(),
            trip.getUpdatedAt()
        );
    }
}