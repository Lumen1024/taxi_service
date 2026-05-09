package com.lumen1024.user_service.repository;

import com.lumen1024.user_service.entity.Driver;
import com.lumen1024.user_service.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Driver d WHERE d.status = 'FREE' ORDER BY d.id LIMIT 1")
    Optional<Driver> acquireFreeDriver();
}
