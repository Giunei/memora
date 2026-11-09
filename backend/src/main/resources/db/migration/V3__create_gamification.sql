-- ---------------------------------------------------------------------------
-- Gamification: the single source of truth for XP, level and streak.
-- Awarding rules live in the backend only; the client never writes these.
-- ---------------------------------------------------------------------------

CREATE TABLE user_progress (
    id                  UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    total_xp            BIGINT      NOT NULL DEFAULT 0,
    level               INTEGER     NOT NULL DEFAULT 1,
    current_streak_days INTEGER     NOT NULL DEFAULT 0,
    longest_streak_days INTEGER     NOT NULL DEFAULT 0,
    last_study_date     DATE,
    total_study_minutes INTEGER     NOT NULL DEFAULT 0,
    daily_goal_minutes  INTEGER     NOT NULL DEFAULT 15,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_progress PRIMARY KEY (id),
    CONSTRAINT fk_user_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ux_user_progress_user UNIQUE (user_id),
    CONSTRAINT ck_user_progress_total_xp CHECK (total_xp >= 0),
    CONSTRAINT ck_user_progress_level CHECK (level >= 1),
    CONSTRAINT ck_user_progress_current_streak CHECK (current_streak_days >= 0),
    CONSTRAINT ck_user_progress_longest_streak CHECK (longest_streak_days >= current_streak_days),
    CONSTRAINT ck_user_progress_study_minutes CHECK (total_study_minutes >= 0),
    CONSTRAINT ck_user_progress_daily_goal CHECK (daily_goal_minutes BETWEEN 5 AND 480)
);
