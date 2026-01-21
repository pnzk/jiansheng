package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.AnalyticsService;
import com.gym.fitness.service.dto.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final JwtUtil jwtUtil;

    @GetMapping("/dashboard")
    public Result<DashboardStatisticsResponse> getDashboardStatistics() {
        DashboardStatisticsResponse stats = analyticsService.getDashboardStatistics();
        return Result.success(stats);
    }

    @GetMapping("/behavior")
    public Result<UserBehaviorAnalysisResponse> getUserBehaviorAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        UserBehaviorAnalysisResponse analysis = analyticsService.getUserBehaviorAnalysis(startDate, endDate);
        return Result.success(analysis);
    }

    @GetMapping("/fitness-effect")
    public Result<FitnessEffectAnalysisResponse> getFitnessEffectAnalysis(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        FitnessEffectAnalysisResponse analysis = analyticsService.getFitnessEffectAnalysis(userId, startDate, endDate);
        return Result.success(analysis);
    }

    @GetMapping("/leaderboard")
    public Result<LeaderboardResponse> getLeaderboard(
            @RequestParam String type,
            @RequestParam(defaultValue = "10") int limit) {
        LeaderboardResponse leaderboard = analyticsService.getLeaderboard(type, limit);
        return Result.success(leaderboard);
    }

    @GetMapping("/peak-hour")
    public Result<PeakHourWarningResponse> getPeakHourWarning() {
        PeakHourWarningResponse warning = analyticsService.getPeakHourWarning();
        return Result.success(warning);
    }

    @GetMapping("/equipment-usage")
    public Result<EquipmentUsageResponse> getEquipmentUsage() {
        EquipmentUsageResponse usage = analyticsService.getEquipmentUsage();
        return Result.success(usage);
    }

    @GetMapping("/hourly-activity")
    public Result<HourlyActivityResponse> getHourlyActivity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        HourlyActivityResponse activity = analyticsService.getHourlyActivity(date);
        return Result.success(activity);
    }

    @GetMapping("/exercise-preference")
    public Result<ExercisePreferenceResponse> getExercisePreference(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ExercisePreferenceResponse preference = analyticsService.getExercisePreference(startDate, endDate);
        return Result.success(preference);
    }
}
