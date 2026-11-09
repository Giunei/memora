package com.memora.gamification.domain;

import com.memora.shared.domain.BaseEntity;
import com.memora.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The authoritative record of a user's XP, level, streak and study time.
 *
 * <p>Every value here is written by the backend only. The client reads it and
 * never submits it, which is what keeps the gamification rules trustworthy.
 */
@Entity
@Table(name = "user_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProgress extends BaseEntity {

    public static final int MIN_DAILY_GOAL_MINUTES = 5;
    public static final int MAX_DAILY_GOAL_MINUTES = 480;
    public static final int DEFAULT_DAILY_GOAL_MINUTES = 15;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private long totalXp;

    @Column(nullable = false)
    private int level;

    @Column(name = "current_streak_days", nullable = false)
    private int currentStreakDays;

    @Column(name = "longest_streak_days", nullable = false)
    private int longestStreakDays;

    @Column(name = "last_study_date")
    private LocalDate lastStudyDate;

    @Column(name = "total_study_minutes", nullable = false)
    private int totalStudyMinutes;

    @Column(name = "daily_goal_minutes", nullable = false)
    private int dailyGoalMinutes;

    /** A fresh account starts at level 1 with nothing accumulated. */
    public static UserProgress initialFor(User user) {
        UserProgress progress = new UserProgress();
        progress.user = user;
        progress.totalXp = 0L;
        progress.level = LevelCurve.levelFor(0L);
        progress.currentStreakDays = 0;
        progress.longestStreakDays = 0;
        progress.totalStudyMinutes = 0;
        progress.dailyGoalMinutes = DEFAULT_DAILY_GOAL_MINUTES;
        return progress;
    }

    public void changeDailyGoal(int minutes) {
        if (minutes < MIN_DAILY_GOAL_MINUTES || minutes > MAX_DAILY_GOAL_MINUTES) {
            throw new IllegalArgumentException(
                    "Daily goal must be between %d and %d minutes"
                            .formatted(MIN_DAILY_GOAL_MINUTES, MAX_DAILY_GOAL_MINUTES));
        }
        this.dailyGoalMinutes = minutes;
    }

    public long xpIntoLevel() {
        return LevelCurve.xpIntoLevel(totalXp);
    }

    public long xpForNextLevel() {
        return LevelCurve.xpForNextLevel(totalXp);
    }
}
