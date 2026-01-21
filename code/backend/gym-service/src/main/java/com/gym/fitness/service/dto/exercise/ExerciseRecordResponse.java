package com.gym.fitness.service.dto.exercise;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExerciseRecordResponse {
    private Long id;
    private Long userId;
    private String exerciseType;
    private LocalDate exerciseDate;
    private Integer durationMinutes;
    private Double caloriesBurned;
    private Integer averageHeartRate;
    private Integer maxHeartRate;
    private String equipmentUsed;
    private String notes;
    private LocalDateTime createdAt;
}
