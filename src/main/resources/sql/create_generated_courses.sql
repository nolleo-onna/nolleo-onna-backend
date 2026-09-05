-- #33 AI 자연어 코스 생성 — 신규 테이블
-- 기존 cs_generated_courses / cs_generated_course_items는 배제하고 새로 시작.
-- 픽스 후 개발서버 반영 시 기존 테이블 삭제 예정.

CREATE TABLE IF NOT EXISTS generated_courses (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    pair_id         UUID,
    generation_mode VARCHAR(20)  NOT NULL,               -- AI / ALGORITHM
    course_type     VARCHAR(20),                          -- ALGORITHM 모드만 사용, AI는 null
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    intent          JSONB,                                -- CourseIntent 스냅샷
    total_cost      INTEGER,                              -- 음식점 포함 시 가격 합산, 미포함 null
    is_public       BOOLEAN      NOT NULL DEFAULT FALSE,
    share_token     VARCHAR(64),
    view_count      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    deleted_at      TIMESTAMPTZ,
    deleted_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_generated_courses_user_id ON generated_courses (user_id);
CREATE INDEX IF NOT EXISTS idx_generated_courses_pair_id ON generated_courses (pair_id);

CREATE TABLE IF NOT EXISTS generated_course_items (
    id                   BIGSERIAL PRIMARY KEY,
    course_id            BIGINT    NOT NULL REFERENCES generated_courses(id) ON DELETE CASCADE,
    serial_num           SMALLINT  NOT NULL,              -- 방문 순서 (1부터)
    spot_content_id      VARCHAR(20),                     -- sp_spots.content_id 참조
    expected_cost        INTEGER,                         -- 음식점만, 관광지는 null
    distance_from_prev_m SMALLINT,                        -- PostGIS 직선거리
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_generated_course_items_course_id ON generated_course_items (course_id);
