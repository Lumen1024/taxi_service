package com.lumen1024.user_service.service;

import com.lumen1024.user_service.dto.ProfileResponse;
import com.lumen1024.user_service.dto.UpdateProfileRequest;
import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.Passenger;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.DriverRepository;
import com.lumen1024.user_service.repository.PassengerRepository;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;

    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String name = null;
        String phone = null;
        String licenseNumber = null;

        if (user.getPassengerId() != null) {
            Passenger p = passengerRepository.findById(user.getPassengerId())
                .orElseThrow(() -> new IllegalStateException("Passenger profile not found"));
            name = p.getName();
            phone = p.getPhone();
        } else if (user.getDriverId() != null) {
            Driver d = driverRepository.findById(user.getDriverId())
                .orElseThrow(() -> new IllegalStateException("Driver profile not found"));
            name = d.getName();
            phone = d.getPhone();
            licenseNumber = d.getLicenseNumber();
        }

        return new ProfileResponse(
            user.getId(), user.getEmail(), user.getRole().name(),
            user.getPassengerId(), user.getDriverId(),
            name, phone, licenseNumber
        );
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getPassengerId() != null) {
            Passenger p = passengerRepository.findById(user.getPassengerId())
                .orElseThrow(() -> new IllegalStateException("Passenger profile not found"));
            p.setName(request.name());
            p.setPhone(request.phone());
            passengerRepository.save(p);
        } else if (user.getDriverId() != null) {
            Driver d = driverRepository.findById(user.getDriverId())
                .orElseThrow(() -> new IllegalStateException("Driver profile not found"));
            d.setName(request.name());
            d.setPhone(request.phone());
            driverRepository.save(d);
        }

        return getProfile(userId);
    }
}
