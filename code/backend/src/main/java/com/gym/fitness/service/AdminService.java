package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.entity.BodyMetric;
import com.gym.fitness.entity.ExerciseRecord;
import com.gym.fitness.entity.TrainingPlan;
import com.gym.fitness.entity.User;
import com.gym.fitness.mapper.BodyMetricMapper;
import com.gym.fitness.mapper.ExerciseRecordMapper;
import com.gym.fitness.mapper.TrainingPlanMapper;
import com.gym.fitness.mapper.UserMapper;
import com.gym.fitness.service.dto.admin.CoachRequest;
import com.gym.fitness.service.dto.admin.CoachResponse;
import com.gym.fitness.service.dto.admin.CoachStudentsResponse;
import com.gym.fitness.service.dto.admin.StudentRequest;
import com.gym.fitness.service.dto.admin.StudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String ROLE_COACH = "COACH";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final Set<String> FITNESS_GOALS = Set.of("WEIGHT_LOSS", "FAT_LOSS", "MUSCLE_GAIN");

    private final UserMapper userMapper;
    private final BodyMetricMapper bodyMetricMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<CoachResponse> getAllCoaches() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.in("user_role", ROLE_COACH, ROLE_COACH.toLowerCase());
        List<User> coaches = userMapper.selectList(wrapper);

        return coaches.stream().map(this::toCoachResponse).collect(Collectors.toList());
    }

    public void addCoach(CoachRequest request) {
        validateRequiredPassword(request.getPassword());
        ensureUsernameAvailable(request.getUsername(), null);
        ensureEmailAvailable(request.getEmail(), null);

        User coach = new User();
        coach.setUsername(request.getUsername());
        coach.setPassword(passwordEncoder.encode(request.getPassword()));
        coach.setRealName(request.getRealName());
        coach.setEmail(request.getEmail());
        coach.setPhone(request.getPhone());
        coach.setAge(request.getAge());
        coach.setGender(normalizeGender(request.getGender()));
        coach.setRole(ROLE_COACH);
        coach.setShowInLeaderboard(false);
        coach.setAllowCoachView(true);
        coach.setCreatedAt(LocalDateTime.now());
        coach.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(coach);
    }

    public void updateCoach(Long coachId, CoachRequest request) {
        User coach = getRequiredUserByRole(coachId, ROLE_COACH, "教练不存在");

        ensureUsernameAvailable(request.getUsername(), coachId);
        ensureEmailAvailable(request.getEmail(), coachId);

        coach.setUsername(request.getUsername());
        if (hasText(request.getPassword())) {
            coach.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        coach.setRealName(request.getRealName());
        coach.setEmail(request.getEmail());
        coach.setPhone(request.getPhone());
        coach.setAge(request.getAge());
        coach.setGender(normalizeGender(request.getGender()));
        coach.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(coach);
    }

    public void deleteCoach(Long coachId) {
        User coach = getRequiredUserByRole(coachId, ROLE_COACH, "教练不存在");

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .in("user_role", ROLE_STUDENT, ROLE_STUDENT.toLowerCase());
        int studentCount = userMapper.selectCount(wrapper).intValue();
        if (studentCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "该教练仍有" + studentCount + "名学员，无法删除");
        }

        userMapper.deleteById(coach.getId());
    }

    public List<StudentResponse> getAllStudents() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.in("user_role", ROLE_STUDENT, ROLE_STUDENT.toLowerCase());
        List<User> students = userMapper.selectList(wrapper);

        return students.stream().map(student -> {
            StudentResponse response = new StudentResponse();
            response.setId(student.getId());
            response.setUsername(student.getUsername());
            response.setRealName(student.getRealName());
            response.setEmail(student.getEmail());
            response.setPhone(student.getPhone());
            response.setAge(student.getAge());
            response.setGender(student.getGender());
            response.setFitnessGoal(student.getFitnessGoal());
            response.setCoachId(student.getCoachId());
            response.setCreatedAt(student.getCreatedAt());

            if (student.getCoachId() != null) {
                User coach = userMapper.selectById(student.getCoachId());
                if (coach != null) {
                    response.setCoachName(coach.getRealName());
                }
            }

            return response;
        }).collect(Collectors.toList());
    }

    public void addStudent(StudentRequest request) {
        validateRequiredPassword(request.getPassword());
        ensureUsernameAvailable(request.getUsername(), null);
        ensureEmailAvailable(request.getEmail(), null);

        if (request.getCoachId() != null) {
            getRequiredUserByRole(request.getCoachId(), ROLE_COACH, "指定的教练不存在");
        }

        User student = new User();
        student.setUsername(request.getUsername());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setRealName(request.getRealName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setAge(request.getAge());
        student.setGender(normalizeGender(request.getGender()));
        student.setFitnessGoal(normalizeFitnessGoal(request.getFitnessGoal()));
        student.setCoachId(request.getCoachId());
        student.setRole(ROLE_STUDENT);
        student.setShowInLeaderboard(true);
        student.setAllowCoachView(true);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(student);
    }

    public void updateStudent(Long studentId, StudentRequest request) {
        User student = getRequiredUserByRole(studentId, ROLE_STUDENT, "学员不存在");

        ensureUsernameAvailable(request.getUsername(), studentId);
        ensureEmailAvailable(request.getEmail(), studentId);

        if (request.getCoachId() != null) {
            getRequiredUserByRole(request.getCoachId(), ROLE_COACH, "指定的教练不存在");
        }

        student.setUsername(request.getUsername());
        if (hasText(request.getPassword())) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        student.setRealName(request.getRealName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setAge(request.getAge());
        student.setGender(normalizeGender(request.getGender()));
        student.setFitnessGoal(normalizeFitnessGoal(request.getFitnessGoal()));
        student.setCoachId(request.getCoachId());
        student.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(student);
    }

    public void deleteStudent(Long studentId) {
        User student = getRequiredUserByRole(studentId, ROLE_STUDENT, "学员不存在");
        userMapper.deleteById(student.getId());
    }

    public void assignCoach(Long studentId, Long coachId) {
        User student = userMapper.selectById(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        if (isRole(student, ROLE_COACH) || isRole(student, ROLE_ADMIN)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标用户不是学员，无法分配教练");
        }

        getRequiredUserByRole(coachId, ROLE_COACH, "教练不存在");

        student.setCoachId(coachId);
        student.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(student);
    }

    public List<CoachStudentsResponse> getCoachStudents(Long coachId) {
        getRequiredUserByRole(coachId, ROLE_COACH, "教练不存在");

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId)
                .in("user_role", ROLE_STUDENT, ROLE_STUDENT.toLowerCase());
        List<User> students = userMapper.selectList(wrapper);

        return students.stream().map(student -> {
            CoachStudentsResponse response = new CoachStudentsResponse();
            response.setId(student.getId());
            response.setUsername(student.getUsername());
            response.setRealName(student.getRealName());
            response.setEmail(student.getEmail());
            response.setAge(student.getAge());
            response.setGender(student.getGender());
            response.setFitnessGoal(student.getFitnessGoal());

            BodyMetric latestBodyMetric = bodyMetricMapper.selectLatestByUserId(student.getId());
            response.setCurrentWeight(latestBodyMetric != null ? latestBodyMetric.getWeightKg() : null);

            TrainingPlan activePlan = trainingPlanMapper.selectActiveByStudentId(student.getId());
            if (activePlan != null) {
                response.setTrainingStatus(activePlan.getStatus());
                response.setPlanProgress(activePlan.getCompletionRate());
            } else {
                response.setTrainingStatus("INACTIVE");
                response.setPlanProgress(0.0);
            }

            QueryWrapper<ExerciseRecord> recordWrapper = new QueryWrapper<>();
            recordWrapper.eq("user_id", student.getId())
                    .orderByDesc("exercise_date")
                    .last("LIMIT 1");
            ExerciseRecord latestRecord = exerciseRecordMapper.selectOne(recordWrapper);
            if (latestRecord != null) {
                response.setLastExerciseTime(
                        latestRecord.getCreatedAt() != null
                                ? latestRecord.getCreatedAt()
                                : latestRecord.getExerciseDate() != null
                                ? latestRecord.getExerciseDate().atStartOfDay()
                                : null
                );
            }

            return response;
        }).collect(Collectors.toList());
    }

    private CoachResponse toCoachResponse(User coach) {
        CoachResponse response = new CoachResponse();
        response.setId(coach.getId());
        response.setUsername(coach.getUsername());
        response.setRealName(coach.getRealName());
        response.setEmail(coach.getEmail());
        response.setPhone(coach.getPhone());
        response.setAge(coach.getAge());
        response.setGender(coach.getGender());
        response.setCreatedAt(coach.getCreatedAt());

        QueryWrapper<User> studentWrapper = new QueryWrapper<>();
        studentWrapper.eq("coach_id", coach.getId())
                .in("user_role", ROLE_STUDENT, ROLE_STUDENT.toLowerCase());
        response.setStudentCount(userMapper.selectCount(studentWrapper).intValue());
        return response;
    }

    private User getRequiredUserByRole(Long userId, String expectedRole, String message) {
        User user = userMapper.selectById(userId);
        if (user == null || !isRole(user, expectedRole)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, message);
        }
        return user;
    }

    private boolean isRole(User user, String expectedRole) {
        return user.getRole() != null && expectedRole.equalsIgnoreCase(user.getRole());
    }

    private void ensureUsernameAvailable(String username, Long excludeUserId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        if (excludeUserId != null) {
            wrapper.ne("id", excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
    }

    private void ensureEmailAvailable(String email, Long excludeUserId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);
        if (excludeUserId != null) {
            wrapper.ne("id", excludeUserId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已存在");
        }
    }

    private void validateRequiredPassword(String password) {
        if (!hasText(password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String normalizeGender(String gender) {
        if (!hasText(gender)) {
            return null;
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
        if (!hasText(fitnessGoal)) {
            return null;
        }

        String normalized = fitnessGoal.trim().toUpperCase();
        if ("BODY_SHAPING".equals(normalized)) {
            return "FAT_LOSS";
        }
        if ("HEALTH".equals(normalized)) {
            return "WEIGHT_LOSS";
        }
        if (FITNESS_GOALS.contains(normalized)) {
            return normalized;
        }

        throw new BusinessException(ErrorCode.PARAM_ERROR, "健身目标仅支持 WEIGHT_LOSS/FAT_LOSS/MUSCLE_GAIN");
    }
}
