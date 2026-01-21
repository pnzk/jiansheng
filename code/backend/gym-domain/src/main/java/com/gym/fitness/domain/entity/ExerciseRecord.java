package com.gym.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exercise_records")
public class ExerciseRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("exercise_type")
    private String exerciseType;
    
    @TableField("exercise_date")
    private LocalDate exerciseDate;
    
    @TableField("duration_minutes")
    private Integer durationMinutes;
    
    @TableField("calories_burned")
    private Double caloriesBurned;
    
    @TableField("average_heart_rate")
    private Integer averageHeartRate;
    
    @TableField("max_heart_rate")
    private Integer maxHeartRate;
    
    @TableField("equipment_used")
    private String equipmentUsed;
    
    private String notes;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
