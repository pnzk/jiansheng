package com.gym.fitness.service.dto.plan;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePlanRequest {
    private String planName;
    private String goalType;
    private Double targetValue;
    private LocalDate endDate;
    private String weeklySchedule;
    private String description;
    private String status;
}
