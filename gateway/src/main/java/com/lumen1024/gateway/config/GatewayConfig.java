package com.lumen1024.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${trip-service.url:http://localhost:8082}")
    private String tripServiceUrl;

    @Value("${notification-service.url:http://localhost:8083}")
    private String notificationServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, JwtGatewayFilter jwtFilter) {
        return builder.routes()
            .route("user-service-public", r -> r
                .path("/login", "/register")
                .uri(userServiceUrl)
            )
            .route("user-service-profile", r -> r
                .path("/profile", "/profile/**")
                .filters(f -> f.filter(jwtFilter))
                .uri(userServiceUrl)
            )
            .route("trip-service", r -> r
                .path("/trips/**")
                .filters(f -> f.filter(jwtFilter))
                .uri(tripServiceUrl)
            )
            .route("notification-service", r -> r
                .path("/notifications/**")
                .filters(f -> f.filter(jwtFilter))
                .uri(notificationServiceUrl)
            )
            .build();
    }
}
