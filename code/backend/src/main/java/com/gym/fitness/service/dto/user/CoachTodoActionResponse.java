package com.gym.fitness.service.dto.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoachTodoActionResponse {
    private Long studentId;
    private String todoKey;
    private String todoTitle;
    private String todoDescription;
    private LocalDateTime handledAt;
}
