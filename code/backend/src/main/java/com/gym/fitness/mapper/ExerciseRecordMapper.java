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
            "SELECT COALESCE(SUM(duration_minutes), 0) FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "</script>"
    })
    Long sumDurationInRange(@Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "</script>"
    })
    Double sumCaloriesInRange(@Param("startDate") LocalDate startDate,
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

    @Select({
            "<script>",
            "SELECT",
            "  exercise_date AS activityDate,",
            "  COUNT(DISTINCT user_id) AS activeUserCount,",
            "  COALESCE(AVG(duration_minutes), 0) AS averageDurationMinutes,",
            "  COALESCE(SUM(duration_minutes), 0) AS totalDurationMinutes,",
            "  COALESCE(SUM(calories_burned), 0) AS totalCaloriesBurned",
            "FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY exercise_date",
            "ORDER BY exercise_date ASC",
            "</script>"
    })
    List<Map<String, Object>> summarizeDailyActivity(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT user_id AS userId, COALESCE(SUM(duration_minutes), 0) AS totalValue",
            "FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY user_id",
            "HAVING totalValue &gt; 0",
            "ORDER BY totalValue DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> sumDurationByUserInRange(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("limit") int limit);

    @Select({
            "<script>",
            "SELECT user_id AS userId, COALESCE(SUM(calories_burned), 0) AS totalValue",
            "FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY user_id",
            "HAVING totalValue &gt; 0",
            "ORDER BY totalValue DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> sumCaloriesByUserInRange(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate,
                                                       @Param("limit") int limit);

    @Select("SELECT equipment_used AS equipment, COUNT(*) AS cnt FROM exercise_records WHERE equipment_used IS NOT NULL AND equipment_used <> '' GROUP BY equipment_used")
    List<Map<String, Object>> countByEquipmentUsed();

    @Select({
            "<script>",
            "SELECT equipment_used AS equipment, COUNT(*) AS cnt",
            "FROM exercise_records",
            "<where>",
            "  equipment_used IS NOT NULL",
            "  AND equipment_used &lt;&gt; ''",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY equipment_used",
            "ORDER BY cnt DESC",
            "</script>"
    })
    List<Map<String, Object>> countByEquipmentUsedInRange(@Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT",
            "  CASE",
            "    WHEN created_at IS NULL THEN 18",
            "    WHEN HOUR(created_at) &lt; 6 OR HOUR(created_at) &gt; 22 THEN 18",
            "    ELSE HOUR(created_at)",
            "  END AS activityHour,",
            "  COUNT(DISTINCT user_id) AS userCount,",
            "  COALESCE(SUM(duration_minutes), 0) AS totalDuration",
            "FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY activityHour",
            "ORDER BY activityHour ASC",
            "</script>"
    })
    List<Map<String, Object>> summarizeHourlyActivity(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select({
            "<script>",
            "SELECT",
            "  exercise_date AS activityDate,",
            "  CASE",
            "    WHEN created_at IS NULL THEN 18",
            "    WHEN HOUR(created_at) &lt; 6 OR HOUR(created_at) &gt; 22 THEN 18",
            "    ELSE HOUR(created_at)",
            "  END AS activityHour,",
            "  COUNT(DISTINCT user_id) AS userCount",
            "FROM exercise_records",
            "<where>",
            "  <if test='startDate != null'> AND exercise_date &gt;= #{startDate} </if>",
            "  <if test='endDate != null'> AND exercise_date &lt;= #{endDate} </if>",
            "</where>",
            "GROUP BY exercise_date, activityHour",
            "ORDER BY exercise_date ASC, activityHour ASC",
            "</script>"
    })
    List<Map<String, Object>> summarizeHeatmapActivity(@Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);
}
