package com.gym.fitness.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.domain.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {
    
    @Select("SELECT * FROM exercise_records WHERE user_id = #{userId} ORDER BY exercise_date DESC")
    List<ExerciseRecord> selectByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM exercise_records WHERE user_id = #{userId} " +
            "AND exercise_date BETWEEN #{startDate} AND #{endDate} ORDER BY exercise_date DESC")
    List<ExerciseRecord> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
}
