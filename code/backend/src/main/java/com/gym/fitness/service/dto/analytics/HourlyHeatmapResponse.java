package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class HourlyHeatmapResponse {
    private List<String> dayLabels;
    private List<HeatmapPoint> points;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;

    @Data
    public static class HeatmapPoint {
        private Integer hour;
        private Integer dayIndex;
        private Integer count;
    }
}
