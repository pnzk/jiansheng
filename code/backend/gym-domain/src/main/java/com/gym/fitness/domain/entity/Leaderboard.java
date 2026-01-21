package com.gym.fitness.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("leaderboards")
public class Leaderboard {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("leaderboard_type")
    private String leaderboardType;
    
    @TableField("user_id")
    private Long userId;
    
    @TableField("rank")
    private Integer rank;
    
    @TableField("value")
    private Double value;
    
    @TableField("period_start")
    private LocalDate periodStart;
    
    @TableField("period_end")
    private LocalDate periodEnd;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
