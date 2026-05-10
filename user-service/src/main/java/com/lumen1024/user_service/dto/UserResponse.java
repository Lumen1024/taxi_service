package com.lumen1024.user_service.dto;

public record UserResponse(
    Long id,
    String role,
    Long passengerId,
    Long driverId
) {}