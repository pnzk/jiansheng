package com.gym.fitness.service.dto.analytics;

import lombok.Data;

@Data
public class CoachWorkloadResponse {
    private Long coachId;
    private String coachName;
    private Integer studentCount;
    private Integer planCount;
    private Integer activeStudents;
    private Double avgProgress;
}

