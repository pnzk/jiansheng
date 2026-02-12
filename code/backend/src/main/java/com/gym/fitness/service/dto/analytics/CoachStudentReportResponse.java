package com.gym.fitness.service.dto.analytics;

import lombok.Data;

@Data
public class CoachStudentReportResponse {
    private Long studentId;
    private String studentName;
    private Double startWeight;
    private Double currentWeight;
    private Double weightChange;
    private Integer totalDuration;
    private Double totalCalories;
    private Integer exerciseCount;
    private Integer avgDuration;
    private Double planProgress;
}

