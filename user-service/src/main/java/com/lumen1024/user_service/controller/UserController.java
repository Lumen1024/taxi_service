package com.lumen1024.user_service.controller;

import com.lumen1024.user_service.dto.UserResponse;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/by-passenger/{passengerId}")
    public ResponseEntity<UserResponse> getByPassengerId(@PathVariable Long passengerId) {
        User user = userRepository.findByPassengerId(passengerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/by-driver/{driverId}")
    public ResponseEntity<UserResponse> getByDriverId(@PathVariable Long driverId) {
        User user = userRepository.findByDriverId(driverId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getRole().name(),
            user.getPassengerId(),
            user.getDriverId()
        );
    }
}