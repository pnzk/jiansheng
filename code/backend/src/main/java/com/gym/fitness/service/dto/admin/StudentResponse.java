package com.gym.fitness.service.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StudentResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer age;
    private String gender;
    private String fitnessGoal;
    private Long coachId;
    private String coachName;
    private LocalDateTime createdAt;
}
