package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.BodyMetric;
import com.gym.fitness.mapper.BodyMetricMapper;
import com.gym.fitness.service.dto.bodymetric.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BodyMetricService {
    
    private final BodyMetricMapper bodyMetricMapper;

    public BodyMetricResponse addBodyMetric(Long userId, AddBodyMetricRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }

        LocalDate measurementDate = request.getMeasurementDate() != null
                ? request.getMeasurementDate()
                : LocalDate.now();

        validateMetricRequest(request, measurementDate);

        QueryWrapper<BodyMetric> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("user_id", userId)
                .eq("measurement_date", measurementDate)
                .last("LIMIT 1");
        BodyMetric metric = bodyMetricMapper.selectOne(existingWrapper);

        boolean isUpdate = metric != null;
        if (!isUpdate) {
            metric = new BodyMetric();
            metric.setUserId(userId);
            metric.setMeasurementDate(measurementDate);
            metric.setCreatedAt(LocalDateTime.now());
        }

        metric.setWeightKg(request.getWeightKg());
        metric.setBodyFatPercentage(request.getBodyFatPercentage());
        metric.setHeightCm(request.getHeightCm());
        metric.setMuscleMassKg(request.getMuscleMassKg());

        // Calculate BMI
        if (request.getHeightCm() != null && request.getWeightKg() != null) {
            metric.setBmi(calculateBMI(request.getHeightCm(), request.getWeightKg()));
        } else {
            metric.setBmi(null);
        }

        if (isUpdate) {
            bodyMetricMapper.updateById(metric);
        } else {
            bodyMetricMapper.insert(metric);
        }

        return convertToResponse(metric);
    }

    private void validateMetricRequest(AddBodyMetricRequest request, LocalDate measurementDate) {
        if (measurementDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "测量日期不能晚于今天");
        }

        if (request.getWeightKg() == null || request.getWeightKg() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "体重必须大于0");
        }

        if (request.getBodyFatPercentage() != null
                && (request.getBodyFatPercentage() < 0 || request.getBodyFatPercentage() > 100)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "体脂率范围应在0-100之间");
        }

        if (request.getHeightCm() != null && request.getHeightCm() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "身高必须大于0");
        }

        if (request.getMuscleMassKg() != null && request.getMuscleMassKg() < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "肌肉量不能小于0");
        }
    }

    public List<BodyMetricResponse> getBodyMetricHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        QueryWrapper<BodyMetric> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        
        if (startDate != null) {
            wrapper.ge("measurement_date", startDate);
        }
        if (endDate != null) {
            wrapper.le("measurement_date", endDate);
        }
        
        wrapper.orderByDesc("measurement_date");
        
        List<BodyMetric> metrics = bodyMetricMapper.selectList(wrapper);
        return metrics.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public BodyMetricResponse getLatestBodyMetric(Long userId) {
        QueryWrapper<BodyMetric> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .orderByDesc("measurement_date")
               .last("LIMIT 1");
        
        BodyMetric metric = bodyMetricMapper.selectOne(wrapper);
        if (metric == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无身体指标数据");
        }
        
        return convertToResponse(metric);
    }

    public double calculateBMI(double heightCm, double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "身高和体重必须大于0");
        }
        
        if (heightCm < 50 || heightCm > 250) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "身高数据异常");
        }
        
        if (weightKg < 20 || weightKg > 300) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "体重数据异常");
        }
        
        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        
        return Math.round(bmi * 100.0) / 100.0;
    }

    private BodyMetricResponse convertToResponse(BodyMetric metric) {
        BodyMetricResponse response = new BodyMetricResponse();
        response.setId(metric.getId());
        response.setUserId(metric.getUserId());
        response.setMeasurementDate(metric.getMeasurementDate());
        response.setWeightKg(metric.getWeightKg());
        response.setBodyFatPercentage(metric.getBodyFatPercentage());
        response.setHeightCm(metric.getHeightCm());
        response.setBmi(metric.getBmi());
        response.setMuscleMassKg(metric.getMuscleMassKg());
        response.setCreatedAt(metric.getCreatedAt());
        return response;
    }
}
