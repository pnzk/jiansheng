package com.gym.fitness.service.dto.exercise;

import lombok.Data;

@Data
public class ExerciseStatisticsResponse {
    private Integer totalRecords;
    private Integer totalDurationMinutes;
    private Double totalCaloriesBurned;
    private Integer averageDurationMinutes;
    private Double averageCaloriesPerSession;
}
