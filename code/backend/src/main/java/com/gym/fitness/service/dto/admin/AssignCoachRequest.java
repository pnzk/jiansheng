package com.gym.fitness.service.dto.admin;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AssignCoachRequest {

    @NotNull(message = "教练ID不能为空")
    private Long coachId;
}

