package com.lumen1024.user_service.repository;

import com.lumen1024.user_service.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
}
