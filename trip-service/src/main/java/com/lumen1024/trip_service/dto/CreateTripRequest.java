package com.lumen1024.trip_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTripRequest(
    @NotNull Long passengerId,
    @NotBlank String origin,
    @NotBlank String destination
) {}