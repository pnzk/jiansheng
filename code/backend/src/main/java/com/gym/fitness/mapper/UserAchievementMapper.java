package com.gym.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.entity.UserAchievement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserAchievementMapper extends BaseMapper<UserAchievement> {
    
    @Select("SELECT * FROM user_achievements WHERE user_id = #{userId}")
    List<UserAchievement> selectByUserId(@Param("userId") Long userId);
}
