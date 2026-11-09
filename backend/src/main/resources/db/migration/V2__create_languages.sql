-- ---------------------------------------------------------------------------
-- Languages catalogue and per-user enrolment.
-- ---------------------------------------------------------------------------

CREATE TABLE languages (
    id          UUID        NOT NULL,
    code        VARCHAR(10) NOT NULL,
    name        VARCHAR(80) NOT NULL,
    native_name VARCHAR(80) NOT NULL,
    flag_emoji  VARCHAR(16),
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_languages PRIMARY KEY (id),
    CONSTRAINT ux_languages_code UNIQUE (code)
);

-- A user studies many languages; each enrolment carries its own CEFR position,
-- because a B2 in English and an A1 in Japanese are unrelated. The daily study
-- goal is deliberately *not* here: it is a single account-wide target stored on
-- `user_progress`, so the dashboard has one unambiguous source of truth.
CREATE TABLE user_languages (
    id              UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    language_id     UUID        NOT NULL,
    current_level   VARCHAR(2)  NOT NULL,
    target_level    VARCHAR(2)  NOT NULL,
    primary_focus   BOOLEAN     NOT NULL DEFAULT FALSE,
    started_at      TIMESTAMPTZ NOT NULL,
    last_studied_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_languages PRIMARY KEY (id),
    CONSTRAINT fk_user_languages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_languages_language FOREIGN KEY (language_id) REFERENCES languages (id),
    CONSTRAINT ux_user_languages_user_language UNIQUE (user_id, language_id),
    CONSTRAINT ck_user_languages_current_level CHECK (current_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    CONSTRAINT ck_user_languages_target_level CHECK (target_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);

CREATE INDEX ix_user_languages_user ON user_languages (user_id);

-- At most one language can be the current focus for a given user. Enforcing it
-- in the database removes the possibility of a concurrent write producing two.
CREATE UNIQUE INDEX ux_user_languages_primary_focus
    ON user_languages (user_id)
    WHERE primary_focus;
