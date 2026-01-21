package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.domain.entity.User;
import com.gym.fitness.repository.mapper.UserMapper;
import com.gym.fitness.service.dto.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserProfileResponse getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToResponse(user);
    }

    public void updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
        user.setFitnessGoal(request.getFitnessGoal());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "原密码错误");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void updatePrivacySettings(Long userId, PrivacySettingsRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setShowInLeaderboard(request.getShowInLeaderboard());
        user.setAllowCoachView(request.getAllowCoachView());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(user);
    }

    private UserProfileResponse convertToResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRealName(user.getRealName());
        response.setAge(user.getAge());
        response.setGender(user.getGender());
        response.setRole(user.getRole());
        response.setFitnessGoal(user.getFitnessGoal());
        response.setCoachId(user.getCoachId());
        response.setShowInLeaderboard(user.getShowInLeaderboard());
        response.setAllowCoachView(user.getAllowCoachView());
        return response;
    }
}
