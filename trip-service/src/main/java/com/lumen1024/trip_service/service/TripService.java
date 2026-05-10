package com.lumen1024.trip_service.service;

import com.lumen1024.trip_service.dto.RateRequest;
import com.lumen1024.trip_service.dto.StatsResponse;
import com.lumen1024.trip_service.dto.TripEvent;
import com.lumen1024.trip_service.dto.TripResponse;
import com.lumen1024.trip_service.dto.UpdateStatusRequest;
import com.lumen1024.trip_service.entity.Trip;
import com.lumen1024.trip_service.entity.TripStatus;
import com.lumen1024.trip_service.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${trip.tariff:50.0}")
    private BigDecimal tariff;

    public TripResponse createTrip(Long userId, String origin, String destination) {
        UserInfo user = resolveUser(userId);

        if (!"PASSENGER".equals(user.role())) {
            throw new SecurityException("Only passengers can create trips");
        }

        restTemplate.getForObject(
            userServiceUrl + "/passengers/{id}", Object.class, user.passengerId()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> driver;
        try {
            driver = restTemplate.postForObject(userServiceUrl + "/drivers/acquire", null, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("No free drivers available");
        }

        if (driver == null || driver.get("id") == null) {
            throw new IllegalStateException("No free drivers available");
        }

        Long driverId = ((Number) driver.get("id")).longValue();
        BigDecimal price = calculatePrice(origin, destination);

        Trip trip = Trip.builder()
            .passengerId(user.passengerId())
            .driverId(driverId)
            .origin(origin)
            .destination(destination)
            .price(price)
            .status(TripStatus.WAITING_DRIVER)
            .build();

        trip = tripRepository.save(trip);

        sendEvent(trip, "TRIP_CREATED", "Новая поездка назначена вам", driverId, "DRIVER");

        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripHistory(Long userId) {
        UserInfo user = resolveUser(userId);

        if ("PASSENGER".equals(user.role())) {
            return tripRepository.findAllByPassenger(user.passengerId())
                .stream().map(TripResponse::from).toList();
        }

        List<Trip> trips = new ArrayList<>();
        for (TripStatus status : TripStatus.values()) {
            trips.addAll(tripRepository.findAllByDriverAndStatus(user.driverId(), status));
        }
        return trips.stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        UserInfo user = resolveUser(userId);

        boolean isPassenger = "PASSENGER".equals(user.role())
            && trip.getPassengerId().equals(user.passengerId());
        boolean isDriver = "DRIVER".equals(user.role())
            && trip.getDriverId() != null && trip.getDriverId().equals(user.driverId());

        if (!isPassenger && !isDriver) {
            throw new SecurityException("Access denied");
        }

        return TripResponse.from(trip);
    }

    public TripResponse updateStatus(Long tripId, UpdateStatusRequest request, Long userId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        UserInfo user = resolveUser(userId);
        TripStatus newStatus = request.status();

        switch (newStatus) {
            case IN_PROGRESS -> {
                if (!"DRIVER".equals(user.role())) {
                    throw new SecurityException("Only driver can accept a trip");
                }
                if (!trip.getDriverId().equals(user.driverId())) {
                    throw new SecurityException("You are not the driver of this trip");
                }
                if (trip.getStatus() != TripStatus.WAITING_DRIVER) {
                    throw new IllegalStateException("Trip must be in WAITING_DRIVER status");
                }
                trip.setStatus(TripStatus.IN_PROGRESS);
                sendEvent(trip, "TRIP_STARTED", "Водитель начал поездку", trip.getPassengerId(), "PASSENGER");
            }
            case COMPLETED -> {
                if (!"DRIVER".equals(user.role())) {
                    throw new SecurityException("Only driver can complete a trip");
                }
                if (!trip.getDriverId().equals(user.driverId())) {
                    throw new SecurityException("You are not the driver of this trip");
                }
                if (trip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Trip must be in IN_PROGRESS status");
                }
                trip.setStatus(TripStatus.COMPLETED);
                freeDriver(trip.getDriverId());
                sendEvent(trip, "TRIP_COMPLETED", "Поездка завершена", trip.getPassengerId(), "PASSENGER");
            }
            case CANCELLED -> {
                if (!"PASSENGER".equals(user.role())) {
                    throw new SecurityException("Only passenger can cancel a trip");
                }
                if (!trip.getPassengerId().equals(user.passengerId())) {
                    throw new SecurityException("You are not the passenger of this trip");
                }
                if (trip.getStatus() != TripStatus.WAITING_DRIVER
                    && trip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new IllegalStateException("Trip cannot be cancelled in its current status");
                }
                trip.setStatus(TripStatus.CANCELLED);
                freeDriver(trip.getDriverId());
                sendEvent(trip, "TRIP_CANCELLED", "Пассажир отменил поездку", trip.getDriverId(), "DRIVER");
            }
            case WAITING_DRIVER ->
                throw new IllegalArgumentException("Cannot set status back to WAITING_DRIVER");
        }

        trip = tripRepository.save(trip);
        return TripResponse.from(trip);
    }

    public TripResponse rateTrip(Long tripId, int rating, Long userId) {
        UserInfo user = resolveUser(userId);

        if (!"PASSENGER".equals(user.role())) {
            throw new SecurityException("Only passengers can rate trips");
        }

        Trip trip = tripRepository.findByOwner(tripId, user.passengerId())
            .orElseThrow(() -> new IllegalArgumentException("Trip not found or you are not the passenger"));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new IllegalStateException("Can only rate completed trips");
        }

        if (trip.getRating() != null) {
            throw new IllegalStateException("Trip already rated");
        }

        trip.setRating(rating);
        trip = tripRepository.save(trip);
        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String dateStr, Long userId) {
        UserInfo user = resolveUser(userId);

        if (!"DRIVER".equals(user.role())) {
            throw new SecurityException("Only drivers can view stats");
        }

        LocalDate date = LocalDate.parse(dateStr);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> result = tripRepository.getStatsByDriverAndDate(user.driverId(), start, end);
        Object[] row = result.get(0);

        long count = ((Number) row[0]).longValue();
        BigDecimal avgPrice = (BigDecimal) row[1];
        BigDecimal totalRevenue = (BigDecimal) row[2];

        return new StatsResponse(count, avgPrice, totalRevenue);
    }

    private UserInfo resolveUser(Long userId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restTemplate.getForObject(
                userServiceUrl + "/users/{id}", Map.class, userId
            );
            if (user == null) {
                throw new IllegalArgumentException("User not found");
            }
            return new UserInfo(
                userId,
                (String) user.get("role"),
                user.get("passengerId") != null ? ((Number) user.get("passengerId")).longValue() : null,
                user.get("driverId") != null ? ((Number) user.get("driverId")).longValue() : null
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve user: " + e.getMessage(), e);
        }
    }

    private BigDecimal calculatePrice(String origin, String destination) {
        double[] o = parseCoordinates(origin);
        double[] d = parseCoordinates(destination);
        double distance = Math.sqrt(Math.pow(d[0] - o[0], 2) + Math.pow(d[1] - o[1], 2));
        return BigDecimal.valueOf(distance).multiply(tariff).setScale(2, RoundingMode.HALF_UP);
    }

    private double[] parseCoordinates(String coords) {
        String[] parts = coords.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Coordinates must be in 'lat,lon' format: " + coords);
        }
        return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
    }

    private void freeDriver(Long driverId) {
        try {
            restTemplate.exchange(
                userServiceUrl + "/drivers/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "FREE")),
                Object.class,
                driverId
            );
        } catch (Exception e) {
            // best-effort
        }
    }

    private void sendEvent(Trip trip, String event, String message, Long recipientId, String recipientType) {
        try {
            TripEvent tripEvent = new TripEvent(
                trip.getId(), event,
                trip.getPassengerId(), trip.getDriverId(),
                message, recipientId, recipientType
            );
            rabbitTemplate.convertAndSend("trip.exchange", "trip.event", tripEvent);
        } catch (Exception e) {
            // best-effort
        }
    }

    private record UserInfo(Long id, String role, Long passengerId, Long driverId) {}
}