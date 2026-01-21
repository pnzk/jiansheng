package com.gym.fitness.service.dto.plan;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePlanRequest {
    private Long studentId;
    private String planName;
    private String goalType;
    private Double targetValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String weeklySchedule;
    private String description;
}
