-- ============================================================
-- V1 - Initial Schema
-- Creates all core tables matching JPA entity definitions
-- ============================================================

-- app_user table (for authentication)
CREATE TABLE IF NOT EXISTS app_user (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(255) UNIQUE,
    password     VARCHAR(255),
    provider_id  VARCHAR(255),
    provider_type VARCHAR(50),
    CONSTRAINT idx_provider_id_provider_type UNIQUE (provider_id, provider_type)
);

-- users table (profile data, separate from auth)
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    number        VARCHAR(10)   NOT NULL UNIQUE,
    location      VARCHAR(100)  NOT NULL,
    experience    INTEGER       NOT NULL,
    profile_photo VARCHAR(255),
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    fcm_token     VARCHAR(255),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- skills table
CREATE TABLE IF NOT EXISTS skills (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- user_skills join table
CREATE TABLE IF NOT EXISTS user_skills (
    user_id  BIGINT NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, skill_id)
);

-- job table
CREATE TABLE IF NOT EXISTS job (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(100)     NOT NULL,
    description         VARCHAR(300),
    salary              DOUBLE PRECISION,
    location            VARCHAR(100),
    job_type            VARCHAR(50),
    experience_required INTEGER,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    user_id             BIGINT           REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- job_skills join table
CREATE TABLE IF NOT EXISTS job_skills (
    job_id   BIGINT NOT NULL REFERENCES job(id)    ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (job_id, skill_id)
);

-- job_application table
CREATE TABLE IF NOT EXISTS job_application (
    id         BIGSERIAL PRIMARY KEY,
    job_id     BIGINT NOT NULL REFERENCES job(id)   ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status     VARCHAR(50),
    applied_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- refresh_token table
CREATE TABLE IF NOT EXISTS refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    app_user_id BIGINT       REFERENCES app_user(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE
);
