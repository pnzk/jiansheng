package com.gym.fitness.service.dto.analytics;

import lombok.Data;

@Data
public class PeakHourWarningResponse {
    private Boolean isPeakHour;
    private Integer currentCount;
    private Integer threshold;
    private Integer peakHour;
}
