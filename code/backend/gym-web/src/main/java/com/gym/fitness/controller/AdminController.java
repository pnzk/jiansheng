package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.service.AdminService;
import com.gym.fitness.service.dto.admin.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;

    // 教练管理
    @GetMapping("/coaches")
    public Result<List<CoachResponse>> getAllCoaches() {
        List<CoachResponse> coaches = adminService.getAllCoaches();
        return Result.success(coaches);
    }

    @PostMapping("/coaches")
    public Result<Void> addCoach(@Valid @RequestBody CoachRequest request) {
        adminService.addCoach(request);
        return Result.success("教练添加成功", null);
    }

    @PutMapping("/coaches/{id}")
    public Result<Void> updateCoach(@PathVariable Long id, 
                                    @Valid @RequestBody CoachRequest request) {
        adminService.updateCoach(id, request);
        return Result.success("教练更新成功", null);
    }

    @DeleteMapping("/coaches/{id}")
    public Result<Void> deleteCoach(@PathVariable Long id) {
        adminService.deleteCoach(id);
        return Result.success("教练删除成功", null);
    }

    // 学员管理
    @GetMapping("/students")
    public Result<List<StudentResponse>> getAllStudents() {
        List<StudentResponse> students = adminService.getAllStudents();
        return Result.success(students);
    }

    @PostMapping("/students")
    public Result<Void> addStudent(@Valid @RequestBody StudentRequest request) {
        adminService.addStudent(request);
        return Result.success("学员添加成功", null);
    }

    @PutMapping("/students/{id}")
    public Result<Void> updateStudent(@PathVariable Long id, 
                                      @Valid @RequestBody StudentRequest request) {
        adminService.updateStudent(id, request);
        return Result.success("学员更新成功", null);
    }

    @DeleteMapping("/students/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        adminService.deleteStudent(id);
        return Result.success("学员删除成功", null);
    }

    @PutMapping("/students/{id}/assign-coach")
    public Result<Void> assignCoach(@PathVariable Long id, 
                                    @Valid @RequestBody AssignCoachRequest request) {
        adminService.assignCoach(id, request.getCoachId());
        return Result.success("教练分配成功", null);
    }
}
