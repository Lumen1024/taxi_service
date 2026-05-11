package com.lumen1024.trip_service.service;

import com.lumen1024.trip_service.client.UserServiceClient;
import com.lumen1024.trip_service.dto.RateRequest;
import com.lumen1024.trip_service.dto.StatsResponse;
import com.lumen1024.common.dto.TripEvent;
import com.lumen1024.trip_service.dto.TripResponse;
import com.lumen1024.trip_service.dto.UpdateStatusRequest;
import com.lumen1024.trip_service.entity.Trip;
import com.lumen1024.trip_service.entity.TripStatus;
import com.lumen1024.trip_service.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TripService {

    private final TripRepository tripRepository;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${trip.tariff:50.0}")
    private BigDecimal tariff;

    public TripResponse createTrip(Long userId, String origin, String destination) {
        var user = userServiceClient.getUser(userId);

        if (!"PASSENGER".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only passengers can create trips");
        }

        userServiceClient.verifyPassenger(user.passengerId());

        var acquired = userServiceClient.acquireDriver();
        BigDecimal price = calculatePrice(origin, destination);

        Trip trip = Trip.builder()
            .passengerId(user.passengerId())
            .driverId(acquired.driverId())
            .origin(origin)
            .destination(destination)
            .price(price)
            .status(TripStatus.IN_PROGRESS)
            .build();

        trip = tripRepository.save(trip);

        sendEvent(trip, "TRIP_STARTED", "Водитель начал поездку", userId, "PASSENGER");
        sendEvent(trip, "TRIP_CREATED", "Новая поездка назначена вам", acquired.userId(), "DRIVER");

        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripHistory(Long userId) {
        var user = userServiceClient.getUser(userId);

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
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        var user = userServiceClient.getUser(userId);

        boolean isPassenger = "PASSENGER".equals(user.role())
            && trip.getPassengerId().equals(user.passengerId());
        boolean isDriver = "DRIVER".equals(user.role())
            && trip.getDriverId() != null && trip.getDriverId().equals(user.driverId());

        if (!isPassenger && !isDriver) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
        }

        return TripResponse.from(trip);
    }

    public TripResponse updateStatus(Long tripId, UpdateStatusRequest request, Long userId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));

        var user = userServiceClient.getUser(userId);
        TripStatus newStatus = request.status();

        switch (newStatus) {
            case COMPLETED -> {
                if (!"DRIVER".equals(user.role())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only driver can complete a trip");
                }
                if (!trip.getDriverId().equals(user.driverId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not the driver of this trip");
                }
                if (trip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Trip must be in IN_PROGRESS status");
                }
                trip.setStatus(TripStatus.COMPLETED);
                userServiceClient.freeDriver(trip.getDriverId());
                sendEvent(trip, "TRIP_COMPLETED", "Поездка завершена", userServiceClient.getUserIdByPassengerId(trip.getPassengerId()), "PASSENGER");
            }
            case CANCELLED -> {
                if (!"PASSENGER".equals(user.role())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only passenger can cancel a trip");
                }
                if (!trip.getPassengerId().equals(user.passengerId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not the passenger of this trip");
                }
                if (trip.getStatus() != TripStatus.IN_PROGRESS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"Trip cannot be cancelled in its current status");
                }
                trip.setStatus(TripStatus.CANCELLED);
                userServiceClient.freeDriver(trip.getDriverId());
                sendEvent(trip, "TRIP_CANCELLED", "Пассажир отменил поездку", userServiceClient.getUserIdByDriverId(trip.getDriverId()), "DRIVER");
            }
        }

        trip = tripRepository.save(trip);
        return TripResponse.from(trip);
    }

    public TripResponse rateTrip(Long tripId, int rating, Long userId) {
        var user = userServiceClient.getUser(userId);

        if (!"PASSENGER".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only passengers can rate trips");
        }

        Trip trip = tripRepository.findByOwner(tripId, user.passengerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found or you are not the passenger"));

        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Can only rate completed trips");
        }

        if (trip.getRating() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Trip already rated");
        }

        trip.setRating(rating);
        trip = tripRepository.save(trip);
        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public StatsResponse getStats(String dateStr, Long userId) {
        var user = userServiceClient.getUser(userId);

        if (!"DRIVER".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Only drivers can view stats");
        }

        LocalDate date = LocalDate.parse(dateStr);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Object[]> result = tripRepository.getStatsByDriverAndDate(user.driverId(), start, end);
        Object[] row = result.get(0);

        long count = ((Number) row[0]).longValue();
        BigDecimal avgPrice = toBigDecimal((Number) row[1]);
        BigDecimal totalRevenue = toBigDecimal((Number) row[2]);

        return new StatsResponse(count, avgPrice, totalRevenue);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordinates must be in 'lat,lon' format: " + coords);
        }
        return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
    }

    private BigDecimal toBigDecimal(Number value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return BigDecimal.valueOf(value.doubleValue());
    }

    private void sendEvent(Trip trip, String event, String message, Long recipientId, String recipientType) {
        try {
            TripEvent tripEvent = new TripEvent(
                trip.getId(), event,
                trip.getPassengerId(), trip.getDriverId(),
                message, recipientId, recipientType
            );
            rabbitTemplate.convertAndSend("trip.exchange", "trip.event", tripEvent);
            log.info("Event sent: tripId={}, event={}, recipientId={}", trip.getId(), event, recipientId);
        } catch (Exception e) {
            log.error("Failed to send event: tripId={}, event={}: {}", trip.getId(), event, e.getMessage());
        }
    }

}