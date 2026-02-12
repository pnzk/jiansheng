package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.ExerciseRecord;
import com.gym.fitness.mapper.ExerciseRecordMapper;
import com.gym.fitness.service.dto.exercise.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    
    private final ExerciseRecordMapper exerciseRecordMapper;

    public ExerciseRecordResponse addExerciseRecord(Long userId, AddExerciseRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }

        String exerciseType = normalizeExerciseType(request.getExerciseType());
        Integer durationMinutes = request.getDurationMinutes();
        if (durationMinutes == null || durationMinutes <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "运动时长必须大于0");
        }

        LocalDate exerciseDate = request.getExerciseDate() == null ? LocalDate.now() : request.getExerciseDate();
        double met = getMetByExerciseType(exerciseType);
        double estimatedCalories = Math.round(durationMinutes * met * 0.9 * 10.0) / 10.0;
        int estimatedAvgHeartRate = estimateAverageHeartRate(met);

        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(userId);
        record.setExerciseType(exerciseType);
        record.setExerciseDate(exerciseDate);
        record.setDurationMinutes(durationMinutes);
        record.setCaloriesBurned(request.getCaloriesBurned() == null ? estimatedCalories : request.getCaloriesBurned());
        record.setAverageHeartRate(request.getAverageHeartRate() == null ? estimatedAvgHeartRate : request.getAverageHeartRate());
        record.setMaxHeartRate(request.getMaxHeartRate() == null ? estimatedAvgHeartRate + 25 : request.getMaxHeartRate());
        record.setEquipmentUsed(trimToNull(request.getEquipmentUsed()));
        record.setNotes(trimToNull(request.getNotes()));
        record.setCreatedAt(LocalDateTime.now());

        exerciseRecordMapper.insert(record);
        return convertToResponse(record);
    }

    public List<ExerciseRecordResponse> getUserExerciseRecords(Long userId, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        
        if (startDate != null) {
            wrapper.ge("exercise_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("exercise_date", endDate);
        }
        
        wrapper.orderByDesc("exercise_date");
        
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ExerciseStatisticsResponse getExerciseStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<ExerciseRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        
        if (startDate != null) {
            wrapper.ge("exercise_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("exercise_date", endDate);
        }
        
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);

        ExerciseStatisticsResponse stats = new ExerciseStatisticsResponse();
        stats.setTotalRecords(records.size());
        stats.setTotalDurationMinutes(records.stream()
                .mapToInt(record -> record.getDurationMinutes() == null ? 0 : record.getDurationMinutes())
                .sum());
        stats.setTotalCaloriesBurned(records.stream()
                .mapToDouble(record -> record.getCaloriesBurned() == null ? 0D : record.getCaloriesBurned())
                .sum());
        stats.setAverageDurationMinutes(records.isEmpty() ? 0 : stats.getTotalDurationMinutes() / records.size());
        stats.setAverageCaloriesPerSession(records.isEmpty() ? 0 : stats.getTotalCaloriesBurned() / records.size());

        return stats;
    }

    public void deleteExerciseRecord(Long userId, Long recordId) {
        ExerciseRecord record = exerciseRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "运动记录不存在");
        }
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除此记录");
        }
        
        exerciseRecordMapper.deleteById(recordId);
    }

    private ExerciseRecordResponse convertToResponse(ExerciseRecord record) {
        ExerciseRecordResponse response = new ExerciseRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setExerciseType(record.getExerciseType());
        response.setExerciseDate(record.getExerciseDate());
        response.setDurationMinutes(record.getDurationMinutes());
        response.setCaloriesBurned(record.getCaloriesBurned());
        response.setAverageHeartRate(record.getAverageHeartRate());
        response.setMaxHeartRate(record.getMaxHeartRate());
        response.setEquipmentUsed(record.getEquipmentUsed());
        response.setNotes(record.getNotes());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private String normalizeExerciseType(String exerciseType) {
        if (!StringUtils.hasText(exerciseType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "运动类型不能为空");
        }

        String normalized = exerciseType.trim();
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private double getMetByExerciseType(String exerciseType) {
        String type = exerciseType.toUpperCase();
        if (type.contains("RUN") || type.contains("JOG")) {
            return 10.0;
        }
        if (type.contains("SWIM")) {
            return 9.0;
        }
        if (type.contains("CYCLE") || type.contains("BIKE")) {
            return 8.0;
        }
        if (type.contains("STRENGTH") || type.contains("WEIGHT") || type.contains("LIFT")) {
            return 6.0;
        }
        if (type.contains("YOGA") || type.contains("PILATES") || type.contains("STRETCH")) {
            return 4.0;
        }
        if (type.contains("WALK")) {
            return 3.5;
        }
        return 5.0;
    }

    private int estimateAverageHeartRate(double met) {
        if (met >= 9) {
            return 145;
        }
        if (met >= 7) {
            return 135;
        }
        if (met >= 5) {
            return 125;
        }
        return 115;
    }
}
