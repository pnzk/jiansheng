package com.gym.fitness.service.dto.bodymetric;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BodyMetricResponse {
    private Long id;
    private Long userId;
    private LocalDate measurementDate;
    private Double weightKg;
    private Double bodyFatPercentage;
    private Double heightCm;
    private Double bmi;
    private Double muscleMassKg;
    private LocalDateTime createdAt;
}
