package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.domain.entity.Achievement;
import com.gym.fitness.domain.entity.UserAchievement;
import com.gym.fitness.domain.entity.ExerciseRecord;
import com.gym.fitness.repository.mapper.AchievementMapper;
import com.gym.fitness.repository.mapper.UserAchievementMapper;
import com.gym.fitness.repository.mapper.ExerciseRecordMapper;
import com.gym.fitness.service.dto.achievement.AchievementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {
    
    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;

    public List<AchievementResponse> getUserAchievements(Long userId) {
        // Get all achievements
        List<Achievement> allAchievements = achievementMapper.selectList(null);
        
        // Get user's unlocked achievements
        QueryWrapper<UserAchievement> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserAchievement> userAchievements = userAchievementMapper.selectList(wrapper);
        
        Set<Long> unlockedIds = userAchievements.stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());
        
        return allAchievements.stream()
                .map(achievement -> {
                    AchievementResponse response = convertToResponse(achievement);
                    response.setUnlocked(unlockedIds.contains(achievement.getId()));
                    
                    // Find unlock time if unlocked
                    userAchievements.stream()
                            .filter(ua -> ua.getAchievementId().equals(achievement.getId()))
                            .findFirst()
                            .ifPresent(ua -> response.setUnlockedAt(ua.getUnlockedAt()));
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    public void checkAndUnlockAchievements(Long userId) {
        // Get all achievements
        List<Achievement> allAchievements = achievementMapper.selectList(null);
        
        // Get user's already unlocked achievements
        QueryWrapper<UserAchievement> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        List<UserAchievement> userAchievements = userAchievementMapper.selectList(wrapper);
        
        Set<Long> unlockedIds = userAchievements.stream()
                .map(UserAchievement::getAchievementId)
                .collect(Collectors.toSet());
        
        // Get user's exercise statistics
        QueryWrapper<ExerciseRecord> exerciseWrapper = new QueryWrapper<>();
        exerciseWrapper.eq("user_id", userId);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(exerciseWrapper);
        
        int totalExercises = records.size();
        double totalCalories = records.stream().mapToDouble(ExerciseRecord::getCaloriesBurned).sum();
        int totalDuration = records.stream().mapToInt(ExerciseRecord::getDurationMinutes).sum();
        
        // Check each achievement
        for (Achievement achievement : allAchievements) {
            if (unlockedIds.contains(achievement.getId())) {
                continue; // Already unlocked
            }
            
            boolean shouldUnlock = false;
            
            switch (achievement.getAchievementType()) {
                case "EXERCISE_COUNT":
                    shouldUnlock = totalExercises >= achievement.getThresholdValue();
                    break;
                case "CALORIES":
                    shouldUnlock = totalCalories >= achievement.getThresholdValue();
                    break;
                case "DURATION":
                    shouldUnlock = totalDuration >= achievement.getThresholdValue();
                    break;
            }
            
            if (shouldUnlock) {
                unlockAchievement(userId, achievement.getId());
            }
        }
    }

    public List<AchievementResponse> getAllAchievements() {
        List<Achievement> achievements = achievementMapper.selectList(null);
        return achievements.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void unlockAchievement(Long userId, Long achievementId) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUserId(userId);
        userAchievement.setAchievementId(achievementId);
        userAchievement.setUnlockedAt(LocalDateTime.now());
        
        userAchievementMapper.insert(userAchievement);
    }

    private AchievementResponse convertToResponse(Achievement achievement) {
        AchievementResponse response = new AchievementResponse();
        response.setId(achievement.getId());
        response.setAchievementName(achievement.getAchievementName());
        response.setDescription(achievement.getDescription());
        response.setAchievementType(achievement.getAchievementType());
        response.setThresholdValue(achievement.getThresholdValue());
        response.setIconUrl(achievement.getIconUrl());
        response.setUnlocked(false); // Default, will be set by caller
        return response;
    }
}
