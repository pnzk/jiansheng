package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.domain.entity.*;
import com.gym.fitness.repository.mapper.*;
import com.gym.fitness.service.dto.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final UserMapper userMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final BodyMetricMapper bodyMetricMapper;
    private final LeaderboardMapper leaderboardMapper;

    public DashboardStatisticsResponse getDashboardStatistics() {
        DashboardStatisticsResponse stats = new DashboardStatisticsResponse();
        
        // Total users
        stats.setTotalUsers(userMapper.selectCount(null).intValue());
        
        // Active users (exercised in last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        QueryWrapper<ExerciseRecord> activeWrapper = new QueryWrapper<>();
        activeWrapper.ge("exercise_date", thirtyDaysAgo);
        List<ExerciseRecord> recentRecords = exerciseRecordMapper.selectList(activeWrapper);
        stats.setActiveUsers((int) recentRecords.stream().map(ExerciseRecord::getUserId).distinct().count());
        
        // Total exercise duration
        List<ExerciseRecord> allRecords = exerciseRecordMapper.selectList(null);
        stats.setTotalDurationMinutes(allRecords.stream().mapToInt(ExerciseRecord::getDurationMinutes).sum());
        
        // Total calories burned
        stats.setTotalCaloriesBurned(allRecords.stream().mapToDouble(ExerciseRecord::getCaloriesBurned).sum());
        
        return stats;
    }

    public UserBehaviorAnalysisResponse getUserBehaviorAnalysis(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        if (startDate != null) {
            wrapper.ge("exercise_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("exercise_date", endDate);
        }
        
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        
        UserBehaviorAnalysisResponse analysis = new UserBehaviorAnalysisResponse();
        
        // Most popular exercise type
        Map<String, Long> exerciseTypeCounts = records.stream()
                .collect(Collectors.groupingBy(ExerciseRecord::getExerciseType, Collectors.counting()));
        analysis.setMostPopularExercise(exerciseTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A"));
        
        // Exercise type distribution
        analysis.setExerciseTypeDistribution(exerciseTypeCounts);
        
        // Average duration
        analysis.setAverageDurationMinutes(records.isEmpty() ? 0 : 
                records.stream().mapToInt(ExerciseRecord::getDurationMinutes).average().orElse(0));
        
        // Active user count
        analysis.setActiveUserCount((int) records.stream().map(ExerciseRecord::getUserId).distinct().count());
        
        return analysis;
    }

    public FitnessEffectAnalysisResponse getFitnessEffectAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<BodyMetric> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        if (startDate != null) {
            wrapper.ge("measurement_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("measurement_date", endDate);
        }
        wrapper.orderByAsc("measurement_date");
        
        List<BodyMetric> metrics = bodyMetricMapper.selectList(wrapper);
        
        FitnessEffectAnalysisResponse analysis = new FitnessEffectAnalysisResponse();
        
        if (metrics.isEmpty()) {
            analysis.setWeightChange(0.0);
            analysis.setBodyFatChange(0.0);
            analysis.setBmiChange(0.0);
            return analysis;
        }
        
        BodyMetric first = metrics.get(0);
        BodyMetric last = metrics.get(metrics.size() - 1);
        
        analysis.setWeightChange(last.getWeightKg() - first.getWeightKg());
        analysis.setBodyFatChange(last.getBodyFatPercentage() - first.getBodyFatPercentage());
        analysis.setBmiChange(last.getBmi() - first.getBmi());
        analysis.setMetrics(metrics);
        
        return analysis;
    }

    public LeaderboardResponse getLeaderboard(String type, int limit) {
        QueryWrapper<Leaderboard> wrapper = new QueryWrapper<>();
        wrapper.eq("leaderboard_type", type)
               .orderByAsc("rank")
               .last("LIMIT " + limit);
        
        List<Leaderboard> leaderboards = leaderboardMapper.selectList(wrapper);
        
        LeaderboardResponse response = new LeaderboardResponse();
        response.setType(type);
        response.setEntries(leaderboards.stream()
                .map(this::convertToLeaderboardEntry)
                .collect(Collectors.toList()));
        
        return response;
    }

    public PeakHourWarningResponse getPeakHourWarning() {
        // Get today's exercise records
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("exercise_date", LocalDate.now());
        List<ExerciseRecord> todayRecords = exerciseRecordMapper.selectList(wrapper);
        
        // Count users by hour (simplified - using created_at hour)
        Map<Integer, Long> hourCounts = todayRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> record.getCreatedAt().getHour(),
                        Collectors.counting()
                ));
        
        PeakHourWarningResponse response = new PeakHourWarningResponse();
        
        if (hourCounts.isEmpty()) {
            response.setIsPeakHour(false);
            response.setCurrentCount(0);
            response.setThreshold(50);
            return response;
        }
        
        int currentHour = LocalDate.now().atStartOfDay().getHour();
        long currentCount = hourCounts.getOrDefault(currentHour, 0L);
        int threshold = 50;
        
        response.setIsPeakHour(currentCount > threshold);
        response.setCurrentCount((int) currentCount);
        response.setThreshold(threshold);
        response.setPeakHour(hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0));
        
        return response;
    }

    public EquipmentUsageResponse getEquipmentUsage() {
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(null);
        
        Map<String, Long> equipmentCounts = records.stream()
                .filter(r -> r.getEquipmentUsed() != null && !r.getEquipmentUsed().isEmpty())
                .collect(Collectors.groupingBy(ExerciseRecord::getEquipmentUsed, Collectors.counting()));
        
        EquipmentUsageResponse response = new EquipmentUsageResponse();
        response.setEquipmentUsage(equipmentCounts);
        response.setTotalUsage(equipmentCounts.values().stream().mapToLong(Long::longValue).sum());
        
        return response;
    }

    private LeaderboardEntry convertToLeaderboardEntry(Leaderboard leaderboard) {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setUserId(leaderboard.getUserId());
        entry.setRank(leaderboard.getRank());
        entry.setValue(leaderboard.getValue());
        
        // Get user info
        User user = userMapper.selectById(leaderboard.getUserId());
        if (user != null) {
            entry.setUsername(user.getUsername());
            entry.setRealName(user.getRealName());
        }
        
        return entry;
    }

    public HourlyActivityResponse getHourlyActivity(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("exercise_date", date);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        
        // Group by hour
        Map<Integer, List<ExerciseRecord>> hourGroups = records.stream()
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().getHour()));
        
        HourlyActivityResponse response = new HourlyActivityResponse();
        List<HourlyActivityResponse.HourlyData> hourlyData = new ArrayList<>();
        
        int peakHour = 0;
        int peakCount = 0;
        
        for (int hour = 0; hour < 24; hour++) {
            HourlyActivityResponse.HourlyData data = new HourlyActivityResponse.HourlyData();
            data.setHour(hour);
            List<ExerciseRecord> hourRecords = hourGroups.getOrDefault(hour, Collections.emptyList());
            data.setCount(hourRecords.size());
            data.setDuration(hourRecords.stream().mapToInt(ExerciseRecord::getDurationMinutes).sum());
            hourlyData.add(data);
            
            if (hourRecords.size() > peakCount) {
                peakCount = hourRecords.size();
                peakHour = hour;
            }
        }
        
        response.setHourlyData(hourlyData);
        response.setPeakHour(peakHour);
        response.setPeakCount(peakCount);
        
        return response;
    }

    public ExercisePreferenceResponse getExercisePreference(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        if (startDate != null) {
            wrapper.ge("exercise_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("exercise_date", endDate);
        }
        
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        
        Map<String, Long> typeCounts = records.stream()
                .collect(Collectors.groupingBy(ExerciseRecord::getExerciseType, Collectors.counting()));
        
        long total = typeCounts.values().stream().mapToLong(Long::longValue).sum();
        
        ExercisePreferenceResponse response = new ExercisePreferenceResponse();
        List<ExercisePreferenceResponse.PreferenceData> preferences = new ArrayList<>();
        
        String mostPopular = null;
        long maxCount = 0;
        
        for (Map.Entry<String, Long> entry : typeCounts.entrySet()) {
            ExercisePreferenceResponse.PreferenceData data = new ExercisePreferenceResponse.PreferenceData();
            data.setExerciseType(entry.getKey());
            data.setCount(entry.getValue().intValue());
            data.setPercentage(total > 0 ? (entry.getValue() * 100.0 / total) : 0);
            preferences.add(data);
            
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostPopular = entry.getKey();
            }
        }
        
        response.setPreferences(preferences);
        response.setMostPopular(mostPopular);
        response.setTotalUsers((int) records.stream().map(ExerciseRecord::getUserId).distinct().count());
        
        return response;
    }
}
