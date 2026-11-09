package com.memora.gamification.web.dto;

import com.memora.gamification.domain.UserProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "ProgressResponse", description = "XP, level, streak and study time")
public record ProgressResponse(
        @Schema(example = "0") long totalXp,
        @Schema(example = "1") int level,
        @Schema(example = "0", description = "XP accumulated inside the current level") long xpIntoLevel,
        @Schema(example = "100", description = "XP the current level spans") long xpForNextLevel,
        @Schema(example = "0") int currentStreakDays,
        @Schema(example = "0") int longestStreakDays,
        @Schema(description = "Null until the first completed study session") LocalDate lastStudyDate,
        @Schema(example = "0") int totalStudyMinutes,
        @Schema(example = "15") int dailyGoalMinutes) {

    public static ProgressResponse from(UserProgress progress) {
        return new ProgressResponse(
                progress.getTotalXp(),
                progress.getLevel(),
                progress.xpIntoLevel(),
                progress.xpForNextLevel(),
                progress.getCurrentStreakDays(),
                progress.getLongestStreakDays(),
                progress.getLastStudyDate(),
                progress.getTotalStudyMinutes(),
                progress.getDailyGoalMinutes());
    }
}
