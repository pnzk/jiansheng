package com.gym.fitness.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.domain.entity.TrainingPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingPlanMapper extends BaseMapper<TrainingPlan> {
    
    @Select("SELECT * FROM training_plans WHERE coach_id = #{coachId} ORDER BY created_at DESC")
    List<TrainingPlan> selectByCoachId(@Param("coachId") Long coachId);
    
    @Select("SELECT * FROM training_plans WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<TrainingPlan> selectByStudentId(@Param("studentId") Long studentId);
    
    @Select("SELECT * FROM training_plans WHERE student_id = #{studentId} AND status = 'ACTIVE' LIMIT 1")
    TrainingPlan selectActiveByStudentId(@Param("studentId") Long studentId);
}
