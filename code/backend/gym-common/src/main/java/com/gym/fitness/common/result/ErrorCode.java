package com.gym.fitness.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("0000", "操作成功"),
    
    // 通用错误 1xxx
    SYSTEM_ERROR("1000", "系统错误"),
    PARAM_ERROR("1001", "参数错误"),
    NOT_FOUND("1002", "资源不存在"),
    CONFLICT("1003", "数据冲突"),
    
    // 认证错误 2xxx
    UNAUTHORIZED("2000", "未授权"),
    TOKEN_INVALID("2001", "Token无效"),
    TOKEN_EXPIRED("2002", "Token已过期"),
    FORBIDDEN("2003", "权限不足"),
    LOGIN_FAILED("2004", "登录失败"),
    USERNAME_EXISTS("2005", "用户名已存在"),
    
    // 业务错误 3xxx
    USER_NOT_FOUND("3001", "用户不存在"),
    USER_ALREADY_EXISTS("3002", "用户已存在"),
    OPERATION_FAILED("3003", "操作失败"),
    EXERCISE_RECORD_NOT_FOUND("3004", "运动记录不存在"),
    BODY_METRIC_NOT_FOUND("3005", "身体指标不存在"),
    TRAINING_PLAN_NOT_FOUND("3006", "训练计划不存在"),
    ACHIEVEMENT_NOT_FOUND("3007", "成就不存在"),
    INVALID_DATE_RANGE("3008", "日期范围无效"),
    INVALID_BMI_PARAMS("3009", "BMI计算参数无效"),
    COACH_NOT_AUTHORIZED("3010", "教练无权访问该学员"),
    PLAN_DATE_INVALID("3011", "训练计划日期无效"),
    
    // 数据处理错误 4xxx
    DATA_CLEANING_ERROR("4001", "数据清洗失败"),
    DATA_ANALYSIS_ERROR("4002", "数据分析失败"),
    HDFS_ERROR("4003", "HDFS操作失败"),
    SPARK_ERROR("4004", "Spark任务失败");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
