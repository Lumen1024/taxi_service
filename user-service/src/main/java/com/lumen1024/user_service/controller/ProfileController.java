package com.lumen1024.user_service.controller;

import com.lumen1024.user_service.dto.ProfileResponse;
import com.lumen1024.user_service.dto.UpdateProfileRequest;
import com.lumen1024.user_service.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    private Long getCurrentUserId() {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.parseLong(name);
    }
}
