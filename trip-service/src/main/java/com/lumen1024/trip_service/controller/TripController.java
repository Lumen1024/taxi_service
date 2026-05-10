package com.lumen1024.trip_service.controller;

import com.lumen1024.trip_service.dto.CreateTripRequest;
import com.lumen1024.trip_service.dto.RateRequest;
import com.lumen1024.trip_service.dto.StatsResponse;
import com.lumen1024.trip_service.dto.TripResponse;
import com.lumen1024.trip_service.dto.UpdateStatusRequest;
import com.lumen1024.trip_service.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
        @Valid @RequestBody CreateTripRequest request,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tripService.createTrip(userId, request.origin(), request.destination()));
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getHistory(
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(tripService.getTripHistory(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTrip(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(tripService.getTrip(id, userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(tripService.updateStatus(id, request, userId));
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<TripResponse> rateTrip(
        @PathVariable Long id,
        @Valid @RequestBody RateRequest request,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(tripService.rateTrip(id, request.rating(), userId));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats(
        @RequestParam("date") String date,
        @RequestHeader("X-User-Id") Long userId
    ) {
        return ResponseEntity.ok(tripService.getStats(date, userId));
    }
}