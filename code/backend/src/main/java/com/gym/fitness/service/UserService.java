package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.CoachTodoAction;
import com.gym.fitness.entity.User;
import com.gym.fitness.mapper.CoachTodoActionMapper;
import com.gym.fitness.mapper.UserMapper;
import com.gym.fitness.service.dto.user.ChangePasswordRequest;
import com.gym.fitness.service.dto.user.CoachTodoActionResponse;
import com.gym.fitness.service.dto.user.CoachTodoHandleRequest;
import com.gym.fitness.service.dto.user.PrivacySettingsRequest;
import com.gym.fitness.service.dto.user.UpdateProfileRequest;
import com.gym.fitness.service.dto.user.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        ensureEmailAvailable(request.getEmail(), userId);

        user.setRealName(trimToNull(request.getRealName()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setPhone(trimToNull(request.getPhone()));
        user.setAge(request.getAge());
        user.setGender(normalizeGender(request.getGender()));
        user.setFitnessGoal(normalizeFitnessGoal(request.getFitnessGoal()));
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

        String normalizedTodoKey = normalizeTodoKey(request.getStudentId(), request.getTodoKey());
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
            action.setTodoDescription(trimToNull(request.getTodoDescription()));
            action.setHandledAt(LocalDateTime.now());
            action.setUpdatedAt(LocalDateTime.now());
            coachTodoActionMapper.insert(action);
            return;
        }

        existing.setTodoTitle(normalizedTodoTitle);
        existing.setTodoDescription(trimToNull(request.getTodoDescription()));
        existing.setHandledAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        coachTodoActionMapper.updateById(existing);
    }

    public boolean hasHandledCoachTodo(Long coachId, Long studentId, String todoKey) {
        if (coachId == null || studentId == null) {
            return false;
        }

        QueryWrapper<CoachTodoAction> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .eq("student_id", studentId)
                .eq("todo_key", normalizeTodoKey(studentId, todoKey));
        return coachTodoActionMapper.selectCount(wrapper) > 0;
    }

    public List<CoachTodoActionResponse> getHandledCoachTodos(Long coachId) {
        QueryWrapper<CoachTodoAction> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .orderByDesc("updated_at");
        return coachTodoActionMapper.selectList(wrapper).stream()
                .map(action -> {
                    CoachTodoActionResponse response = new CoachTodoActionResponse();
                    response.setStudentId(action.getStudentId());
                    response.setTodoKey(action.getTodoKey());
                    response.setTodoTitle(action.getTodoTitle());
                    response.setTodoDescription(action.getTodoDescription());
                    response.setHandledAt(action.getHandledAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private String normalizeTodoKey(Long studentId, String todoKey) {
        String raw = StringUtils.hasText(todoKey) ? todoKey.trim() : "";
        if (raw.startsWith("INACTIVE_DAYS::")
                || raw.startsWith("NO_EXERCISE_RECORD::")
                || raw.startsWith("NO_ACTIVE_PLAN::")
                || raw.startsWith("LOW_PROGRESS::")
                || raw.startsWith("FOLLOW_UP_")) {
            return raw;
        }

        if (raw.contains("未运动")) {
            return "INACTIVE_DAYS::" + studentId;
        }
        if (raw.contains("暂无运动记录")) {
            return "NO_EXERCISE_RECORD::" + studentId;
        }
        if (raw.contains("暂无进行中的训练计划")) {
            return "NO_ACTIVE_PLAN::" + studentId;
        }
        if (raw.contains("计划完成率偏低")) {
            return "LOW_PROGRESS::" + studentId;
        }

        return "FOLLOW_UP_" + studentId;
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

    private void ensureEmailAvailable(String email, Long excludeUserId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", trimToNull(email));
        if (excludeUserId != null) {
            wrapper.ne("id", excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被占用");
        }
    }

    private String normalizeGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "性别不能为空");
        }

        String normalized = gender.trim().toUpperCase();
        if ("男".equals(gender) || "MALE".equals(normalized)) {
            return "MALE";
        }
        if ("女".equals(gender) || "FEMALE".equals(normalized)) {
            return "FEMALE";
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "性别仅支持 MALE/FEMALE");
    }

    private String normalizeFitnessGoal(String fitnessGoal) {
        if (!StringUtils.hasText(fitnessGoal)) {
            return null;
        }

        String normalized = fitnessGoal.trim().toUpperCase();
        if ("BODY_SHAPING".equals(normalized)) {
            return "FAT_LOSS";
        }
        if ("HEALTH".equals(normalized)) {
            return "WEIGHT_LOSS";
        }
        if ("WEIGHT_LOSS".equals(normalized)
                || "FAT_LOSS".equals(normalized)
                || "MUSCLE_GAIN".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "健身目标仅支持 WEIGHT_LOSS/FAT_LOSS/MUSCLE_GAIN");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
