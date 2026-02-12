package com.gym.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {
    
    @Select("SELECT * FROM exercise_records WHERE user_id = #{userId} ORDER BY exercise_date DESC")
    List<ExerciseRecord> selectByUserId(@Param("userId") Long userId);
    
    @Select("SELECT * FROM exercise_records WHERE user_id = #{userId} " +
            "AND exercise_date BETWEEN #{startDate} AND #{endDate} ORDER BY exercise_date DESC")
    List<ExerciseRecord> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM exercise_records")
    Long sumDurationMinutes();

    @Select("SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_records")
    Double sumCaloriesBurned();

    @Select("SELECT COUNT(DISTINCT user_id) FROM exercise_records WHERE exercise_date >= #{startDate}")
    Integer countDistinctUsersSince(@Param("startDate") LocalDate startDate);

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT user_id) FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "</script>"
    })
    Integer countDistinctUsersInRange(@Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT COALESCE(AVG(duration_minutes), 0) FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "</script>"
    })
    Double avgDurationInRange(@Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT exercise_type AS type, COUNT(*) AS cnt FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY exercise_type",
            "</script>"
    })
    List<Map<String, Object>> countByExerciseType(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Select("SELECT equipment_used AS equipment, COUNT(*) AS cnt FROM exercise_records WHERE equipment_used IS NOT NULL AND equipment_used <> '' GROUP BY equipment_used")
    List<Map<String, Object>> countByEquipmentUsed();
}
