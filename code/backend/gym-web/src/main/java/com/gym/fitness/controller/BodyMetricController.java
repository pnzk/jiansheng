package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.BodyMetricService;
import com.gym.fitness.service.dto.bodymetric.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bodymetric")
@RequiredArgsConstructor
public class BodyMetricController {
    
    private final BodyMetricService bodyMetricService;
    private final JwtUtil jwtUtil;

    @PostMapping("/add")
    public Result<BodyMetricResponse> addBodyMetric(
            @RequestHeader("Authorization") String token,
            @RequestBody AddBodyMetricRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BodyMetricResponse response = bodyMetricService.addBodyMetric(userId, request);
        return Result.success(response);
    }

    @GetMapping("/history")
    public Result<List<BodyMetricResponse>> getBodyMetricHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<BodyMetricResponse> history = bodyMetricService.getBodyMetricHistory(userId, startDate, endDate);
        return Result.success(history);
    }

    @GetMapping("/latest")
    public Result<BodyMetricResponse> getLatestBodyMetric(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BodyMetricResponse latest = bodyMetricService.getLatestBodyMetric(userId);
        return Result.success(latest);
    }

    @GetMapping("/bmi")
    public Result<Double> calculateBMI(
            @RequestParam Double height,
            @RequestParam Double weight) {
        double bmi = bodyMetricService.calculateBMI(height, weight);
        return Result.success(bmi);
    }
}
