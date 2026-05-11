package com.lumen1024.trip_service.client;

import com.lumen1024.common.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

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
        try {
            var user = restTemplate.exchange(
                baseUrl + "/users/{id}",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class,
                userId
            ).getBody();

            if (user == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            return new UserInfo(
                userId,
                (String) user.get("role"),
                user.get("passengerId") != null ? ((Number) user.get("passengerId")).longValue() : null,
                user.get("driverId") != null ? ((Number) user.get("driverId")).longValue() : null
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), "User service error: " + e.getResponseBodyAsString());
        }
    }

    public void verifyPassenger(Long passengerId) {
        try {
            restTemplate.exchange(
                baseUrl + "/passengers/{id}",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Object.class,
                passengerId
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), "User service error: " + e.getResponseBodyAsString());
        }
    }

    public Long acquireDriver() {
        Map body;
        try {
            body = restTemplate.exchange(
                baseUrl + "/drivers/acquire",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders()),
                Map.class
            ).getBody();
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), "User service error: " + e.getResponseBodyAsString());
        }

        if (body == null || body.get("id") == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No free drivers available");
        }

        return ((Number) body.get("id")).longValue();
    }

    public void freeDriver(Long driverId) {
        try {
            restTemplate.exchange(
                baseUrl + "/drivers/{id}/status",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("status", "FREE"), authHeaders()),
                Object.class,
                driverId
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(e.getStatusCode(), "User service error: " + e.getResponseBodyAsString());
        }
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
