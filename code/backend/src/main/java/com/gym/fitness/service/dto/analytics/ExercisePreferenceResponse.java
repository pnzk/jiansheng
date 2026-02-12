package com.gym.fitness.service.dto.analytics;

import lombok.Data;
import java.util.List;

@Data
public class ExercisePreferenceResponse {
    private List<PreferenceData> preferences;
    private String mostPopular;
    private Integer totalUsers;
    
    @Data
    public static class PreferenceData {
        private String exerciseType;
        private Integer count;
        private Double percentage;
    }
}
