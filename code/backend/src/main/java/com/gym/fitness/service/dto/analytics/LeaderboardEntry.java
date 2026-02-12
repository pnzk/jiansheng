package com.gym.fitness.service.dto.analytics;

import lombok.Data;

@Data
public class LeaderboardEntry {
    private Long userId;
    private String username;
    private String realName;
    private Integer rank;
    private Double value;
}
