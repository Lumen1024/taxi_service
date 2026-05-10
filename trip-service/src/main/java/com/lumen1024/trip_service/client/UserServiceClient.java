package com.lumen1024.trip_service.client;

import com.lumen1024.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private final String baseUrl;

    public UserServiceClient(
        RestTemplate restTemplate,
        JwtService jwtService,
        @Value("${user-service.url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
        this.baseUrl = baseUrl;
    }

    public UserInfo getUser(Long userId) {
        var user = restTemplate.exchange(
            baseUrl + "/users/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Map.class,
            userId
        ).getBody();

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return new UserInfo(
            userId,
            (String) user.get("role"),
            user.get("passengerId") != null ? ((Number) user.get("passengerId")).longValue() : null,
            user.get("driverId") != null ? ((Number) user.get("driverId")).longValue() : null
        );
    }

    public void verifyPassenger(Long passengerId) {
        restTemplate.exchange(
            baseUrl + "/passengers/{id}",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            Object.class,
            passengerId
        );
    }

    public Long acquireDriver() {
        var driver = restTemplate.exchange(
            baseUrl + "/drivers/acquire",
            HttpMethod.POST,
            new HttpEntity<>(authHeaders()),
            Map.class
        ).getBody();

        if (driver == null || driver.get("id") == null) {
            throw new IllegalStateException("No free drivers available");
        }

        return ((Number) driver.get("id")).longValue();
    }

    public void freeDriver(Long driverId) {
        restTemplate.exchange(
            baseUrl + "/drivers/{id}/status",
            HttpMethod.PATCH,
            new HttpEntity<>(Map.of("status", "FREE"), authHeaders()),
            Object.class,
            driverId
        );
    }

    private HttpHeaders authHeaders() {
        String token = jwtService.generateToken(0L, "SERVICE");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public record UserInfo(Long id, String role, Long passengerId, Long driverId) {}
}
