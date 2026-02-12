package com.gym.fitness.service.dto.auth;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 15, message = "年龄必须大于等于15")
    @Max(value = 80, message = "年龄必须小于等于80")
    private Integer age;
    
    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "性别必须是MALE或FEMALE")
    private String gender;
    
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(STUDENT|COACH)$", message = "角色必须是STUDENT或COACH")
    private String role;
    
    private String fitnessGoal;
}
