package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LeaderboardResponse {
    private String type;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;
    private List<LeaderboardEntry> entries;
}
