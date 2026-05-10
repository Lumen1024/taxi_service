package com.lumen1024.user_service.service;

import com.lumen1024.user_service.dto.LoginRequest;
import com.lumen1024.user_service.dto.LoginResponse;
import com.lumen1024.user_service.dto.RegisterRequest;
import com.lumen1024.common.security.JwtService;
import com.lumen1024.user_service.entity.*;
import com.lumen1024.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Long passengerId = null;
        Long driverId = null;

        if (request.role() == UserRole.PASSENGER) {
            Passenger passenger = Passenger.builder()
                .name(request.name())
                .phone(request.phone())
                .build();
            passenger = passengerRepository.save(passenger);
            passengerId = passenger.getId();
        } else {
            Driver driver = Driver.builder()
                .name(request.name())
                .phone(request.phone())
                .licenseNumber(request.licenseNumber())
                .status(DriverStatus.FREE)
                .build();
            driver = driverRepository.save(driver);
            driverId = driver.getId();
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(request.role())
            .passengerId(passengerId)
            .driverId(driverId)
            .build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name(), user.getId());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name());
        return new LoginResponse(token, user.getRole().name(), user.getId());
    }
}
