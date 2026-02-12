package com.gym.fitness.controller;

import com.gym.fitness.common.result.Result;
import com.gym.fitness.service.AdminService;
import com.gym.fitness.service.dto.admin.AssignCoachRequest;
import com.gym.fitness.service.dto.admin.CoachRequest;
import com.gym.fitness.service.dto.admin.CoachResponse;
import com.gym.fitness.service.dto.admin.StudentRequest;
import com.gym.fitness.service.dto.admin.StudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/coaches")
    public Result<List<CoachResponse>> getAllCoaches() {
        return Result.success(adminService.getAllCoaches());
    }

    @PostMapping("/coaches")
    public Result<Void> addCoach(@Valid @RequestBody CoachRequest request) {
        adminService.addCoach(request);
        return Result.success("教练添加成功", null);
    }

    @PutMapping("/coaches/{coachId}")
    public Result<Void> updateCoach(@PathVariable Long coachId,
                                    @Valid @RequestBody CoachRequest request) {
        adminService.updateCoach(coachId, request);
        return Result.success("教练更新成功", null);
    }

    @DeleteMapping("/coaches/{coachId}")
    public Result<Void> deleteCoach(@PathVariable Long coachId) {
        adminService.deleteCoach(coachId);
        return Result.success("教练删除成功", null);
    }

    @GetMapping("/students")
    public Result<List<StudentResponse>> getAllStudents() {
        return Result.success(adminService.getAllStudents());
    }

    @PostMapping("/students")
    public Result<Void> addStudent(@Valid @RequestBody StudentRequest request) {
        adminService.addStudent(request);
        return Result.success("学员添加成功", null);
    }

    @PutMapping("/students/{studentId}")
    public Result<Void> updateStudent(@PathVariable Long studentId,
                                      @Valid @RequestBody StudentRequest request) {
        adminService.updateStudent(studentId, request);
        return Result.success("学员更新成功", null);
    }

    @DeleteMapping("/students/{studentId}")
    public Result<Void> deleteStudent(@PathVariable Long studentId) {
        adminService.deleteStudent(studentId);
        return Result.success("学员删除成功", null);
    }

    @PutMapping("/students/{studentId}/assign-coach")
    public Result<Void> assignCoach(@PathVariable Long studentId,
                                    @Valid @RequestBody AssignCoachRequest request) {
        adminService.assignCoach(studentId, request.getCoachId());
        return Result.success("教练分配成功", null);
    }
}

