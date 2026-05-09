package com.lumen1024.user_service.dto;

public record LoginResponse(
    String token,
    String role,
    Long userId
) {}
