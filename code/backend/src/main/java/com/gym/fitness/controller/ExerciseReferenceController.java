package com.gym.fitness.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.fitness.common.result.Result;
import com.gym.fitness.entity.ExerciseReference;
import com.gym.fitness.service.ExerciseReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise-reference")
@RequiredArgsConstructor
public class ExerciseReferenceController {
    
    private final ExerciseReferenceService exerciseReferenceService;
    
    @GetMapping("/list")
    public Result<Page<ExerciseReference>> getExerciseList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String exerciseType,
            @RequestParam(required = false) String bodyPart,
            @RequestParam(required = false) String level) {
        Page<ExerciseReference> result = exerciseReferenceService.getExercisesPage(page, size, exerciseType, bodyPart, level);
        return Result.success(result);
    }
    
    @GetMapping("/all")
    public Result<List<ExerciseReference>> getAllExercises() {
        return Result.success(exerciseReferenceService.getAllExercises());
    }
    
    @GetMapping("/{id}")
    public Result<ExerciseReference> getById(@PathVariable Long id) {
        return Result.success(exerciseReferenceService.getById(id));
    }
    
    @GetMapping("/search")
    public Result<List<ExerciseReference>> search(@RequestParam String keyword) {
        return Result.success(exerciseReferenceService.searchByName(keyword));
    }
    
    @GetMapping("/types")
    public Result<List<String>> getExerciseTypes() {
        return Result.success(exerciseReferenceService.getAllExerciseTypes());
    }
    
    @GetMapping("/body-parts")
    public Result<List<String>> getBodyParts() {
        return Result.success(exerciseReferenceService.getAllBodyParts());
    }
    
    @GetMapping("/levels")
    public Result<List<String>> getLevels() {
        return Result.success(exerciseReferenceService.getAllLevels());
    }
    
    @GetMapping("/by-body-part/{bodyPart}")
    public Result<List<ExerciseReference>> getByBodyPart(@PathVariable String bodyPart) {
        return Result.success(exerciseReferenceService.getByBodyPart(bodyPart));
    }
}
