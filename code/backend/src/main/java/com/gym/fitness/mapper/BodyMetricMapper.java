package com.gym.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.entity.BodyMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @Select({
            "<script>",
            "SELECT",
            "  start_metric.user_id AS userId,",
            "  (start_metric.weight_kg - end_metric.weight_kg) AS totalValue",
            "FROM",
            "  (",
            "    SELECT bm.user_id, bm.weight_kg",
            "    FROM body_metrics bm",
            "    INNER JOIN (",
            "      SELECT user_id, MIN(measurement_date) AS metric_date",
            "      FROM body_metrics",
            "      <where>",
            "        <if test='startDate != null'> AND measurement_date &gt;= #{startDate} </if>",
            "        <if test='endDate != null'> AND measurement_date &lt;= #{endDate} </if>",
            "      </where>",
            "      GROUP BY user_id",
            "    ) first_metric ON first_metric.user_id = bm.user_id AND first_metric.metric_date = bm.measurement_date",
            "  ) start_metric",
            "INNER JOIN",
            "  (",
            "    SELECT bm.user_id, bm.weight_kg",
            "    FROM body_metrics bm",
            "    INNER JOIN (",
            "      SELECT user_id, MAX(measurement_date) AS metric_date",
            "      FROM body_metrics",
            "      <where>",
            "        <if test='startDate != null'> AND measurement_date &gt;= #{startDate} </if>",
            "        <if test='endDate != null'> AND measurement_date &lt;= #{endDate} </if>",
            "      </where>",
            "      GROUP BY user_id",
            "    ) last_metric ON last_metric.user_id = bm.user_id AND last_metric.metric_date = bm.measurement_date",
            "  ) end_metric ON start_metric.user_id = end_metric.user_id",
            "HAVING totalValue &gt; 0",
            "ORDER BY totalValue DESC",
            "LIMIT #{limit}",
            "</script>"
    })
    List<Map<String, Object>> sumWeightLossByUserInRange(@Param("startDate") LocalDate startDate,
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("limit") int limit);
}
