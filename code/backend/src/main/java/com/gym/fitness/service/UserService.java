package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.CoachTodoAction;
import com.gym.fitness.entity.User;
import com.gym.fitness.mapper.CoachTodoActionMapper;
import com.gym.fitness.mapper.UserMapper;
import com.gym.fitness.service.dto.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final CoachTodoActionMapper coachTodoActionMapper;
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

    public void handleCoachTodo(Long coachId, CoachTodoHandleRequest request) {
        User coach = userMapper.selectById(coachId);
        if (coach == null || coach.getRole() == null || !"COACH".equalsIgnoreCase(coach.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅教练可处理待办事项");
        }

        User student = userMapper.selectById(request.getStudentId());
        if (student == null || student.getRole() == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "待办关联的学员不存在");
        }
        if (!coachId.equals(student.getCoachId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权处理该学员待办事项");
        }

        String normalizedTodoKey = StringUtils.hasText(request.getTodoKey())
                ? request.getTodoKey().trim()
                : "FOLLOW_UP_" + request.getStudentId();
        String normalizedTodoTitle = StringUtils.hasText(request.getTodoTitle())
                ? request.getTodoTitle().trim()
                : "跟进学员状态";

        QueryWrapper<CoachTodoAction> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .eq("student_id", request.getStudentId())
                .eq("todo_key", normalizedTodoKey);
        CoachTodoAction existing = coachTodoActionMapper.selectOne(wrapper);

        if (existing == null) {
            CoachTodoAction action = new CoachTodoAction();
            action.setCoachId(coachId);
            action.setStudentId(request.getStudentId());
            action.setTodoKey(normalizedTodoKey);
            action.setTodoTitle(normalizedTodoTitle);
            action.setTodoDescription(request.getTodoDescription());
            action.setHandledAt(LocalDateTime.now());
            action.setUpdatedAt(LocalDateTime.now());
            coachTodoActionMapper.insert(action);
            return;
        }

        existing.setTodoTitle(normalizedTodoTitle);
        existing.setTodoDescription(request.getTodoDescription());
        existing.setHandledAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        coachTodoActionMapper.updateById(existing);
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
