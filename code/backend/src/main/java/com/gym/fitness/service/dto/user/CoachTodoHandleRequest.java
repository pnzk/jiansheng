package com.gym.fitness.service.dto.user;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CoachTodoHandleRequest {

    @NotNull(message = "studentId不能为空")
    private Long studentId;

    private String todoKey;

    private String todoTitle;

    private String todoDescription;
}
