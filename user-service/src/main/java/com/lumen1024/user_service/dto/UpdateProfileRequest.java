package com.lumen1024.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
    @NotBlank String name,
    @NotBlank String phone
) {}
