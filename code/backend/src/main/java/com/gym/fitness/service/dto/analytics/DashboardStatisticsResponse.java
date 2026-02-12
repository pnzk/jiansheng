package com.gym.fitness.service.dto.analytics;

import lombok.Data;

@Data
public class DashboardStatisticsResponse {
    private Integer totalUsers;
    private Integer activeUsers;
    private Integer totalDurationMinutes;
    private Double totalCaloriesBurned;
}
