package com.gym.fitness.service.dto.user;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class PrivacySettingsRequest {
    @NotNull(message = "排行榜显示设置不能为空")
    private Boolean showInLeaderboard;
    
    @NotNull(message = "教练查看权限设置不能为空")
    private Boolean allowCoachView;
}
