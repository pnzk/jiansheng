package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.domain.entity.User;
import com.gym.fitness.repository.mapper.UserMapper;
import com.gym.fitness.service.dto.admin.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 获取所有教练
    public List<CoachResponse> getAllCoaches() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_role", "coach");
        List<User> coaches = userMapper.selectList(wrapper);
        
        return coaches.stream().map(coach -> {
            CoachResponse response = new CoachResponse();
            response.setId(coach.getId());
            response.setUsername(coach.getUsername());
            response.setRealName(coach.getRealName());
            response.setEmail(coach.getEmail());
            response.setPhone(coach.getPhone());
            response.setAge(coach.getAge());
            response.setGender(coach.getGender());
            response.setCreatedAt(coach.getCreatedAt());
            
            // 统计学员数量
            QueryWrapper<User> studentWrapper = new QueryWrapper<>();
            studentWrapper.eq("coach_id", coach.getId());
            int studentCount = userMapper.selectCount(studentWrapper).intValue();
            response.setStudentCount(studentCount);
            
            return response;
        }).collect(Collectors.toList());
    }

    // 添加教练
    public void addCoach(CoachRequest request) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        
        // 检查邮箱是否已存在
        wrapper = new QueryWrapper<>();
        wrapper.eq("email", request.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已存在");
        }
        
        User coach = new User();
        coach.setUsername(request.getUsername());
        coach.setPassword(passwordEncoder.encode(request.getPassword()));
        coach.setRealName(request.getRealName());
        coach.setEmail(request.getEmail());
        coach.setPhone(request.getPhone());
        coach.setAge(request.getAge());
        coach.setGender(request.getGender());
        coach.setRole("coach");
        coach.setCreatedAt(LocalDateTime.now());
        coach.setUpdatedAt(LocalDateTime.now());
        coach.setShowInLeaderboard(false);
        coach.setAllowCoachView(true);
        
        userMapper.insert(coach);
    }

    // 更新教练
    public void updateCoach(Long coachId, CoachRequest request) {
        User coach = userMapper.selectById(coachId);
        if (coach == null || !"coach".equals(coach.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "教练不存在");
        }
        
        // 检查用户名是否被其他用户使用
        if (!coach.getUsername().equals(request.getUsername())) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("username", request.getUsername());
            wrapper.ne("id", coachId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被使用");
            }
        }
        
        // 检查邮箱是否被其他用户使用
        if (!coach.getEmail().equals(request.getEmail())) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("email", request.getEmail());
            wrapper.ne("id", coachId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
            }
        }
        
        coach.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            coach.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        coach.setRealName(request.getRealName());
        coach.setEmail(request.getEmail());
        coach.setPhone(request.getPhone());
        coach.setAge(request.getAge());
        coach.setGender(request.getGender());
        coach.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(coach);
    }

    // 删除教练
    public void deleteCoach(Long coachId) {
        User coach = userMapper.selectById(coachId);
        if (coach == null || !"coach".equals(coach.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "教练不存在");
        }
        
        // 检查是否有学员
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId);
        int studentCount = userMapper.selectCount(wrapper).intValue();
        if (studentCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_FAILED, "该教练还有" + studentCount + "个学员，无法删除");
        }
        
        userMapper.deleteById(coachId);
    }

    // 获取所有学员
    public List<StudentResponse> getAllStudents() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("user_role", "student");
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
            
            // 获取教练姓名
            if (student.getCoachId() != null) {
                User coach = userMapper.selectById(student.getCoachId());
                if (coach != null) {
                    response.setCoachName(coach.getRealName());
                }
            }
            
            return response;
        }).collect(Collectors.toList());
    }

    // 添加学员
    public void addStudent(StudentRequest request) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        
        // 检查邮箱是否已存在
        wrapper = new QueryWrapper<>();
        wrapper.eq("email", request.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已存在");
        }
        
        // 如果指定了教练，检查教练是否存在
        if (request.getCoachId() != null) {
            User coach = userMapper.selectById(request.getCoachId());
            if (coach == null || !"coach".equals(coach.getRole())) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "指定的教练不存在");
            }
        }
        
        User student = new User();
        student.setUsername(request.getUsername());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setRealName(request.getRealName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setAge(request.getAge());
        student.setGender(request.getGender());
        student.setFitnessGoal(request.getFitnessGoal());
        student.setCoachId(request.getCoachId());
        student.setRole("student");
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        student.setShowInLeaderboard(true);
        student.setAllowCoachView(true);
        
        userMapper.insert(student);
    }

    // 更新学员
    public void updateStudent(Long studentId, StudentRequest request) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        
        // 检查用户名是否被其他用户使用
        if (!student.getUsername().equals(request.getUsername())) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("username", request.getUsername());
            wrapper.ne("id", studentId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被使用");
            }
        }
        
        // 检查邮箱是否被其他用户使用
        if (!student.getEmail().equals(request.getEmail())) {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("email", request.getEmail());
            wrapper.ne("id", studentId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已被使用");
            }
        }
        
        // 如果指定了教练，检查教练是否存在
        if (request.getCoachId() != null) {
            User coach = userMapper.selectById(request.getCoachId());
            if (coach == null || !"coach".equals(coach.getRole())) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND, "指定的教练不存在");
            }
        }
        
        student.setUsername(request.getUsername());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        student.setRealName(request.getRealName());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setAge(request.getAge());
        student.setGender(request.getGender());
        student.setFitnessGoal(request.getFitnessGoal());
        student.setCoachId(request.getCoachId());
        student.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(student);
    }

    // 删除学员
    public void deleteStudent(Long studentId) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        
        userMapper.deleteById(studentId);
    }

    // 分配教练
    public void assignCoach(Long studentId, Long coachId) {
        User student = userMapper.selectById(studentId);
        if (student == null || !"student".equals(student.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "学员不存在");
        }
        
        User coach = userMapper.selectById(coachId);
        if (coach == null || !"coach".equals(coach.getRole())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "教练不存在");
        }
        
        student.setCoachId(coachId);
        student.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(student);
    }

    // 获取教练的学员列表
    public List<CoachStudentsResponse> getCoachStudents(Long coachId) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("coach_id", coachId);
        wrapper.eq("user_role", "student");
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
            // TODO: 从exercise_records表获取最后运动时间
            // TODO: 从training_plans表获取训练状态
            response.setTrainingStatus("进行中");
            return response;
        }).collect(Collectors.toList());
    }
}
