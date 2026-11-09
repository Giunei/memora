package com.memora.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LevelCurveTest {

    @Test
    @DisplayName("a new account with no XP is level 1")
    void startsAtLevelOne() {
        assertThat(LevelCurve.levelFor(0L)).isEqualTo(1);
        assertThat(LevelCurve.xpIntoLevel(0L)).isZero();
        assertThat(LevelCurve.xpForNextLevel(0L)).isEqualTo(100L);
    }

    @ParameterizedTest(name = "{0} XP cumulative reaches level {1}")
    @CsvSource({
        "0, 1",
        "99, 1",
        "100, 2",
        "299, 2",
        "300, 3",
        "599, 3",
        "600, 4",
        "1000, 5",
        "1500, 6",
        "4499, 9",
        "4500, 10",
        "5499, 10",
        "5500, 11"
    })
    @DisplayName("level boundaries sit exactly on the cumulative requirement")
    void mapsXpToLevel(long totalXp, int expectedLevel) {
        assertThat(LevelCurve.levelFor(totalXp)).isEqualTo(expectedLevel);
    }

    @ParameterizedTest(name = "reaching level {0} costs {1} XP in total")
    @CsvSource({"1, 0", "2, 100", "3, 300", "4, 600", "5, 1000", "10, 4500", "11, 5500"})
    void reportsCumulativeRequirement(int level, long expectedTotalXp) {
        assertThat(LevelCurve.totalXpToReach(level)).isEqualTo(expectedTotalXp);
    }

    @Test
    @DisplayName("each level costs 100 XP more than the previous one")
    void spanGrowsLinearly() {
        for (int level = 1; level <= 20; level++) {
            assertThat(LevelCurve.xpSpanOf(level)).isEqualTo(100L * level);
            assertThat(LevelCurve.totalXpToReach(level) + LevelCurve.xpSpanOf(level))
                    .as("span of level %d must close the gap to level %d", level, level + 1)
                    .isEqualTo(LevelCurve.totalXpToReach(level + 1));
        }
    }

    @Test
    @DisplayName("progress inside a level never exceeds that level's span")
    void progressStaysWithinLevel() {
        for (long xp = 0; xp <= 20_000; xp += 37) {
            long into = LevelCurve.xpIntoLevel(xp);
            long span = LevelCurve.xpForNextLevel(xp);
            assertThat(into).as("xp=%d", xp).isBetween(0L, span - 1);
        }
    }

    @Test
    @DisplayName("the closed-form inverse agrees with an incremental walk up the curve")
    void inverseMatchesIncrementalWalk() {
        int expectedLevel = 1;
        for (long xp = 0; xp <= 200_000; xp++) {
            if (xp >= LevelCurve.totalXpToReach(expectedLevel + 1)) {
                expectedLevel++;
            }
            if (xp % 991 == 0) {
                assertThat(LevelCurve.levelFor(xp)).as("xp=%d", xp).isEqualTo(expectedLevel);
            }
        }
    }

    @Test
    void rejectsNegativeXp() {
        assertThatIllegalArgumentException().isThrownBy(() -> LevelCurve.levelFor(-1L));
    }

    @Test
    void rejectsLevelsBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> LevelCurve.totalXpToReach(0));
        assertThatIllegalArgumentException().isThrownBy(() -> LevelCurve.xpSpanOf(0));
    }
}
