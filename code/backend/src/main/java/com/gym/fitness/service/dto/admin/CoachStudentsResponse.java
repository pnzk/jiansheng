package com.gym.fitness.service.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CoachStudentsResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private Integer age;
    private String gender;
    private String fitnessGoal;
    private Double currentWeight;
    private Double planProgress;
    private LocalDateTime lastExerciseTime;
    private String trainingStatus;
}
