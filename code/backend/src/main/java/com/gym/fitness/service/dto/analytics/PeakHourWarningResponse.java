package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PeakHourWarningResponse {
    private Boolean isPeakHour;
    private Integer currentCount;
    private Integer threshold;
    private Integer peakHour;
    private Integer peakCount;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;
}
