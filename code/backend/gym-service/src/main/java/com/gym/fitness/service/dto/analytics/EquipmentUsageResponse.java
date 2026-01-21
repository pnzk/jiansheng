package com.gym.fitness.service.dto.analytics;

import lombok.Data;

import java.util.Map;

@Data
public class EquipmentUsageResponse {
    private Map<String, Long> equipmentUsage;
    private Long totalUsage;
}
