package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.TrainingPlan;
import com.gym.fitness.mapper.TrainingPlanMapper;
import com.gym.fitness.service.dto.plan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {
    
    private final TrainingPlanMapper trainingPlanMapper;

    public TrainingPlanResponse createTrainingPlan(Long coachId, CreatePlanRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
        }
        
        TrainingPlan plan = new TrainingPlan();
        plan.setStudentId(request.getStudentId());
        plan.setCoachId(coachId);
        plan.setPlanName(request.getPlanName());
        plan.setGoalType(request.getGoalType());
        plan.setTargetValue(request.getTargetValue());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setStatus("ACTIVE");
        plan.setCompletionRate(0.0);
        plan.setWeeklySchedule(request.getWeeklySchedule());
        plan.setDescription(request.getDescription());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        
        trainingPlanMapper.insert(plan);
        return convertToResponse(plan);
    }

    public TrainingPlanResponse updateTrainingPlan(Long planId, UpdatePlanRequest request) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "训练计划不存在");
        }
        
        if (request.getPlanName() != null) {
            plan.setPlanName(request.getPlanName());
        }
        if (request.getGoalType() != null) {
            plan.setGoalType(request.getGoalType());
        }
        if (request.getTargetValue() != null) {
            plan.setTargetValue(request.getTargetValue());
        }
        if (request.getEndDate() != null) {
            if (request.getEndDate().isBefore(plan.getStartDate())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
            }
            plan.setEndDate(request.getEndDate());
        }
        if (request.getWeeklySchedule() != null) {
            plan.setWeeklySchedule(request.getWeeklySchedule());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            plan.setStatus(request.getStatus());
        }
        
        plan.setUpdatedAt(LocalDateTime.now());
        trainingPlanMapper.updateById(plan);
        
        return convertToResponse(plan);
    }

    public void deleteTrainingPlan(Long planId) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "训练计划不存在");
        }
        
        trainingPlanMapper.deleteById(planId);
    }

    public TrainingPlanResponse getStudentTrainingPlan(Long studentId) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("student_id", studentId)
               .eq("status", "ACTIVE")
               .orderByDesc("created_at")
               .last("LIMIT 1");
        
        TrainingPlan plan = trainingPlanMapper.selectOne(wrapper);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无活跃的训练计划");
        }
        
        return convertToResponse(plan);
    }

    public List<TrainingPlanResponse> getCoachTrainingPlans(Long coachId) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
               .orderByDesc("created_at");
        
        List<TrainingPlan> plans = trainingPlanMapper.selectList(wrapper);
        return plans.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public void updatePlanProgress(Long planId, ProgressUpdateRequest request) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "训练计划不存在");
        }
        
        double completionRate = request.getCompletionRate();
        if (completionRate < 0 || completionRate > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "完成率必须在0-100之间");
        }
        
        plan.setCompletionRate(completionRate);
        plan.setUpdatedAt(LocalDateTime.now());
        
        if (completionRate >= 100) {
            plan.setStatus("COMPLETED");
        }
        
        trainingPlanMapper.updateById(plan);
    }

    private TrainingPlanResponse convertToResponse(TrainingPlan plan) {
        TrainingPlanResponse response = new TrainingPlanResponse();
        response.setId(plan.getId());
        response.setStudentId(plan.getStudentId());
        response.setCoachId(plan.getCoachId());
        response.setPlanName(plan.getPlanName());
        response.setGoalType(plan.getGoalType());
        response.setTargetValue(plan.getTargetValue());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setStatus(plan.getStatus());
        response.setCompletionRate(plan.getCompletionRate());
        response.setWeeklySchedule(plan.getWeeklySchedule());
        response.setDescription(plan.getDescription());
        response.setCreatedAt(plan.getCreatedAt());
        response.setUpdatedAt(plan.getUpdatedAt());
        return response;
    }
}
