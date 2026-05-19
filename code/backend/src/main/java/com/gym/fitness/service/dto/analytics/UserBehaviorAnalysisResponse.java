package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class UserBehaviorAnalysisResponse {
    private String mostPopularExercise;
    private Map<String, Long> exerciseTypeDistribution;
    private Double averageDurationMinutes;
    private Integer activeUserCount;
    private Double averageActiveRate;
    private Integer totalDurationMinutes;
    private Double totalCaloriesBurned;
    private Double averagePlanCompletionRate;
    private List<DailyActivityPoint> dailyActivity;
    private List<RetentionPoint> retentionRates;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;

    @Data
    public static class DailyActivityPoint {
        private LocalDate date;
        private Integer activeUserCount;
        private Double averageDurationMinutes;
        private Integer totalDurationMinutes;
        private Double totalCaloriesBurned;
    }

    @Data
    public static class RetentionPoint {
        private Integer days;
        private String label;
        private Integer cohortSize;
        private Integer retainedUsers;
        private Double retentionRate;
    }
}
