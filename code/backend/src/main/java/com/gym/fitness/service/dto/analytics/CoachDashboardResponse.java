package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class CoachDashboardResponse {
    private Integer totalStudents;
    private Integer maleStudents;
    private Integer femaleStudents;
    private Integer avgAge;
    private Integer activeStudents;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Map<String, Integer> goalDistribution;
    private Map<String, Integer> exerciseTypeDistribution;
    private List<WeightTrendPoint> weightTrend;

    @Data
    public static class WeightTrendPoint {
        private LocalDate date;
        private Double avgWeight;
    }
}

