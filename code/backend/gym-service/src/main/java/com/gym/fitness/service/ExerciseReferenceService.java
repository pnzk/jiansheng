package com.gym.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gym.fitness.domain.entity.ExerciseReference;
import com.gym.fitness.repository.mapper.ExerciseReferenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseReferenceService {
    
    private final ExerciseReferenceMapper exerciseReferenceMapper;
    
    public List<ExerciseReference> getAllExercises() {
        return exerciseReferenceMapper.selectList(null);
    }
    
    public Page<ExerciseReference> getExercisesPage(int page, int size, String exerciseType, String bodyPart, String level) {
        Page<ExerciseReference> pageRequest = new Page<>(page, size);
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        
        if (exerciseType != null && !exerciseType.isEmpty()) {
            wrapper.eq("exercise_type", exerciseType);
        }
        if (bodyPart != null && !bodyPart.isEmpty()) {
            wrapper.eq("body_part", bodyPart);
        }
        if (level != null && !level.isEmpty()) {
            wrapper.eq("level", level);
        }
        
        return exerciseReferenceMapper.selectPage(pageRequest, wrapper);
    }
    
    public ExerciseReference getById(Long id) {
        return exerciseReferenceMapper.selectById(id);
    }
    
    public List<ExerciseReference> searchByName(String keyword) {
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        wrapper.like("exercise_name_en", keyword);
        return exerciseReferenceMapper.selectList(wrapper);
    }
    
    public List<String> getAllExerciseTypes() {
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT exercise_type");
        return exerciseReferenceMapper.selectList(wrapper)
                .stream()
                .map(ExerciseReference::getExerciseType)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<String> getAllBodyParts() {
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT body_part");
        return exerciseReferenceMapper.selectList(wrapper)
                .stream()
                .map(ExerciseReference::getBodyPart)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<String> getAllLevels() {
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT level");
        return exerciseReferenceMapper.selectList(wrapper)
                .stream()
                .map(ExerciseReference::getLevel)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<ExerciseReference> getByBodyPart(String bodyPart) {
        QueryWrapper<ExerciseReference> wrapper = new QueryWrapper<>();
        wrapper.eq("body_part", bodyPart);
        return exerciseReferenceMapper.selectList(wrapper);
    }
}
