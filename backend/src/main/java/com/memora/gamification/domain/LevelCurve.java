package com.memora.gamification.domain;

/**
 * Maps accumulated XP onto levels.
 *
 * <p>The curve is quadratic: advancing from level {@code L} to {@code L + 1}
 * costs {@code 100 * L} XP, so the cumulative requirement to reach level
 * {@code L} is {@code 50 * L * (L - 1)}. Early levels arrive quickly, which is
 * what makes the first sessions feel rewarding, while later ones stretch out.
 *
 * <p>Pure functions with no dependencies, so the progression can be verified
 * exactly by tests and reasoned about without a database.
 */
public final class LevelCurve {

    private static final int XP_PER_LEVEL_STEP = 100;

    private LevelCurve() {}

    /** Total XP needed to have reached {@code level}. Level 1 starts at zero. */
    public static long totalXpToReach(int level) {
        requireValidLevel(level);
        return (long) XP_PER_LEVEL_STEP * level * (level - 1) / 2;
    }

    /** XP needed to advance from {@code level} to the next one. */
    public static long xpSpanOf(int level) {
        requireValidLevel(level);
        return (long) XP_PER_LEVEL_STEP * level;
    }

    public static int levelFor(long totalXp) {
        if (totalXp < 0) {
            throw new IllegalArgumentException("totalXp must not be negative: " + totalXp);
        }
        // Closed-form inverse of the cumulative curve, then corrected so that
        // floating-point rounding can never shift a level boundary.
        int estimate = Math.max(1, (int) Math.floor((1 + Math.sqrt(1 + 0.08 * totalXp)) / 2));
        while (totalXpToReach(estimate + 1) <= totalXp) {
            estimate++;
        }
        while (estimate > 1 && totalXpToReach(estimate) > totalXp) {
            estimate--;
        }
        return estimate;
    }

    /** XP accumulated inside the current level, i.e. the progress-bar numerator. */
    public static long xpIntoLevel(long totalXp) {
        return totalXp - totalXpToReach(levelFor(totalXp));
    }

    /** XP the current level spans, i.e. the progress-bar denominator. */
    public static long xpForNextLevel(long totalXp) {
        return xpSpanOf(levelFor(totalXp));
    }

    private static void requireValidLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1: " + level);
        }
    }
}
