package com.lumen1024.user_service.dto;

import com.lumen1024.user_service.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
    @NotBlank String email,
    @NotBlank String password,
    @NotNull UserRole role,
    @NotBlank String name,
    @NotBlank String phone,
    String licenseNumber
) {}
