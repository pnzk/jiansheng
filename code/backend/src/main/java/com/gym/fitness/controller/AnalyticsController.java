package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.AnalyticsService;
import com.gym.fitness.service.dto.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.gym.fitness.entity.BodyMetric;
import com.gym.fitness.entity.ExerciseRecord;

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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate actualStartDate = startDate;
        LocalDate actualEndDate = endDate;

        if (actualStartDate == null && actualEndDate == null && date != null) {
            actualStartDate = date;
            actualEndDate = date;
        }

        HourlyActivityResponse activity = analyticsService.getHourlyActivity(actualStartDate, actualEndDate);
        return Result.success(activity);
    }

    @GetMapping("/exercise-preference")
    public Result<ExercisePreferenceResponse> getExercisePreference(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        ExercisePreferenceResponse preference = analyticsService.getExercisePreference(startDate, endDate);
        return Result.success(preference);
    }

    @GetMapping("/coach-workload")
    public Result<List<CoachWorkloadResponse>> getCoachWorkload() {
        return Result.success(analyticsService.getCoachWorkload());
    }

    @GetMapping("/coach-dashboard")
    public Result<CoachDashboardResponse> getCoachDashboard(
            @RequestHeader("Authorization") String token) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(analyticsService.getCoachDashboard(coachId));
    }

    @GetMapping("/coach-student-report")
    public Result<List<CoachStudentReportResponse>> getCoachStudentReport(
            @RequestHeader("Authorization") String token,
            @RequestParam String studentIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        List<Long> parsedStudentIds = parseStudentIds(studentIds);
        List<CoachStudentReportResponse> report = analyticsService.getCoachStudentReport(coachId, parsedStudentIds, startDate, endDate);
        return Result.success(report);
    }

    @GetMapping("/coach-student-exercise-records")
    public Result<List<ExerciseRecord>> getCoachStudentExerciseRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(analyticsService.getCoachStudentExerciseRecords(coachId, studentId, startDate, endDate));
    }

    @GetMapping("/coach-student-body-metrics")
    public Result<List<BodyMetric>> getCoachStudentBodyMetrics(
            @RequestHeader("Authorization") String token,
            @RequestParam Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        return Result.success(analyticsService.getCoachStudentBodyMetrics(coachId, studentId, startDate, endDate));
    }

    private List<Long> parseStudentIds(String rawStudentIds) {
        if (rawStudentIds == null || rawStudentIds.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rawStudentIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());
    }
}
