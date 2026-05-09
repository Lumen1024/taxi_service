package com.lumen1024.trip_service.repository;

import com.lumen1024.trip_service.entity.Trip;
import com.lumen1024.trip_service.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Query("SELECT t FROM Trip t WHERE t.passengerId = :passengerId ORDER BY t.createdAt DESC")
    List<Trip> findAllByPassenger(@Param("passengerId") Long passengerId);

    @Query("SELECT t FROM Trip t WHERE t.driverId = :driverId AND t.status = :status")
    List<Trip> findAllByDriverAndStatus(@Param("driverId") Long driverId, @Param("status") TripStatus status);

    @Query("""
        SELECT t FROM Trip t
        WHERE t.driverId = :driverId
          AND t.createdAt < :end
          AND t.createdAt >= :start
    """)
    List<Trip> findAllByDriverAndDate(
        @Param("driverId") Long driverId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );

    @Query("SELECT t FROM Trip t WHERE t.id = :id AND t.passengerId = :passengerId")
    Optional<Trip> findByOwner(@Param("id") Long id, @Param("passengerId") Long passengerId);

    @Query("SELECT t FROM Trip t WHERE t.id = :id AND t.driverId = :driverId")
    Optional<Trip> findByDriver(@Param("id") Long id, @Param("driverId") Long driverId);

    @Query("""
        SELECT COUNT(t), COALESCE(AVG(t.price), 0), COALESCE(SUM(t.price), 0)
        FROM Trip t
        WHERE t.driverId = :driverId
          AND t.createdAt >= :start
          AND t.createdAt < :end
          AND t.status = 'COMPLETED'
    """)
    List<Object[]> getStatsByDriverAndDate(
        @Param("driverId") Long driverId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}