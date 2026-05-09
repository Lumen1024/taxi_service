package com.lumen1024.user_service.controller;

import com.lumen1024.user_service.dto.DriverResponse;
import com.lumen1024.user_service.dto.UpdateStatusRequest;
import com.lumen1024.user_service.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriver(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<DriverResponse> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(driverService.updateStatus(id, request));
    }

    @PostMapping("/acquire")
    public ResponseEntity<DriverResponse> acquireDriver() {
        return ResponseEntity.ok(driverService.acquireDriver());
    }
}
