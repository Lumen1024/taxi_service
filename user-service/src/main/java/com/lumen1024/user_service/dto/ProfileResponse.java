package com.lumen1024.user_service.dto;

public record ProfileResponse(
    Long id,
    String email,
    String role,
    Long passengerId,
    Long driverId,
    String name,
    String phone,
    String licenseNumber
) {}
