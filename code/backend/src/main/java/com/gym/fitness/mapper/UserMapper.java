package com.gym.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.fitness.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    List<User> selectActiveUsers(@Param("days") int days);
    
    @Select("SELECT COUNT(*) FROM users WHERE user_role = #{role}")
    int countUsersByRole(@Param("role") String role);
}
