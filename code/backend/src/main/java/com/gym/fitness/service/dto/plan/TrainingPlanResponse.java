package com.gym.fitness.service.dto.plan;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TrainingPlanResponse {
    private Long id;
    private Long studentId;
    private Long coachId;
    private String planName;
    private String goalType;
    private Double targetValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Double completionRate;
    private String weeklySchedule;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
