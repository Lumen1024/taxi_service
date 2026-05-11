package com.lumen1024.user_service.dto;

import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.DriverStatus;

public record DriverResponse(
    Long id,
    Long userId,
    String name,
    String phone,
    String licenseNumber,
    DriverStatus status
) {
    public static DriverResponse from(Driver driver, Long userId) {
        return new DriverResponse(
            driver.getId(),
            userId,
            driver.getName(),
            driver.getPhone(),
            driver.getLicenseNumber(),
            driver.getStatus()
        );
    }
}
