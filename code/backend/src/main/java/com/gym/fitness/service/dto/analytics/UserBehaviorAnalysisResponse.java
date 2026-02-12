package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.util.Map;

@Data
public class UserBehaviorAnalysisResponse {
    private String mostPopularExercise;
    private Map<String, Long> exerciseTypeDistribution;
    private Double averageDurationMinutes;
    private Integer activeUserCount;
}
