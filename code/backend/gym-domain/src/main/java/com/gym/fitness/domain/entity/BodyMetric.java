package com.gym.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("body_metrics")
public class BodyMetric {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("measurement_date")
    private LocalDate measurementDate;
    
    @TableField("weight_kg")
    private Double weightKg;
    
    @TableField("body_fat_percentage")
    private Double bodyFatPercentage;
    
    @TableField("height_cm")
    private Double heightCm;
    
    @TableField("bmi")
    private Double bmi;
    
    @TableField("muscle_mass_kg")
    private Double muscleMassKg;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
