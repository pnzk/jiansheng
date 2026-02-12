package com.gym.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.entity.BodyMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BodyMetricMapper extends BaseMapper<BodyMetric> {
    
    @Select("SELECT * FROM body_metrics WHERE user_id = #{userId} ORDER BY measurement_date DESC")
    List<BodyMetric> selectByUserIdOrderByDate(@Param("userId") Long userId);
    
    @Select("SELECT * FROM body_metrics WHERE user_id = #{userId} ORDER BY measurement_date DESC LIMIT 1")
    BodyMetric selectLatestByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM body_metrics WHERE user_id = #{userId} " +
            "AND measurement_date BETWEEN #{startDate} AND #{endDate} ORDER BY measurement_date DESC")
    List<BodyMetric> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
