package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.ExerciseService;
import com.gym.fitness.service.dto.exercise.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exercise")
@RequiredArgsConstructor
public class ExerciseController {
    
    private final ExerciseService exerciseService;
    private final JwtUtil jwtUtil;

    @PostMapping("/add")
    public Result<ExerciseRecordResponse> addExerciseRecord(
            @RequestHeader("Authorization") String token,
            @RequestBody AddExerciseRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        ExerciseRecordResponse response = exerciseService.addExerciseRecord(userId, request);
        return Result.success(response);
    }

    @GetMapping("/records")
    public Result<List<ExerciseRecordResponse>> getUserExerciseRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<ExerciseRecordResponse> records = exerciseService.getUserExerciseRecords(userId, startDate, endDate);
        return Result.success(records);
    }

    @GetMapping("/statistics")
    public Result<ExerciseStatisticsResponse> getExerciseStatistics(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        ExerciseStatisticsResponse stats = exerciseService.getExerciseStatistics(userId, startDate, endDate);
        return Result.success(stats);
    }

    @DeleteMapping("/{recordId}")
    public Result<Void> deleteExerciseRecord(
            @RequestHeader("Authorization") String token,
            @PathVariable Long recordId) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        exerciseService.deleteExerciseRecord(userId, recordId);
        return Result.success(null);
    }
}
