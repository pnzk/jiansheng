package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.domain.entity.User;
import com.gym.fitness.repository.mapper.UserMapper;
import com.gym.fitness.service.dto.auth.LoginRequest;
import com.gym.fitness.service.dto.auth.LoginResponse;
import com.gym.fitness.service.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        return new LoginResponse(token, user.getId(), user.getUsername(), 
                                user.getRole(), user.getRealName());
    }

    /**
     * 用户注册
     */
    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setFitnessGoal(request.getFitnessGoal());
        user.setShowInLeaderboard(true);
        user.setAllowCoachView(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}
