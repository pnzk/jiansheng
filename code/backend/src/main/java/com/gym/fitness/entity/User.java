package com.gym.fitness.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    private String password;
    private String email;
    private String phone;
    
    @TableField("real_name")
    private String realName;
    
    private Integer age;
    private String gender;
    
    @TableField("user_role")
    private String role;
    
    @TableField("created_at")
    private LocalDateTime createdAt;
    
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    
    @TableField("coach_id")
    private Long coachId;
    
    @TableField("fitness_goal")
    private String fitnessGoal;
    
    @TableField("show_in_leaderboard")
    private Boolean showInLeaderboard;
    
    @TableField("allow_coach_view")
    private Boolean allowCoachView;
}
