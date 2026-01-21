package com.gym.fitness.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.domain.entity.Leaderboard;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LeaderboardMapper extends BaseMapper<Leaderboard> {
}
