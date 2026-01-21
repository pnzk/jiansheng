package com.gym.fitness.service.dto.achievement;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AchievementResponse {
    private Long id;
    private String achievementName;
    private String description;
    private String achievementType;
    private Double thresholdValue;
    private String iconUrl;
    private Boolean unlocked;
    private LocalDateTime unlockedAt;
}
