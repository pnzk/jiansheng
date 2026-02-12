package com.gym.fitness.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exercise_reference")
public class ExerciseReference {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("exercise_name_en")
    private String exerciseNameEn;
    
    @TableField("exercise_type")
    private String exerciseType;
    
    @TableField("body_part")
    private String bodyPart;
    
    @TableField("equipment")
    private String equipment;
    
    @TableField("level")
    private String level;
    
    @TableField("description")
    private String description;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
}
