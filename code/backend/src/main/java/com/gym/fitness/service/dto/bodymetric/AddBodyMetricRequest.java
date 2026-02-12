package com.gym.fitness.service.dto.bodymetric;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AddBodyMetricRequest {
    private LocalDate measurementDate;
    private Double weightKg;
    private Double bodyFatPercentage;
    private Double heightCm;
    private Double muscleMassKg;
}
