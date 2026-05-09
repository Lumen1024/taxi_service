package com.lumen1024.user_service.dto;

import com.lumen1024.user_service.entity.Passenger;

public record PassengerResponse(
    Long id,
    String name,
    String phone,
    String email
) {
    public static PassengerResponse from(Passenger passenger, String email) {
        return new PassengerResponse(
            passenger.getId(),
            passenger.getName(),
            passenger.getPhone(),
            email
        );
    }
}
