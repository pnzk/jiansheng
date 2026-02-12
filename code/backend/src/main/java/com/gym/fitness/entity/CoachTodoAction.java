package com.gym.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("coach_todo_actions")
public class CoachTodoAction {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("coach_id")
    private Long coachId;

    @TableField("student_id")
    private Long studentId;

    @TableField("todo_key")
    private String todoKey;

    @TableField("todo_title")
    private String todoTitle;

    @TableField("todo_description")
    private String todoDescription;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
