package com.lumen1024.user_service.dto;

import com.lumen1024.user_service.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull DriverStatus status
) {}
