package com.gym.fitness.service.dto.user;

import lombok.Data;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String realName;
    private Integer age;
    private String gender;
    private String role;
    private String fitnessGoal;
    private Long coachId;
    private Boolean showInLeaderboard;
    private Boolean allowCoachView;
}
