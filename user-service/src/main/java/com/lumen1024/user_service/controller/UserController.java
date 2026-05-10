package com.lumen1024.user_service.controller;

import com.lumen1024.user_service.dto.UserResponse;
import com.lumen1024.user_service.entity.User;
import com.lumen1024.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(new UserResponse(
            user.getId(),
            user.getRole().name(),
            user.getPassengerId(),
            user.getDriverId()
        ));
    }
}