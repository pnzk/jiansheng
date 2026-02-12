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
        DashboardStatisticsResponse stats = new DashboardStatisticsResponse();
        
        // Total users
        stats.setTotalUsers(userMapper.selectCount(null).intValue());
        
        // Active users (exercised in last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        Integer activeUsers = exerciseRecordMapper.countDistinctUsersSince(thirtyDaysAgo);
        stats.setActiveUsers(activeUsers == null ? 0 : activeUsers);
        
        // Total exercise duration
        Long totalDuration = exerciseRecordMapper.sumDurationMinutes();
        stats.setTotalDurationMinutes(totalDuration == null ? 0 : totalDuration.intValue());
        
        // Total calories burned
        Double totalCalories = exerciseRecordMapper.sumCaloriesBurned();
        stats.setTotalCaloriesBurned(totalCalories == null ? 0 : totalCalories);
        
        return stats;
    }

    public UserBehaviorAnalysisResponse getUserBehaviorAnalysis(LocalDate startDate, LocalDate endDate) {
        UserBehaviorAnalysisResponse analysis = new UserBehaviorAnalysisResponse();

        List<Map<String, Object>> grouped = exerciseRecordMapper.countByExerciseType(startDate, endDate);
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
        Double avgDuration = exerciseRecordMapper.avgDurationInRange(startDate, endDate);
        analysis.setAverageDurationMinutes(avgDuration == null ? 0 : avgDuration);

        // Active user count
        Integer activeUserCount = exerciseRecordMapper.countDistinctUsersInRange(startDate, endDate);
        analysis.setActiveUserCount(activeUserCount == null ? 0 : activeUserCount);

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

    public PeakHourWarningResponse getPeakHourWarning() {
        // Get today's exercise records
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("exercise_date", LocalDate.now());
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
        
        if (hourCounts.isEmpty()) {
            response.setIsPeakHour(false);
            response.setCurrentCount(0);
            response.setThreshold(50);
            return response;
        }
        
        int currentHour = LocalTime.now().getHour();
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
        List<Map<String, Object>> grouped = exerciseRecordMapper.countByEquipmentUsed();
        Map<String, Long> equipmentCounts = new LinkedHashMap<>();
        for (Map<String, Object> row : grouped) {
            String equipment = row.get("equipment") == null ? "UNKNOWN" : String.valueOf(row.get("equipment"));
            Number countNumber = (Number) row.get("cnt");
            long count = countNumber == null ? 0L : countNumber.longValue();
            equipmentCounts.put(equipment, count);
        }
        
        EquipmentUsageResponse response = new EquipmentUsageResponse();
        response.setEquipmentUsage(equipmentCounts);
        response.setTotalUsage(equipmentCounts.values().stream().mapToLong(Long::longValue).sum());
        
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

    public HourlyActivityResponse getHourlyActivity(LocalDate startDate, LocalDate endDate) {
        LocalDate actualEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStartDate = startDate == null ? actualEndDate.minusDays(6) : startDate;
        if (actualStartDate.isAfter(actualEndDate)) {
            LocalDate swap = actualStartDate;
            actualStartDate = actualEndDate;
            actualEndDate = swap;
        }

        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.between("exercise_date", actualStartDate, actualEndDate);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);

        // Fallback: if selected date/range has no records, fallback to latest date with records.
        if (records.isEmpty()) {
            QueryWrapper<ExerciseRecord> latestDateWrapper = new QueryWrapper<>();
            latestDateWrapper.select("exercise_date")
                    .orderByDesc("exercise_date")
                    .last("LIMIT 1");
            ExerciseRecord latestRecord = exerciseRecordMapper.selectOne(latestDateWrapper);
            if (latestRecord != null && latestRecord.getExerciseDate() != null) {
                actualEndDate = latestRecord.getExerciseDate();
                if (startDate == null) {
                    actualStartDate = actualEndDate.minusDays(6);
                } else {
                    long span = ChronoUnit.DAYS.between(startDate, actualEndDate);
                    if (span < 0) {
                        actualStartDate = actualEndDate;
                    } else {
                        actualStartDate = actualEndDate.minusDays(span);
                    }
                }
                wrapper.clear();
                wrapper.between("exercise_date", actualStartDate, actualEndDate);
                records = exerciseRecordMapper.selectList(wrapper);
            }
        }
        
        Map<Integer, Set<Long>> hourUsers = new HashMap<>();
        Map<Integer, Integer> hourDurations = new HashMap<>();
        for (ExerciseRecord record : records) {
            int hour = resolveActivityHour(record);
            hourUsers.computeIfAbsent(hour, key -> new HashSet<>());
            if (record.getUserId() != null) {
                hourUsers.get(hour).add(record.getUserId());
            }
            hourDurations.merge(hour, Optional.ofNullable(record.getDurationMinutes()).orElse(0), Integer::sum);
        }
        
        HourlyActivityResponse response = new HourlyActivityResponse();
        List<HourlyActivityResponse.HourlyData> hourlyData = new ArrayList<>();
        
        int peakHour = 0;
        int peakCount = 0;
        
        for (int hour = 0; hour < 24; hour++) {
            HourlyActivityResponse.HourlyData data = new HourlyActivityResponse.HourlyData();
            data.setHour(hour);
            int count = hourUsers.getOrDefault(hour, Collections.emptySet()).size();
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

    public ExercisePreferenceResponse getExercisePreference(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> grouped = exerciseRecordMapper.countByExerciseType(startDate, endDate);
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
        Integer totalUsers = exerciseRecordMapper.countDistinctUsersInRange(startDate, endDate);
        response.setTotalUsers(totalUsers == null ? 0 : totalUsers);
        
        return response;
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
}
