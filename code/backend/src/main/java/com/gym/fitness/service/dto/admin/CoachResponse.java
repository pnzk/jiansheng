package com.gym.fitness.service.dto.admin;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CoachResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private Integer age;
    private String gender;
    private Integer studentCount;
    private LocalDateTime createdAt;
}
