package com.lumen1024.user_service.service;

import com.lumen1024.user_service.dto.DriverResponse;
import com.lumen1024.user_service.dto.UpdateStatusRequest;
import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.DriverStatus;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.DriverRepository;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class DriverService {

    private static final String AVAILABLE_DRIVERS_KEY = "available_drivers";

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public DriverService(DriverRepository driverRepository, UserRepository userRepository, StringRedisTemplate redisTemplate) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    private Long findUserIdByDriverId(Long driverId) {
        return userRepository.findByDriverId(driverId)
            .map(User::getId)
            .orElse(null);
    }

    public DriverResponse getDriver(Long id) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        return DriverResponse.from(driver, findUserIdByDriverId(id));
    }

    public DriverResponse updateStatus(Long id, UpdateStatusRequest request) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        driver.setStatus(request.status());
        driver = driverRepository.save(driver);

        try {
            if (request.status() == DriverStatus.FREE) {
                redisTemplate.opsForSet().add(AVAILABLE_DRIVERS_KEY, driver.getId().toString());
            } else {
                redisTemplate.opsForSet().remove(AVAILABLE_DRIVERS_KEY, driver.getId().toString());
            }
        } catch (Exception e) {
            log.warn("Redis operation failed (non-critical): {}", e.getMessage());
        }

        return DriverResponse.from(driver, findUserIdByDriverId(id));
    }

    @Transactional
    public DriverResponse acquireDriver() {
        Driver driver = driverRepository.acquireFreeDriver()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No free drivers available"));

        driver.setStatus(DriverStatus.BUSY);
        driver = driverRepository.save(driver);

        try {
            redisTemplate.opsForSet().remove(AVAILABLE_DRIVERS_KEY, driver.getId().toString());
        } catch (Exception e) {
            log.warn("Redis operation failed (non-critical): {}", e.getMessage());
        }

        return DriverResponse.from(driver, findUserIdByDriverId(driver.getId()));
    }
}
