package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.common.util.JwtUtil;
import com.gym.fitness.service.AdminService;
import com.gym.fitness.service.UserService;
import com.gym.fitness.service.dto.admin.CoachStudentsResponse;
import com.gym.fitness.service.dto.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    @GetMapping("/profile")
    public Result<UserProfileResponse> getUserProfile(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        UserProfileResponse response = userService.getUserProfile(userId);
        return Result.success(response);
    }

    @PutMapping("/profile")
    public Result<Void> updateUserProfile(HttpServletRequest request, 
                                          @Valid @RequestBody UpdateProfileRequest updateRequest) {
        Long userId = getUserIdFromToken(request);
        userService.updateUserProfile(userId, updateRequest);
        return Result.success("更新成功", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(HttpServletRequest request,
                                       @Valid @RequestBody ChangePasswordRequest changeRequest) {
        Long userId = getUserIdFromToken(request);
        userService.changePassword(userId, changeRequest);
        return Result.success("密码修改成功", null);
    }

    @PutMapping("/privacy")
    public Result<Void> updatePrivacySettings(HttpServletRequest request,
                                              @Valid @RequestBody PrivacySettingsRequest privacyRequest) {
        Long userId = getUserIdFromToken(request);
        userService.updatePrivacySettings(userId, privacyRequest);
        return Result.success("隐私设置更新成功", null);
    }

    @PostMapping("/coach/todos/handle")
    public Result<Void> handleCoachTodo(HttpServletRequest request,
                                        @Valid @RequestBody CoachTodoHandleRequest todoRequest) {
        Long coachId = getUserIdFromToken(request);
        userService.handleCoachTodo(coachId, todoRequest);
        return Result.success("待办事项处理成功", null);
    }

    @GetMapping("/coach/students")
    public Result<List<CoachStudentsResponse>> getCoachStudents(HttpServletRequest request) {
        Long coachId = getUserIdFromToken(request);
        List<CoachStudentsResponse> students = adminService.getCoachStudents(coachId);
        return Result.success(students);
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}
