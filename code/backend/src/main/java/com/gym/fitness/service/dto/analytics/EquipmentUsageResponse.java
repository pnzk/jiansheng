package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
public class EquipmentUsageResponse {
    private Map<String, Long> equipmentUsage;
    private Long totalUsage;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Boolean fallbackApplied;
}
