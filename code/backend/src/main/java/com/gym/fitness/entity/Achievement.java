package com.gym.fitness.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("achievements")
public class Achievement {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("achievement_name")
    private String achievementName;
    
    private String description;
    
    @TableField("achievement_type")
    private String achievementType;
    
    @TableField("threshold_value")
    private Double thresholdValue;
    
    @TableField("icon_url")
    private String iconUrl;
}
