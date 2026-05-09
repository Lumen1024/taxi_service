package com.lumen1024.user_service.repository;

import com.lumen1024.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.passengerId = :passengerId")
    Optional<User> findByPassengerId(@Param("passengerId") Long passengerId);

    @Query("SELECT u FROM User u WHERE u.driverId = :driverId")
    Optional<User> findByDriverId(@Param("driverId") Long driverId);
}
