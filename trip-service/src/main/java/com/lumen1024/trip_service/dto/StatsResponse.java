package com.lumen1024.trip_service.dto;

import java.math.BigDecimal;

public record StatsResponse(
    long tripCount,
    BigDecimal averagePrice,
    BigDecimal totalRevenue
) {}