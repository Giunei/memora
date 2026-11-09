package com.memora.gamification.web.dto;

import com.memora.gamification.domain.UserProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(name = "UpdateDailyGoalRequest")
public record UpdateDailyGoalRequest(
        @Min(UserProgress.MIN_DAILY_GOAL_MINUTES)
                @Max(UserProgress.MAX_DAILY_GOAL_MINUTES)
                @Schema(example = "20", description = "Daily study target in minutes")
                int dailyGoalMinutes) {}
