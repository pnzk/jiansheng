package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.TrainingPlan;
import com.gym.fitness.entity.User;
import com.gym.fitness.mapper.TrainingPlanMapper;
import com.gym.fitness.mapper.UserMapper;
import com.gym.fitness.service.dto.plan.CreatePlanRequest;
import com.gym.fitness.service.dto.plan.ProgressUpdateRequest;
import com.gym.fitness.service.dto.plan.TrainingPlanResponse;
import com.gym.fitness.service.dto.plan.UpdatePlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanMapper trainingPlanMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public TrainingPlanResponse createTrainingPlan(Long coachId, CreatePlanRequest request) {
        validateCoachStudentRelation(coachId, request.getStudentId());
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "训练计划日期不能为空");
        }
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
        plan.setWeeklySchedule(normalizeWeeklySchedule(request.getWeeklySchedule()));
        plan.setDescription(request.getDescription());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());

        trainingPlanMapper.insert(plan);
        return convertToResponse(plan);
    }

    public TrainingPlanResponse updateTrainingPlan(Long planId, UpdatePlanRequest request) {
        return updateTrainingPlan(null, planId, request);
    }

    public TrainingPlanResponse updateTrainingPlan(Long coachId, Long planId, UpdatePlanRequest request) {
        TrainingPlan plan = getRequiredPlan(planId);

        if (coachId != null) {
            if (plan.getCoachId() == null || !coachId.equals(plan.getCoachId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改该训练计划");
            }
            validateCoachStudentRelation(coachId, request.getStudentId() != null ? request.getStudentId() : plan.getStudentId());
        }

        if (request.getStudentId() != null) {
            plan.setStudentId(request.getStudentId());
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
        if (request.getStartDate() != null) {
            plan.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            if (plan.getStartDate() != null && request.getEndDate().isBefore(plan.getStartDate())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "结束日期不能早于开始日期");
            }
            plan.setEndDate(request.getEndDate());
        }
        if (request.getWeeklySchedule() != null) {
            plan.setWeeklySchedule(normalizeWeeklySchedule(request.getWeeklySchedule()));
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
        TrainingPlan plan = getRequiredPlan(planId);
        trainingPlanMapper.deleteById(plan.getId());
    }

    public TrainingPlanResponse getStudentTrainingPlan(Long studentId) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("student_id", studentId)
                .eq("status", "ACTIVE")
                .orderByDesc("created_at")
                .last("LIMIT 1");

        TrainingPlan plan = trainingPlanMapper.selectOne(wrapper);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "暂无活动的训练计划");
        }

        return convertToResponse(plan);
    }

    public List<TrainingPlanResponse> getCoachTrainingPlans(Long coachId) {
        QueryWrapper<TrainingPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .orderByDesc("created_at");

        return trainingPlanMapper.selectList(wrapper)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public void updatePlanProgress(Long planId, ProgressUpdateRequest request) {
        TrainingPlan plan = getRequiredPlan(planId);

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

    private TrainingPlan getRequiredPlan(Long planId) {
        TrainingPlan plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "训练计划不存在");
        }
        return plan;
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

    private String normalizeWeeklySchedule(String weeklySchedule) {
        if (weeklySchedule == null || weeklySchedule.trim().isEmpty()) {
            return null;
        }

        String trimmed = weeklySchedule.trim();
        try {
            JsonNode parsed = objectMapper.readTree(trimmed);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception ignored) {
            try {
                return objectMapper.writeValueAsString(trimmed);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "周训练安排格式不合法");
            }
        }
    }

    private void validateCoachStudentRelation(Long coachId, Long studentId) {
        if (coachId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "教练ID不能为空");
        }
        if (studentId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "学员ID不能为空");
        }

        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        if (student.getRole() != null && !"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标用户不是学员");
        }
        if (student.getCoachId() == null || !coachId.equals(student.getCoachId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能为自己名下的学员维护计划");
        }
    }
}
