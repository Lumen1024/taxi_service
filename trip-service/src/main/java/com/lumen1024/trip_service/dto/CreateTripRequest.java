package com.lumen1024.trip_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTripRequest(
    @NotBlank String origin,
    @NotBlank String destination
) {}