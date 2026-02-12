package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gym.fitness.common.exception.BusinessException;
import com.gym.fitness.common.result.ErrorCode;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.entity.User;
import com.gym.fitness.mapper.UserMapper;
import com.gym.fitness.service.dto.auth.LoginRequest;
import com.gym.fitness.service.dto.auth.LoginResponse;
import com.gym.fitness.service.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String loginId = safeText(request.getUsername());
        String rawPassword = safeText(request.getPassword());

        // 支持用户名 / 邮箱 / 手机号登录
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.and(condition -> condition
                .eq("username", loginId)
                .or()
                .eq("email", loginId)
                .or()
                .eq("phone", loginId));
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        // 验证密码
        if (!matchesPassword(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        // 兼容历史明文密码，并在首次成功登录时升级为 BCrypt
        upgradeLegacyPasswordIfNeeded(user, rawPassword);

        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        return new LoginResponse(token, user.getId(), user.getUsername(),
                                user.getRole(), user.getRealName());
    }

    /**
     * 用户注册
     */
    @Transactional
    public void register(RegisterRequest request) {
        String username = safeText(request.getUsername());
        String email = safeText(request.getEmail()).toLowerCase();
        String phone = safeText(request.getPhone());
        String role = safeText(request.getRole()).toUpperCase();

        if (!"STUDENT".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前仅支持学员自助注册");
        }

        // 检查用户名是否已存在
        QueryWrapper<User> usernameQuery = new QueryWrapper<>();
        usernameQuery.eq("username", username);
        if (userMapper.selectCount(usernameQuery) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        QueryWrapper<User> emailQuery = new QueryWrapper<>();
        emailQuery.eq("email", email);
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被注册");
        }

        QueryWrapper<User> phoneQuery = new QueryWrapper<>();
        phoneQuery.eq("phone", phone);
        if (userMapper.selectCount(phoneQuery) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已被注册");
        }

        String fitnessGoal = normalizeFitnessGoal(request.getFitnessGoal());

        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(safeText(request.getPassword())));
        user.setEmail(email);
        user.setPhone(phone);
        user.setRealName(safeText(request.getRealName()));
        user.setAge(request.getAge());
        user.setGender(safeText(request.getGender()));
        user.setRole(role);
        user.setFitnessGoal(fitnessGoal);
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

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBcryptHash(String encodedPassword) {
        return encodedPassword != null
                && encodedPassword.length() >= 60
                && (encodedPassword.startsWith("$2a$")
                || encodedPassword.startsWith("$2b$")
                || encodedPassword.startsWith("$2y$"));
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return rawPassword.equals(storedPassword);
    }

    private void upgradeLegacyPasswordIfNeeded(User user, String rawPassword) {
        if (isBcryptHash(user.getPassword())) {
            return;
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(rawPassword))
                .set(User::getUpdatedAt, LocalDateTime.now());
        userMapper.update(null, updateWrapper);
    }

    private String normalizeFitnessGoal(String input) {
        String goal = safeText(input).toUpperCase();
        if (goal.isBlank()) {
            return "WEIGHT_LOSS";
        }

        if (!"WEIGHT_LOSS".equals(goal)
                && !"FAT_LOSS".equals(goal)
                && !"MUSCLE_GAIN".equals(goal)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "健身目标不合法");
        }

        return goal;
    }
}
