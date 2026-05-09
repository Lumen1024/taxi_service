package com.lumen1024.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, JwtGatewayFilter jwtFilter) {
        return builder.routes()
            .route("user-service-public", r -> r
                .path("/login", "/register")
                .uri("http://localhost:8081")
            )
            .route("user-service-profile", r -> r
                .path("/profile")
                .filters(f -> f.filter(jwtFilter))
                .uri("http://localhost:8081")
            )
            .route("trip-service", r -> r
                .path("/trips/**")
                .filters(f -> f.filter(jwtFilter))
                .uri("http://localhost:8082")
            )
            .route("notification-service", r -> r
                .path("/notifications/**")
                .filters(f -> f.filter(jwtFilter))
                .uri("http://localhost:8083")
            )
            .build();
    }
}
