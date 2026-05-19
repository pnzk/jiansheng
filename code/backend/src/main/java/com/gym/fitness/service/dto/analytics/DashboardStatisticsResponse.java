package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DashboardStatisticsResponse {
    private Integer totalUsers;
    private Integer activeUsers;
    private Integer totalDurationMinutes;
    private Double totalCaloriesBurned;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;
}
