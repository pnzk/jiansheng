package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.service.AuthService;
import com.gym.fitness.service.dto.auth.LoginRequest;
import com.gym.fitness.service.dto.auth.LoginResponse;
import com.gym.fitness.service.dto.auth.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功", null);
    }
}
