package com.lumen1024.trip_service.dto;

import com.lumen1024.trip_service.entity.TripStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull TripStatus status
) {}