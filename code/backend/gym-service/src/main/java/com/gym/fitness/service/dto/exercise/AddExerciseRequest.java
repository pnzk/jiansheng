package com.gym.fitness.service.dto.exercise;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AddExerciseRequest {
    private String exerciseType;
    private LocalDate exerciseDate;
    private Integer durationMinutes;
    private Double caloriesBurned;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private String equipmentUsed;
    private String notes;
}
