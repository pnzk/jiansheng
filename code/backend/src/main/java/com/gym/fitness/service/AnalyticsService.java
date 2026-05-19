package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.*;
import com.gym.fitness.mapper.*;
import com.gym.fitness.service.dto.analytics.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    
    private final UserMapper userMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final BodyMetricMapper bodyMetricMapper;
    private final LeaderboardMapper leaderboardMapper;
    private final TrainingPlanMapper trainingPlanMapper;

    private static final int DEFAULT_RECENT_DAYS = 30;
    private static final int DEFAULT_WEEK_DAYS = 7;
    private static final long CACHE_TTL_MILLIS = 30_000L;
    private final Map<String, CacheEntry<?>> analyticsCache = new ConcurrentHashMap<>();

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get();
    }

    public CoachDashboardResponse getCoachDashboard(Long coachId) {
        if (coachId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教练ID不能为空");
        }

        QueryWrapper<User> studentWrapper = new QueryWrapper<>();
        studentWrapper.eq("coach_id", coachId)
                .in("user_role", "STUDENT", "student");
        List<User> students = userMapper.selectList(studentWrapper);

        CoachDashboardResponse response = new CoachDashboardResponse();
        response.setPeriodEnd(LocalDate.now());
        response.setPeriodStart(LocalDate.now().minusDays(30));

        response.setTotalStudents(students.size());
        response.setMaleStudents((int) students.stream()
                .filter(student -> "MALE".equalsIgnoreCase(student.getGender()))
                .count());
        response.setFemaleStudents((int) students.stream()
                .filter(student -> "FEMALE".equalsIgnoreCase(student.getGender()))
                .count());

        List<Integer> ages = students.stream()
                .map(User::getAge)
                .filter(Objects::nonNull)
                .filter(age -> age > 0)
                .collect(Collectors.toList());
        int avgAge = ages.isEmpty()
                ? 0
                : (int) Math.round(ages.stream().mapToInt(Integer::intValue).average().orElse(0));
        response.setAvgAge(avgAge);

        Map<String, Integer> goalDistribution = students.stream()
                .collect(Collectors.groupingBy(
                        student -> normalizeGoal(student.getFitnessGoal()),
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        response.setGoalDistribution(goalDistribution);

        List<Long> studentIds = students.stream().map(User::getId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            response.setActiveStudents(0);
            response.setExerciseTypeDistribution(new LinkedHashMap<>());
            response.setWeightTrend(Collections.emptyList());
            return response;
        }

        LocalDate periodStart = response.getPeriodStart();
        LocalDate periodEnd = response.getPeriodEnd();

        QueryWrapper<ExerciseRecord> exerciseWrapper = new QueryWrapper<>();
        exerciseWrapper.in("user_id", studentIds)
                .between("exercise_date", periodStart, periodEnd);
        List<ExerciseRecord> recentRecords = exerciseRecordMapper.selectList(exerciseWrapper);

        response.setActiveStudents((int) recentRecords.stream()
                .map(ExerciseRecord::getUserId)
                .distinct()
                .count());

        Map<String, Integer> exerciseTypeDistribution = recentRecords.stream()
                .filter(record -> hasText(record.getExerciseType()))
                .collect(Collectors.groupingBy(
                        ExerciseRecord::getExerciseType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
        response.setExerciseTypeDistribution(exerciseTypeDistribution);

        QueryWrapper<BodyMetric> metricWrapper = new QueryWrapper<>();
        metricWrapper.in("user_id", studentIds)
                .between("measurement_date", periodStart, periodEnd)
                .orderByAsc("measurement_date", "id");
        List<BodyMetric> metrics = bodyMetricMapper.selectList(metricWrapper);

        Map<LocalDate, Double> avgWeightByDate = metrics.stream()
                .filter(metric -> metric.getMeasurementDate() != null)
                .filter(metric -> metric.getWeightKg() != null)
                .collect(Collectors.groupingBy(
                        BodyMetric::getMeasurementDate,
                        TreeMap::new,
                        Collectors.averagingDouble(BodyMetric::getWeightKg)
                ));

        List<CoachDashboardResponse.WeightTrendPoint> weightTrend = avgWeightByDate.entrySet().stream()
                .map(entry -> {
                    CoachDashboardResponse.WeightTrendPoint point = new CoachDashboardResponse.WeightTrendPoint();
                    point.setDate(entry.getKey());
                    point.setAvgWeight(roundTwo(entry.getValue()));
                    return point;
                })
                .collect(Collectors.toList());
        response.setWeightTrend(weightTrend);

        return response;
    }

    public DashboardStatisticsResponse getDashboardStatistics() {
        return getDashboardStatistics(null, null);
    }

    public DashboardStatisticsResponse getDashboardStatistics(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("dashboard", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeDashboardStatistics(startDate, endDate));
    }

    private DashboardStatisticsResponse computeDashboardStatistics(LocalDate startDate, LocalDate endDate) {
        DashboardStatisticsResponse stats = new DashboardStatisticsResponse();

        stats.setTotalUsers(userMapper.selectCount(null).intValue());

        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_RECENT_DAYS);
        Integer activeUsers = exerciseRecordMapper.countDistinctUsersInRange(range.getStartDate(), range.getEndDate());
        stats.setActiveUsers(activeUsers == null ? 0 : activeUsers);

        Long totalDuration = exerciseRecordMapper.sumDurationInRange(range.getStartDate(), range.getEndDate());
        stats.setTotalDurationMinutes(totalDuration == null ? 0 : totalDuration.intValue());

        Double totalCalories = exerciseRecordMapper.sumCaloriesInRange(range.getStartDate(), range.getEndDate());
        stats.setTotalCaloriesBurned(roundTwo(totalCalories == null ? 0.0 : totalCalories));
        stats.setPeriodStart(range.getStartDate());
        stats.setPeriodEnd(range.getEndDate());
        stats.setFallbackApplied(range.isFallbackApplied());
        
        return stats;
    }

    public UserBehaviorAnalysisResponse getUserBehaviorAnalysis(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("behavior", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeUserBehaviorAnalysis(startDate, endDate));
    }

    private UserBehaviorAnalysisResponse computeUserBehaviorAnalysis(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_WEEK_DAYS);
        LocalDate actualStartDate = range.getStartDate();
        LocalDate actualEndDate = range.getEndDate();

        UserBehaviorAnalysisResponse analysis = new UserBehaviorAnalysisResponse();
        analysis.setPeriodStart(actualStartDate);
        analysis.setPeriodEnd(actualEndDate);
        analysis.setFallbackApplied(range.isFallbackApplied());

        List<Map<String, Object>> grouped = exerciseRecordMapper.countByExerciseType(actualStartDate, actualEndDate);
        Map<String, Long> exerciseTypeCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : grouped) {
            String type = row.get("type") == null ? "UNKNOWN" : String.valueOf(row.get("type"));
            Number countNumber = (Number) row.get("cnt");
            long count = countNumber == null ? 0L : countNumber.longValue();
            exerciseTypeCounts.put(type, count);
        }

        analysis.setMostPopularExercise(exerciseTypeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A"));

        // Exercise type distribution
        analysis.setExerciseTypeDistribution(exerciseTypeCounts);

        // Average duration
        Double avgDuration = exerciseRecordMapper.avgDurationInRange(actualStartDate, actualEndDate);
        analysis.setAverageDurationMinutes(avgDuration == null ? 0 : avgDuration);

        // Active user count
        Integer activeUserCount = exerciseRecordMapper.countDistinctUsersInRange(actualStartDate, actualEndDate);
        analysis.setActiveUserCount(activeUserCount == null ? 0 : activeUserCount);

        QueryWrapper<ExerciseRecord> summaryWrapper = new QueryWrapper<>();
        summaryWrapper.between("exercise_date", actualStartDate, actualEndDate);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(summaryWrapper);
        analysis.setTotalDurationMinutes(records.stream()
                .map(ExerciseRecord::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum());
        analysis.setTotalCaloriesBurned(roundTwo(records.stream()
                .map(ExerciseRecord::getCaloriesBurned)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum()));

        LocalDate retentionWindowStart = actualStartDate.minusDays(30);
        QueryWrapper<ExerciseRecord> retentionWrapper = new QueryWrapper<>();
        retentionWrapper.select("user_id", "exercise_date")
                .between("exercise_date", retentionWindowStart, actualEndDate);
        List<ExerciseRecord> retentionRecords = exerciseRecordMapper.selectList(retentionWrapper);

        Map<Long, Set<LocalDate>> activityDatesByUser = new HashMap<>();
        for (ExerciseRecord record : retentionRecords) {
            if (record.getUserId() == null || record.getExerciseDate() == null) {
                continue;
            }
            activityDatesByUser
                    .computeIfAbsent(record.getUserId(), key -> new HashSet<>())
                    .add(record.getExerciseDate());
        }

        List<Map<String, Object>> dailyRows = exerciseRecordMapper.summarizeDailyActivity(actualStartDate, actualEndDate);
        Map<LocalDate, Map<String, Object>> dailyRowMap = dailyRows.stream()
                .map(row -> new AbstractMap.SimpleEntry<>(toLocalDate(row.get("activityDate")), row))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right, LinkedHashMap::new));

        List<UserBehaviorAnalysisResponse.DailyActivityPoint> dailyActivity = new ArrayList<>();
        LocalDate cursor = actualStartDate;
        while (!cursor.isAfter(actualEndDate)) {
            Map<String, Object> row = dailyRowMap.get(cursor);
            UserBehaviorAnalysisResponse.DailyActivityPoint point = new UserBehaviorAnalysisResponse.DailyActivityPoint();
            point.setDate(cursor);
            point.setActiveUserCount(getIntValue(row, "activeUserCount"));
            point.setAverageDurationMinutes(roundTwo(getDoubleValue(row, "averageDurationMinutes")));
            point.setTotalDurationMinutes(getIntValue(row, "totalDurationMinutes"));
            point.setTotalCaloriesBurned(roundTwo(getDoubleValue(row, "totalCaloriesBurned")));
            dailyActivity.add(point);
            cursor = cursor.plusDays(1);
        }
        analysis.setDailyActivity(dailyActivity);
        analysis.setRetentionRates(buildRetentionRates(activityDatesByUser, actualStartDate, actualEndDate));
        analysis.setAverageActiveRate(calculateAverageActiveRate(dailyActivity));
        analysis.setAveragePlanCompletionRate(calculateAveragePlanCompletionRate(actualStartDate, actualEndDate));

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
               .orderByAsc("`rank`");
        
        List<Leaderboard> leaderboards = leaderboardMapper.selectList(wrapper);

        List<LeaderboardEntry> entries = leaderboards.stream()
                .map(this::convertToLeaderboardEntry)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());

        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).setRank(index + 1);
        }
        
        LeaderboardResponse response = new LeaderboardResponse();
        response.setType(type);
        response.setEntries(entries);
        
        return response;
    }

    public LeaderboardResponse getLeaderboard(String type, int limit, LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("leaderboard:" + type + ":" + limit, startDate, endDate);
        return getOrCompute(cacheKey, () -> computeLeaderboard(type, limit, startDate, endDate));
    }

    private LeaderboardResponse computeLeaderboard(String type, int limit, LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_RECENT_DAYS);
        LocalDate actualStartDate = range.getStartDate();
        LocalDate actualEndDate = range.getEndDate();

        List<LeaderboardEntry> entries;
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        switch (normalizedType) {
            case "TOTAL_DURATION":
                entries = buildRangeLeaderboardEntries(
                        exerciseRecordMapper.sumDurationByUserInRange(actualStartDate, actualEndDate, limit),
                        limit
                );
                break;
            case "TOTAL_CALORIES":
                entries = buildRangeLeaderboardEntries(
                        exerciseRecordMapper.sumCaloriesByUserInRange(actualStartDate, actualEndDate, limit),
                        limit
                );
                break;
            case "WEIGHT_LOSS":
                entries = buildRangeLeaderboardEntries(
                        bodyMetricMapper.sumWeightLossByUserInRange(actualStartDate, actualEndDate, limit),
                        limit
                );
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "排行榜类型不支持");
        }

        LeaderboardResponse response = new LeaderboardResponse();
        response.setType(normalizedType);
        response.setPeriodStart(actualStartDate);
        response.setPeriodEnd(actualEndDate);
        response.setFallbackApplied(range.isFallbackApplied());
        response.setEntries(entries);
        return response;
    }

    public PeakHourWarningResponse getPeakHourWarning() {
        return getPeakHourWarning(LocalDate.now(), LocalDate.now());
    }

    public PeakHourWarningResponse getPeakHourWarning(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("peak-hour", startDate, endDate);
        return getOrCompute(cacheKey, () -> computePeakHourWarning(startDate, endDate));
    }

    private PeakHourWarningResponse computePeakHourWarning(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, 1);
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.between("exercise_date", range.getStartDate(), range.getEndDate());
        List<ExerciseRecord> todayRecords = exerciseRecordMapper.selectList(wrapper);
        
        Map<Integer, Set<Long>> hourUserSets = new HashMap<>();
        for (ExerciseRecord record : todayRecords) {
            int hour = resolveActivityHour(record);
            hourUserSets.computeIfAbsent(hour, key -> new HashSet<>());
            if (record.getUserId() != null) {
                hourUserSets.get(hour).add(record.getUserId());
            }
        }

        Map<Integer, Long> hourCounts = hourUserSets.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));
        
        PeakHourWarningResponse response = new PeakHourWarningResponse();
        response.setPeriodStart(range.getStartDate());
        response.setPeriodEnd(range.getEndDate());
        response.setFallbackApplied(range.isFallbackApplied());
        
        if (hourCounts.isEmpty()) {
            response.setIsPeakHour(false);
            response.setCurrentCount(0);
            response.setThreshold(50);
            response.setPeakCount(0);
            return response;
        }
        
        int currentHour = resolveCurrentHourForRange(range);
        long currentCount = hourCounts.getOrDefault(currentHour, 0L);
        int threshold = 50;
        
        response.setIsPeakHour(currentCount > threshold);
        response.setCurrentCount((int) currentCount);
        response.setThreshold(threshold);
        response.setPeakCount((int) hourCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L));
        response.setPeakHour(hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0));
        
        return response;
    }

    public List<CoachWorkloadResponse> getCoachWorkload() {
        QueryWrapper<User> coachWrapper = new QueryWrapper<>();
        coachWrapper.in("user_role", "COACH", "coach");
        List<User> coaches = userMapper.selectList(coachWrapper);

        QueryWrapper<User> studentWrapper = new QueryWrapper<>();
        studentWrapper.in("user_role", "STUDENT", "student");
        List<User> students = userMapper.selectList(studentWrapper);

        QueryWrapper<TrainingPlan> planWrapper = new QueryWrapper<>();
        List<TrainingPlan> plans = trainingPlanMapper.selectList(planWrapper);

        Map<Long, List<User>> studentsByCoach = students.stream()
                .filter(student -> student.getCoachId() != null)
                .collect(Collectors.groupingBy(User::getCoachId));

        Map<Long, List<TrainingPlan>> plansByCoach = plans.stream()
                .filter(plan -> plan.getCoachId() != null)
                .collect(Collectors.groupingBy(TrainingPlan::getCoachId));

        return coaches.stream().map(coach -> {
            List<User> coachStudents = studentsByCoach.getOrDefault(coach.getId(), Collections.emptyList());
            List<TrainingPlan> coachPlans = plansByCoach.getOrDefault(coach.getId(), Collections.emptyList());

            long activeStudents = coachStudents.stream()
                    .filter(student -> hasActivePlan(student.getId(), coachPlans))
                    .count();

            double avgProgress = coachPlans.stream()
                    .map(TrainingPlan::getCompletionRate)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            CoachWorkloadResponse response = new CoachWorkloadResponse();
            response.setCoachId(coach.getId());
            response.setCoachName(hasText(coach.getRealName()) ? coach.getRealName() : coach.getUsername());
            response.setStudentCount(coachStudents.size());
            response.setPlanCount(coachPlans.size());
            response.setActiveStudents((int) activeStudents);
            response.setAvgProgress(Math.round(avgProgress * 100.0) / 100.0);
            return response;
        }).sorted(Comparator.comparing(CoachWorkloadResponse::getStudentCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<CoachStudentReportResponse> getCoachStudentReport(Long coachId,
                                                                  List<Long> studentIds,
                                                                  LocalDate startDate,
                                                                  LocalDate endDate) {
        if (coachId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教练ID不能为空");
        }
        if (studentIds == null || studentIds.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);
        if (effectiveStart.isAfter(effectiveEnd)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "开始日期不能晚于结束日期");
        }

        QueryWrapper<User> studentWrapper = new QueryWrapper<>();
        studentWrapper.in("id", studentIds)
                .eq("coach_id", coachId)
                .in("user_role", "STUDENT", "student");
        List<User> students = userMapper.selectList(studentWrapper);
        Map<Long, User> studentMap = students.stream().collect(Collectors.toMap(User::getId, user -> user));

        List<CoachStudentReportResponse> report = new ArrayList<>();
        for (Long studentId : studentIds) {
            User student = studentMap.get(studentId);
            if (student == null) {
                continue;
            }

            QueryWrapper<BodyMetric> metricWrapper = new QueryWrapper<>();
            metricWrapper.eq("user_id", studentId)
                    .between("measurement_date", effectiveStart, effectiveEnd)
                    .orderByAsc("measurement_date");
            List<BodyMetric> metrics = bodyMetricMapper.selectList(metricWrapper);

            Double startWeight = null;
            Double currentWeight = null;
            Double weightChange = 0.0;
            if (!metrics.isEmpty()) {
                startWeight = metrics.get(0).getWeightKg();
                currentWeight = metrics.get(metrics.size() - 1).getWeightKg();
                if (startWeight != null && currentWeight != null) {
                    weightChange = roundTwo(currentWeight - startWeight);
                }
            }

            QueryWrapper<ExerciseRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("user_id", studentId)
                    .between("exercise_date", effectiveStart, effectiveEnd);
            List<ExerciseRecord> records = exerciseRecordMapper.selectList(recordWrapper);

            int totalDuration = records.stream()
                    .map(ExerciseRecord::getDurationMinutes)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            double totalCalories = records.stream()
                    .map(ExerciseRecord::getCaloriesBurned)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            int exerciseCount = records.size();
            int avgDuration = exerciseCount > 0 ? Math.round((float) totalDuration / exerciseCount) : 0;

            QueryWrapper<TrainingPlan> planWrapper = new QueryWrapper<>();
            planWrapper.eq("student_id", studentId)
                    .eq("coach_id", coachId)
                    .orderByDesc("updated_at")
                    .last("LIMIT 1");
            TrainingPlan latestPlan = trainingPlanMapper.selectOne(planWrapper);
            double planProgress = latestPlan != null && latestPlan.getCompletionRate() != null
                    ? latestPlan.getCompletionRate()
                    : 0.0;

            CoachStudentReportResponse row = new CoachStudentReportResponse();
            row.setStudentId(studentId);
            row.setStudentName(hasText(student.getRealName()) ? student.getRealName() : student.getUsername());
            row.setStartWeight(startWeight);
            row.setCurrentWeight(currentWeight);
            row.setWeightChange(weightChange);
            row.setTotalDuration(totalDuration);
            row.setTotalCalories(roundTwo(totalCalories));
            row.setExerciseCount(exerciseCount);
            row.setAvgDuration(avgDuration);
            row.setPlanProgress(roundTwo(planProgress));
            report.add(row);
        }

        return report;
    }

    public List<ExerciseRecord> getCoachStudentExerciseRecords(Long coachId,
                                                               Long studentId,
                                                               LocalDate startDate,
                                                               LocalDate endDate) {
        validateCoachStudentRelation(coachId, studentId);

        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", studentId)
                .orderByDesc("exercise_date", "id");
        if (startDate != null) {
            wrapper.ge("exercise_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("exercise_date", endDate);
        }

        return exerciseRecordMapper.selectList(wrapper);
    }

    public List<BodyMetric> getCoachStudentBodyMetrics(Long coachId,
                                                       Long studentId,
                                                       LocalDate startDate,
                                                       LocalDate endDate) {
        validateCoachStudentRelation(coachId, studentId);

        QueryWrapper<BodyMetric> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", studentId)
                .orderByAsc("measurement_date", "id");
        if (startDate != null) {
            wrapper.ge("measurement_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("measurement_date", endDate);
        }

        return bodyMetricMapper.selectList(wrapper);
    }

    public EquipmentUsageResponse getEquipmentUsage() {
        return getEquipmentUsage(null, null);
    }

    public EquipmentUsageResponse getEquipmentUsage(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("equipment-usage", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeEquipmentUsage(startDate, endDate));
    }

    private EquipmentUsageResponse computeEquipmentUsage(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_RECENT_DAYS);
        Map<String, Long> equipmentCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : exerciseRecordMapper.countByEquipmentUsedInRange(range.getStartDate(), range.getEndDate())) {
            String equipment = row.get("equipment") == null ? "UNKNOWN" : String.valueOf(row.get("equipment"));
            long count = row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L;
            equipmentCounts.put(equipment, count);
        }
        
        EquipmentUsageResponse response = new EquipmentUsageResponse();
        response.setEquipmentUsage(equipmentCounts);
        response.setTotalUsage(equipmentCounts.values().stream().mapToLong(Long::longValue).sum());
        response.setPeriodStart(range.getStartDate());
        response.setPeriodEnd(range.getEndDate());
        response.setFallbackApplied(range.isFallbackApplied());
        
        return response;
    }

    private LeaderboardEntry convertToLeaderboardEntry(Leaderboard leaderboard) {
        User user = userMapper.selectById(leaderboard.getUserId());
        if (user == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(user.getShowInLeaderboard())) {
            return null;
        }
        if (user.getRole() != null && !"STUDENT".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setUserId(leaderboard.getUserId());
        entry.setRank(leaderboard.getRank());
        entry.setValue(leaderboard.getValue());

        entry.setUsername(user.getUsername());
        entry.setRealName(user.getRealName());
        
        return entry;
    }

    private List<LeaderboardEntry> buildRangeLeaderboardEntries(List<Map<String, Object>> rows, int limit) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long userId = getLongValue(row, "userId");
            if (userId == null) {
                continue;
            }

            User user = userMapper.selectById(userId);
            if (user == null || !Boolean.TRUE.equals(user.getShowInLeaderboard())) {
                continue;
            }
            if (user.getRole() != null && !"STUDENT".equalsIgnoreCase(user.getRole())) {
                continue;
            }

            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setUserId(userId);
            entry.setUsername(user.getUsername());
            entry.setRealName(user.getRealName());
            entry.setValue(roundTwo(getDoubleValue(row, "totalValue")));
            entries.add(entry);
        }

        entries.sort(Comparator.comparing(LeaderboardEntry::getValue, Comparator.nullsLast(Comparator.reverseOrder())));
        if (entries.size() > limit) {
            entries = new ArrayList<>(entries.subList(0, limit));
        }
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        return entries;
    }

    public HourlyActivityResponse getHourlyActivity(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("hourly-activity", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeHourlyActivity(startDate, endDate));
    }

    private HourlyActivityResponse computeHourlyActivity(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_WEEK_DAYS);
        LocalDate actualStartDate = range.getStartDate();
        LocalDate actualEndDate = range.getEndDate();
        List<Map<String, Object>> rows = exerciseRecordMapper.summarizeHourlyActivity(actualStartDate, actualEndDate);
        Map<Integer, Integer> hourUsers = new HashMap<>();
        Map<Integer, Integer> hourDurations = new HashMap<>();
        for (Map<String, Object> row : rows) {
            int hour = getIntValue(row, "activityHour");
            hourUsers.put(hour, getIntValue(row, "userCount"));
            hourDurations.put(hour, getIntValue(row, "totalDuration"));
        }
        
        HourlyActivityResponse response = new HourlyActivityResponse();
        List<HourlyActivityResponse.HourlyData> hourlyData = new ArrayList<>();
        
        int peakHour = 0;
        int peakCount = 0;
        
        for (int hour = 0; hour < 24; hour++) {
            HourlyActivityResponse.HourlyData data = new HourlyActivityResponse.HourlyData();
            data.setHour(hour);
            int count = hourUsers.getOrDefault(hour, 0);
            int duration = hourDurations.getOrDefault(hour, 0);
            data.setCount(count);
            data.setDuration(duration);
            hourlyData.add(data);
            
            if (count > peakCount) {
                peakCount = count;
                peakHour = hour;
            }
        }
        
        response.setHourlyData(hourlyData);
        response.setPeakHour(peakHour);
        response.setPeakCount(peakCount);
        response.setPeriodStart(actualStartDate);
        response.setPeriodEnd(actualEndDate);
        response.setFallbackApplied(range.isFallbackApplied());
        
        return response;
    }

    public HourlyHeatmapResponse getHourlyHeatmap(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("hourly-heatmap", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeHourlyHeatmap(startDate, endDate));
    }

    private HourlyHeatmapResponse computeHourlyHeatmap(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_WEEK_DAYS);
        LocalDate actualStartDate = range.getStartDate();
        LocalDate actualEndDate = range.getEndDate();
        List<Map<String, Object>> rows = exerciseRecordMapper.summarizeHeatmapActivity(actualStartDate, actualEndDate);
        Map<LocalDate, Map<Integer, Integer>> dayHourUsers = new LinkedHashMap<>();
        LocalDate cursor = actualStartDate;
        int dayIndex = 0;
        List<String> dayLabels = new ArrayList<>();
        Map<LocalDate, Integer> dayIndexMap = new HashMap<>();

        while (!cursor.isAfter(actualEndDate)) {
            dayHourUsers.put(cursor, new HashMap<>());
            dayIndexMap.put(cursor, dayIndex++);
            dayLabels.add(cursor.toString());
            cursor = cursor.plusDays(1);
        }

        for (Map<String, Object> row : rows) {
            LocalDate activityDate = toLocalDate(row.get("activityDate"));
            if (activityDate == null || !dayHourUsers.containsKey(activityDate)) {
                continue;
            }
            int hour = getIntValue(row, "activityHour");
            dayHourUsers.get(activityDate).put(hour, getIntValue(row, "userCount"));
        }

        List<HourlyHeatmapResponse.HeatmapPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<Integer, Integer>> entry : dayHourUsers.entrySet()) {
            int currentDayIndex = dayIndexMap.get(entry.getKey());
            for (int hour = 0; hour < 24; hour++) {
                HourlyHeatmapResponse.HeatmapPoint point = new HourlyHeatmapResponse.HeatmapPoint();
                point.setHour(hour);
                point.setDayIndex(currentDayIndex);
                point.setCount(entry.getValue().getOrDefault(hour, 0));
                points.add(point);
            }
        }

        HourlyHeatmapResponse response = new HourlyHeatmapResponse();
        response.setDayLabels(dayLabels);
        response.setPoints(points);
        response.setPeriodStart(actualStartDate);
        response.setPeriodEnd(actualEndDate);
        response.setFallbackApplied(range.isFallbackApplied());
        return response;
    }

    private int resolveActivityHour(ExerciseRecord record) {
        if (record == null || record.getCreatedAt() == null) {
            return 18;
        }

        int hour = record.getCreatedAt().getHour();
        if (hour < 6 || hour > 22) {
            return 18;
        }
        return hour;
    }

    private int resolveCurrentHourForRange(DateRangeResolution range) {
        if (range == null || range.getEndDate() == null) {
            return LocalTime.now().getHour();
        }
        if (range.getEndDate().isEqual(LocalDate.now())) {
            return LocalTime.now().getHour();
        }
        return 18;
    }

    private List<UserBehaviorAnalysisResponse.RetentionPoint> buildRetentionRates(
            Map<Long, Set<LocalDate>> activityDatesByUser,
            LocalDate startDate,
            LocalDate endDate
    ) {
        int[] checkpoints = {1, 3, 7, 14, 30};
        if (activityDatesByUser == null || activityDatesByUser.isEmpty()) {
            return Arrays.stream(checkpoints)
                    .mapToObj(days -> createRetentionPoint(days, 0, 0))
                    .collect(Collectors.toList());
        }

        Map<LocalDate, Set<Long>> usersByDate = new HashMap<>();
        for (Map.Entry<Long, Set<LocalDate>> entry : activityDatesByUser.entrySet()) {
            Long userId = entry.getKey();
            if (userId == null || entry.getValue() == null) {
                continue;
            }
            for (LocalDate activeDate : entry.getValue()) {
                if (activeDate == null) {
                    continue;
                }
                usersByDate.computeIfAbsent(activeDate, key -> new HashSet<>()).add(userId);
            }
        }

        List<UserBehaviorAnalysisResponse.RetentionPoint> retentionPoints = new ArrayList<>();
        for (int checkpoint : checkpoints) {
            int cohortSize = 0;
            int retainedUsers = 0;
            LocalDate cohortCursor = startDate.minusDays(checkpoint);
            LocalDate cohortEnd = endDate.minusDays(checkpoint);

            while (!cohortCursor.isAfter(cohortEnd)) {
                Set<Long> cohortUsers = usersByDate.getOrDefault(cohortCursor, Collections.emptySet());
                Set<Long> returnUsers = usersByDate.getOrDefault(cohortCursor.plusDays(checkpoint), Collections.emptySet());
                cohortSize += cohortUsers.size();
                if (!cohortUsers.isEmpty() && !returnUsers.isEmpty()) {
                    for (Long userId : cohortUsers) {
                        if (returnUsers.contains(userId)) {
                            retainedUsers++;
                        }
                    }
                }
                cohortCursor = cohortCursor.plusDays(1);
            }

            retentionPoints.add(createRetentionPoint(
                    checkpoint,
                    cohortSize,
                    retainedUsers
            ));
        }
        return retentionPoints;
    }

    private UserBehaviorAnalysisResponse.RetentionPoint createRetentionPoint(int days, int cohortSize, int retainedUsers) {
        UserBehaviorAnalysisResponse.RetentionPoint point = new UserBehaviorAnalysisResponse.RetentionPoint();
        point.setDays(days);
        point.setLabel(days + "日");
        point.setCohortSize(cohortSize);
        point.setRetainedUsers(retainedUsers);
        point.setRetentionRate(cohortSize <= 0 ? 0.0 : roundTwo(retainedUsers * 100.0 / cohortSize));
        return point;
    }

    private double calculateAverageActiveRate(List<UserBehaviorAnalysisResponse.DailyActivityPoint> dailyActivity) {
        long totalStudents = countStudentUsers();
        if (totalStudents <= 0 || dailyActivity == null || dailyActivity.isEmpty()) {
            return 0.0;
        }

        double average = dailyActivity.stream()
                .mapToDouble(point -> {
                    int activeUsers = point == null || point.getActiveUserCount() == null ? 0 : point.getActiveUserCount();
                    return activeUsers * 100.0 / totalStudents;
                })
                .average()
                .orElse(0.0);
        return roundTwo(average);
    }

    private double calculateAveragePlanCompletionRate(LocalDate startDate, LocalDate endDate) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.in("status", "ACTIVE", "COMPLETED");
        if (startDate != null) {
            wrapper.le("start_date", endDate == null ? startDate : endDate);
        }
        if (endDate != null) {
            wrapper.ge("end_date", startDate == null ? endDate : startDate);
        }

        List<TrainingPlan> plans = trainingPlanMapper.selectList(wrapper);
        if (plans.isEmpty()) {
            return 0.0;
        }

        double average = plans.stream()
                .map(TrainingPlan::getCompletionRate)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return roundTwo(average);
    }

    public ExercisePreferenceResponse getExercisePreference(LocalDate startDate, LocalDate endDate) {
        String cacheKey = buildCacheKey("exercise-preference", startDate, endDate);
        return getOrCompute(cacheKey, () -> computeExercisePreference(startDate, endDate));
    }

    private ExercisePreferenceResponse computeExercisePreference(LocalDate startDate, LocalDate endDate) {
        DateRangeResolution range = resolveRequestedDateRange(startDate, endDate, DEFAULT_RECENT_DAYS);
        List<Map<String, Object>> grouped = exerciseRecordMapper.countByExerciseType(range.getStartDate(), range.getEndDate());
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : grouped) {
            String type = row.get("type") == null ? "UNKNOWN" : String.valueOf(row.get("type"));
            Number countNumber = (Number) row.get("cnt");
            long count = countNumber == null ? 0L : countNumber.longValue();
            typeCounts.put(type, count);
        }
        
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
        Integer totalUsers = exerciseRecordMapper.countDistinctUsersInRange(range.getStartDate(), range.getEndDate());
        response.setTotalUsers(totalUsers == null ? 0 : totalUsers);
        response.setPeriodStart(range.getStartDate());
        response.setPeriodEnd(range.getEndDate());
        response.setFallbackApplied(range.isFallbackApplied());
        
        return response;
    }

    private DateRangeResolution resolveRecentDateRange(int days) {
        return resolveRequestedDateRange(null, null, days);
    }

    private DateRangeResolution resolveRequestedDateRange(LocalDate startDate, LocalDate endDate, int defaultDays) {
        LocalDate latestExerciseDate = getLatestExerciseDate();
        LocalDate effectiveLatest = latestExerciseDate != null ? latestExerciseDate : LocalDate.now();

        LocalDate actualEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStartDate = startDate == null ? actualEndDate.minusDays(Math.max(defaultDays - 1L, 0L)) : startDate;
        if (actualStartDate.isAfter(actualEndDate)) {
            LocalDate swap = actualStartDate;
            actualStartDate = actualEndDate;
            actualEndDate = swap;
        }

        boolean fallbackApplied = false;
        if (latestExerciseDate != null && actualEndDate.isAfter(effectiveLatest)) {
            long span = ChronoUnit.DAYS.between(actualStartDate, actualEndDate);
            actualEndDate = effectiveLatest;
            actualStartDate = actualEndDate.minusDays(Math.max(span, 0L));
            fallbackApplied = true;
        }

        return new DateRangeResolution(actualStartDate, actualEndDate, fallbackApplied);
    }

    private String buildCacheKey(String prefix, LocalDate startDate, LocalDate endDate) {
        return prefix + "::" + (startDate == null ? "null" : startDate) + "::" + (endDate == null ? "null" : endDate);
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCompute(String cacheKey, SupplierWithException<T> supplier) {
        long now = System.currentTimeMillis();
        CacheEntry<?> cached = analyticsCache.get(cacheKey);
        if (cached != null && cached.expiresAt > now) {
            return (T) cached.value;
        }

        try {
            T value = supplier.get();
            analyticsCache.put(cacheKey, new CacheEntry<>(value, now + CACHE_TTL_MILLIS));
            if (analyticsCache.size() > 256) {
                cleanupExpiredCacheEntries(now);
            }
            return value;
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private void cleanupExpiredCacheEntries(long now) {
        analyticsCache.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private LocalDate getLatestExerciseDate() {
        QueryWrapper<ExerciseRecord> latestDateWrapper = new QueryWrapper<>();
        latestDateWrapper.select("exercise_date")
                .orderByDesc("exercise_date")
                .last("LIMIT 1");
        ExerciseRecord latestRecord = exerciseRecordMapper.selectOne(latestDateWrapper);
        return latestRecord == null ? null : latestRecord.getExerciseDate();
    }

    @lombok.Value
    private static class DateRangeResolution {
        LocalDate startDate;
        LocalDate endDate;
        boolean fallbackApplied;
    }

    @lombok.Value
    private static class CacheEntry<T> {
        T value;
        long expiresAt;
    }

    private boolean hasActivePlan(Long studentId, List<TrainingPlan> coachPlans) {
        return coachPlans.stream().anyMatch(plan ->
                Objects.equals(plan.getStudentId(), studentId)
                        && plan.getStatus() != null
                        && "ACTIVE".equalsIgnoreCase(plan.getStatus())
        );
    }

    private void validateCoachStudentRelation(Long coachId, Long studentId) {
        if (coachId == null || studentId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }

        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        if (student.getRole() != null && !"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标用户不是学员");
        }
        if (!Objects.equals(student.getCoachId(), coachId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该学员数据");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private long countStudentUsers() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_role", "STUDENT");
        return userMapper.selectCount(wrapper);
    }

    private String normalizeGoal(String goal) {
        if ("FAT_LOSS".equalsIgnoreCase(goal)) {
            return "FAT_LOSS";
        }
        if ("MUSCLE_GAIN".equalsIgnoreCase(goal)) {
            return "MUSCLE_GAIN";
        }
        return "WEIGHT_LOSS";
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private int getIntValue(Map<String, Object> row, String key) {
        if (row == null) {
            return 0;
        }
        Object value = row.get(key);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private double getDoubleValue(Map<String, Object> row, String key) {
        if (row == null) {
            return 0.0;
        }
        Object value = row.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        Object value = row.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        try {
            return LocalDate.parse(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }
}
