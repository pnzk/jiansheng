package com.gym.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("training_plans")
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("student_id")
    private Long studentId;
    
    @TableField("coach_id")
    private Long coachId;
    
    @TableField("plan_name")
    private String planName;
    
    @TableField("goal_type")
    private String goalType;
    
    @TableField("target_value")
    private Double targetValue;
    
    @TableField("start_date")
    private LocalDate startDate;
    
    @TableField("end_date")
    private LocalDate endDate;
    
    @TableField("status")
    private String status;
    
    @TableField("completion_rate")
    private Double completionRate;
    
    @TableField("weekly_schedule")
    private String weeklySchedule;
    
    private String description;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
