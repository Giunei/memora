-- ---------------------------------------------------------------------------
-- Identity: accounts, roles and refresh-token storage.
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id            UUID         NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    locale        VARCHAR(16)  NOT NULL DEFAULT 'pt-BR',
    time_zone     VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- Emails are stored as the user typed them but compared case-insensitively,
-- so uniqueness has to be enforced on the normalised value.
CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));

CREATE TABLE user_roles (
    user_id UUID        NOT NULL,
    role    VARCHAR(30) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'ADMIN'))
);

-- Refresh tokens are opaque random values; only their SHA-256 hex digest is
-- persisted, so a database leak does not yield usable credentials (ADR 0002).
-- `family_id` groups a rotation chain: reuse of an already-consumed token
-- revokes the entire family.
CREATE TABLE refresh_tokens (
    id          UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    family_id   UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);
