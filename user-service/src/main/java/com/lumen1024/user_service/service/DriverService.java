package com.lumen1024.user_service.service;

import com.lumen1024.user_service.dto.DriverResponse;
import com.lumen1024.user_service.dto.UpdateStatusRequest;
import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.DriverStatus;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.DriverRepository;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private static final String AVAILABLE_DRIVERS_KEY = "available_drivers";

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public DriverResponse getDriver(Long id) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));
        String email = userRepository.findByDriverId(driver.getId())
            .map(User::getEmail)
            .orElse(null);
        return DriverResponse.from(driver, email);
    }

    @Transactional
    public DriverResponse updateStatus(Long id, UpdateStatusRequest request) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        DriverStatus oldStatus = driver.getStatus();
        driver.setStatus(request.status());
        driver = driverRepository.save(driver);

        if (request.status() == DriverStatus.FREE) {
            redisTemplate.opsForSet().add(AVAILABLE_DRIVERS_KEY, driver.getId().toString());
        } else {
            redisTemplate.opsForSet().remove(AVAILABLE_DRIVERS_KEY, driver.getId().toString());
        }

        String email = userRepository.findByDriverId(driver.getId())
            .map(User::getEmail)
            .orElse(null);
        return DriverResponse.from(driver, email);
    }

    @Transactional
    public DriverResponse acquireDriver() {
        Driver driver = driverRepository.acquireFreeDriver()
            .orElseThrow(() -> new IllegalStateException("No free drivers available"));

        driver.setStatus(DriverStatus.BUSY);
        driver = driverRepository.save(driver);
        redisTemplate.opsForSet().remove(AVAILABLE_DRIVERS_KEY, driver.getId().toString());

        String email = userRepository.findByDriverId(driver.getId())
            .map(User::getEmail)
            .orElse(null);
        return DriverResponse.from(driver, email);
    }
}
