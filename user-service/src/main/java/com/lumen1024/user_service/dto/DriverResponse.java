package com.lumen1024.user_service.dto;

import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.DriverStatus;

public record DriverResponse(
    Long id,
    String name,
    String phone,
    String licenseNumber,
    DriverStatus status
) {
    public static DriverResponse from(Driver driver) {
        return new DriverResponse(
            driver.getId(),
            driver.getName(),
            driver.getPhone(),
            driver.getLicenseNumber(),
            driver.getStatus()
        );
    }
}
