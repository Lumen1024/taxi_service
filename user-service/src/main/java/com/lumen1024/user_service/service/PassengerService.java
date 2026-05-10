package com.lumen1024.user_service.service;

import com.lumen1024.user_service.dto.PassengerResponse;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.PassengerRepository;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;

    public PassengerResponse getPassenger(Long id) {
        var passenger = passengerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passenger not found"));
        String email = userRepository.findByPassengerId(id)
            .map(User::getEmail)
            .orElse(null);
        return PassengerResponse.from(passenger, email);
    }
}
