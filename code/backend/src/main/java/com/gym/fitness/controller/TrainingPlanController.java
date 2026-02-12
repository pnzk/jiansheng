package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.TrainingPlanService;
import com.gym.fitness.service.dto.plan.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class TrainingPlanController {
    
    private final TrainingPlanService trainingPlanService;
    private final JwtUtil jwtUtil;

    @PostMapping("/create")
    public Result<TrainingPlanResponse> createTrainingPlan(
            @RequestHeader("Authorization") String token,
            @RequestBody CreatePlanRequest request) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        TrainingPlanResponse response = trainingPlanService.createTrainingPlan(coachId, request);
        return Result.success(response);
    }

    @PutMapping("/{planId}")
    public Result<TrainingPlanResponse> updateTrainingPlan(
            @PathVariable Long planId,
            @RequestBody UpdatePlanRequest request) {
        TrainingPlanResponse response = trainingPlanService.updateTrainingPlan(planId, request);
        return Result.success(response);
    }

    @DeleteMapping("/{planId}")
    public Result<Void> deleteTrainingPlan(@PathVariable Long planId) {
        trainingPlanService.deleteTrainingPlan(planId);
        return Result.success(null);
    }

    @GetMapping("/student/{studentId}")
    public Result<TrainingPlanResponse> getStudentTrainingPlan(@PathVariable Long studentId) {
        TrainingPlanResponse response = trainingPlanService.getStudentTrainingPlan(studentId);
        return Result.success(response);
    }

    @GetMapping("/my")
    public Result<TrainingPlanResponse> getMyTrainingPlan(
            @RequestHeader("Authorization") String token) {
        Long studentId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        TrainingPlanResponse response = trainingPlanService.getStudentTrainingPlan(studentId);
        return Result.success(response);
    }

    @GetMapping("/coach")
    public Result<List<TrainingPlanResponse>> getCoachTrainingPlans(
            @RequestHeader("Authorization") String token) {
        Long coachId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<TrainingPlanResponse> plans = trainingPlanService.getCoachTrainingPlans(coachId);
        return Result.success(plans);
    }

    @PutMapping("/{planId}/progress")
    public Result<Void> updatePlanProgress(
            @PathVariable Long planId,
            @RequestBody ProgressUpdateRequest request) {
        trainingPlanService.updatePlanProgress(planId, request);
        return Result.success(null);
    }
}
