package com.gym.fitness.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.domain.entity.Achievement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AchievementMapper extends BaseMapper<Achievement> {
}
