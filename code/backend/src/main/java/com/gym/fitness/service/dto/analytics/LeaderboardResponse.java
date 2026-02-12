package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.util.List;

@Data
public class LeaderboardResponse {
    private String type;
    private List<LeaderboardEntry> entries;
}
