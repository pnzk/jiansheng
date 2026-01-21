package com.gym.fitness.service.dto.analytics;

import lombok.Data;
import java.util.List;

@Data
public class HourlyActivityResponse {
    private List<HourlyData> hourlyData;
    private Integer peakHour;
    private Integer peakCount;
    
    @Data
    public static class HourlyData {
        private Integer hour;
        private Integer count;
        private Integer duration;
    }
}
