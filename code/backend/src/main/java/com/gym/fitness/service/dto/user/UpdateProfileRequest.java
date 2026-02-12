package com.gym.fitness.service.dto.user;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 15, message = "年龄必须大于等于15")
    @Max(value = 80, message = "年龄必须小于等于80")
    private Integer age;
    
    @NotBlank(message = "性别不能为空")
    private String gender;
    
    private String fitnessGoal;
}
