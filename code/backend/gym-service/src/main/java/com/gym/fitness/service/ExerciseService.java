package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.domain.entity.ExerciseRecord;
import com.gym.fitness.repository.mapper.ExerciseRecordMapper;
import com.gym.fitness.service.dto.exercise.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseService {
    
    private final ExerciseRecordMapper exerciseRecordMapper;

    public ExerciseRecordResponse addExerciseRecord(Long userId, AddExerciseRequest request) {
        ExerciseRecord record = new ExerciseRecord();
        record.setUserId(userId);
        record.setExerciseType(request.getExerciseType());
        record.setExerciseDate(request.getExerciseDate());
        record.setDurationMinutes(request.getDurationMinutes());
        record.setCaloriesBurned(request.getCaloriesBurned());
        record.setAverageHeartRate(request.getAverageHeartRate());
        record.setMaxHeartRate(request.getMaxHeartRate());
        record.setEquipmentUsed(request.getEquipmentUsed());
        record.setNotes(request.getNotes());
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
        stats.setTotalDurationMinutes(records.stream().mapToInt(ExerciseRecord::getDurationMinutes).sum());
        stats.setTotalCaloriesBurned(records.stream().mapToDouble(ExerciseRecord::getCaloriesBurned).sum());
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
}
