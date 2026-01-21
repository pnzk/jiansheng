package com.gym.fitness.service.dto.analytics;

import com.gym.fitness.domain.entity.BodyMetric;
import lombok.Data;

import java.util.List;

@Data
public class FitnessEffectAnalysisResponse {
    private Double weightChange;
    private Double bodyFatChange;
    private Double bmiChange;
    private List<BodyMetric> metrics;
}
