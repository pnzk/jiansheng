package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.AchievementService;
import com.gym.fitness.service.dto.achievement.AchievementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achievement")
@RequiredArgsConstructor
public class AchievementController {
    
    private final AchievementService achievementService;
    private final JwtUtil jwtUtil;

    @GetMapping("/my")
    public Result<List<AchievementResponse>> getUserAchievements(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<AchievementResponse> achievements = achievementService.getUserAchievements(userId);
        return Result.success(achievements);
    }

    @PostMapping("/check")
    public Result<Void> checkAndUnlockAchievements(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        achievementService.checkAndUnlockAchievements(userId);
        return Result.success(null);
    }

    @GetMapping("/all")
    public Result<List<AchievementResponse>> getAllAchievements() {
        List<AchievementResponse> achievements = achievementService.getAllAchievements();
        return Result.success(achievements);
    }
}
