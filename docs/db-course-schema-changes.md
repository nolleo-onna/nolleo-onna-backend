# Course 도메인 스키마 변경 정리 — 참조 축 전환 + 좋아요

> 순서: 실제 적용은 1단계 → 2단계 → **0단계(002)** 순으로 진행됨. 0단계는 그에 맞춰 수정된 판본
> 원칙: DDL은 리포에 담지 않는다. 이 문서를 개발서버(RDS)에 수동 적용하고 결과를 이슈에 남긴다
> 주의: 시퀀스 소유자 불일치가 확인됨(`generated_course_items_id_seq1`). DDL은 **테이블 소유자 계정**으로 실행

## 1. 최종 목표 스키마

### generated_courses

| 컬럼 | 타입 | NULL | 기본 | 상태 |
|---|---|---|---|---|
| `id` | BIGSERIAL | NN | PK | |
| `user_id` | BIGINT | NN | | mb_user_info.id, FK 없음 |
| `pair_id` | UUID | NN | | 002에서 NOT NULL |
| `generation_mode` | VARCHAR(20) | NN | | AI \| ALGORITHM |
| `course_type` | VARCHAR(20) | | | ALGORITHM 전용 |
| `title` | VARCHAR(200) | NN | | |
| `description` | TEXT | | | |
| `intent` | JSONB | | | |
| `total_cost` | INTEGER | | | |
| `is_public` | BOOLEAN | NN | FALSE | |
| `share_token` | VARCHAR(64) | | | |
| `view_count` | INTEGER | NN | 0 | |
| **`like_count`** | **INTEGER** | **NN** | **0** | **신규** — 좋아요 역정규화 |
| `created_at` / `created_by` | TIMESTAMPTZ / VARCHAR(50) | NN / | NOW() / | 002에서 50자 |
| `updated_at` / `updated_by` | TIMESTAMPTZ / VARCHAR(50) | | | |
| `deleted_at` / `deleted_by` | TIMESTAMPTZ / VARCHAR(50) | | | 소프트 삭제 |

제약·인덱스
- `ck_generated_courses_generation_mode`, `ck_generated_courses_course_type`, `ck_generated_courses_type_by_mode` (002)
- **`ck_generated_courses_non_negative`** — 002 것을 `like_count >= 0` 포함하도록 **재정의**
- `uq_generated_courses_share_token` 부분 UNIQUE (002)
- `idx_generated_courses_user_created`, `idx_generated_courses_pair_id` 부분 인덱스 (002)

### generated_course_items

| 컬럼 | 타입 | NULL | 기본 | 상태 |
|---|---|---|---|---|
| `id` | BIGSERIAL | NN | PK | |
| `course_id` | BIGINT | NN | | FK → generated_courses ON DELETE CASCADE |
| `serial_num` | SMALLINT | NN | | 방문 순번, 1부터 |
| **`place_type`** | **VARCHAR(10)** | **NN** | | **신규** — SPOT \| FOOD |
| **`original_id`** | **VARCHAR(50)** | **NN** | | **`spot_content_id` 리네임 + 20→50 확장** |
| `expected_cost` | INTEGER | | | |
| `distance_from_prev_m` | INTEGER | | | 002에서 SMALLINT→INTEGER |
| `created_at` / `created_by` | TIMESTAMPTZ / VARCHAR(50) | NN / | NOW() / | |
| `updated_at` / `updated_by` | TIMESTAMPTZ / VARCHAR(50) | | | |

제약·인덱스
- `uq_generated_course_items_course_serial` UNIQUE (course_id, serial_num) (002)
- `ck_generated_course_items_serial_num`, `ck_generated_course_items_non_negative` (002)
- **`ck_generated_course_items_place_type`** CHECK (place_type IN ('SPOT','FOOD')) — 신규
- **`uq_generated_course_items_course_place`** UNIQUE (course_id, place_type, original_id) — 신규, 같은 장소 중복 담기 방지
- `idx_generated_course_items_course_id` (001)

참조 규칙: `place_type = 'SPOT'` → `sp_spots.content_id` / `'FOOD'` → `fd_food_places.id`. 둘 다 FK 없음 (컨텍스트 경계)

### generated_course_likes — 신규

| 컬럼 | 타입 | NULL | 기본 | 비고 |
|---|---|---|---|---|
| `id` | BIGSERIAL | NN | PK | |
| `course_id` | BIGINT | NN | | FK → generated_courses ON DELETE CASCADE |
| `user_id` | BIGINT | NN | | mb_user_info.id, FK 없음 |
| `created_at` | TIMESTAMPTZ | NN | NOW() | |

제약·인덱스
- `uq_generated_course_likes_course_user` UNIQUE (course_id, user_id) — 1인 1회
- `idx_generated_course_likes_user_id` — "내가 좋아요한 코스". course_id 단독 인덱스는 UNIQUE 선두 컬럼이 대신

## 2. 적용 순서와 DDL

### 0단계 — 002 스키마 정리 (현재 상태 맞춤판)

> 원본 `002_course_schema_cleanup.sql`은 리포에서 제거됨. 아래가 유일한 사본이다.
> **1·2단계가 먼저 적용된 뒤에 실행하는 상황**을 전제로 3곳을 원본에서 수정했다:
> `spot_content_id` → `original_id`, `ck_generated_courses_non_negative` 제외(이미 like_count 포함 버전 존재),
> `spot_content_id` COMMENT 제거(1단계에서 처리). 순서가 달라도 무해하도록 `IF EXISTS` / `IF NOT EXISTS`를 유지.

```sql
-- [실행 전 검증] — 테이블이 비어 있으면 전부 0 / 없음
SELECT count(*) FROM generated_courses      WHERE pair_id IS NULL;
SELECT count(*) FROM generated_course_items WHERE original_id IS NULL;
SELECT count(*) FROM generated_courses
 WHERE length(created_by) > 50 OR length(updated_by) > 50 OR length(deleted_by) > 50;
SELECT course_id, serial_num, count(*) FROM generated_course_items
 GROUP BY 1, 2 HAVING count(*) > 1;

BEGIN;

-- 1. 레거시 테이블 제거 (#33 이전 스키마, 애플리케이션 매핑 없음)
DROP TABLE IF EXISTS cs_generated_course_items;
DROP TABLE IF EXISTS cs_generated_courses;

-- 2. 거리 컬럼 오버플로 제거 — SMALLINT 상한 32,767m, 기장↔가덕도 약 50km에서 넘침
ALTER TABLE generated_course_items
    ALTER COLUMN distance_from_prev_m TYPE INTEGER;

-- 3. NULL 허용 범위를 도메인 규칙에 맞춤
ALTER TABLE generated_courses      ALTER COLUMN pair_id     SET NOT NULL;
ALTER TABLE generated_course_items ALTER COLUMN original_id SET NOT NULL;

-- 4. 감사 컬럼 길이 — 임베더블(CreateAudit/UpdateAudit/SoftDeleteAudit)의 length=50에 맞춤
ALTER TABLE generated_courses      ALTER COLUMN created_by TYPE VARCHAR(50);
ALTER TABLE generated_courses      ALTER COLUMN updated_by TYPE VARCHAR(50);
ALTER TABLE generated_courses      ALTER COLUMN deleted_by TYPE VARCHAR(50);
ALTER TABLE generated_course_items ALTER COLUMN created_by TYPE VARCHAR(50);
ALTER TABLE generated_course_items ALTER COLUMN updated_by TYPE VARCHAR(50);

-- 5. 무결성 제약
ALTER TABLE generated_course_items
    ADD CONSTRAINT uq_generated_course_items_course_serial UNIQUE (course_id, serial_num);

ALTER TABLE generated_courses
    ADD CONSTRAINT ck_generated_courses_generation_mode
        CHECK (generation_mode IN ('AI', 'ALGORITHM')),
    ADD CONSTRAINT ck_generated_courses_course_type
        CHECK (course_type IS NULL OR course_type IN ('ACTIVE', 'CULTURE', 'FOOD_TOUR')),
    ADD CONSTRAINT ck_generated_courses_type_by_mode
        CHECK ((generation_mode = 'ALGORITHM' AND course_type IS NOT NULL)
            OR (generation_mode = 'AI'        AND course_type IS NULL));
-- ck_generated_courses_non_negative: 2단계(좋아요)에서 like_count 포함 버전으로 이미 생성 → 여기서 제외

ALTER TABLE generated_course_items
    ADD CONSTRAINT ck_generated_course_items_serial_num
        CHECK (serial_num > 0),
    ADD CONSTRAINT ck_generated_course_items_non_negative
        CHECK ((expected_cost IS NULL OR expected_cost >= 0)
           AND (distance_from_prev_m IS NULL OR distance_from_prev_m >= 0));

-- 6. 공유 토큰 유일성 — 미공개(NULL) 다수 허용, 발급된 토큰만 유일
CREATE UNIQUE INDEX IF NOT EXISTS uq_generated_courses_share_token
    ON generated_courses (share_token)
    WHERE share_token IS NOT NULL;

-- 7. 조회 패턴에 맞춘 인덱스 재정비
DROP INDEX IF EXISTS idx_generated_courses_user_id;
DROP INDEX IF EXISTS idx_generated_courses_pair_id;

CREATE INDEX IF NOT EXISTS idx_generated_courses_user_created
    ON generated_courses (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_generated_courses_pair_id
    ON generated_courses (pair_id)
    WHERE deleted_at IS NULL;

-- 8. 문서화
COMMENT ON TABLE  generated_courses IS 'AI/알고리즘으로 생성된 여행 코스 (Course 애그리거트 루트)';
COMMENT ON COLUMN generated_courses.user_id         IS '코스를 저장한 사용자 ID (mb_user_info.id, FK 없음)';
COMMENT ON COLUMN generated_courses.pair_id         IS '같은 요청으로 생성된 형제 코스 묶음 UUID — 상세 조회 키';
COMMENT ON COLUMN generated_courses.generation_mode IS 'AI | ALGORITHM';
COMMENT ON COLUMN generated_courses.course_type     IS 'ALGORITHM 전용 — ACTIVE | CULTURE | FOOD_TOUR, AI는 NULL';
COMMENT ON COLUMN generated_courses.intent          IS 'CourseIntent 스냅샷 (재현·디버깅용, 조건 검색 대상 아님)';
COMMENT ON COLUMN generated_courses.total_cost      IS '아이템 expected_cost 합계 — 음식점 미포함 코스는 NULL';
COMMENT ON COLUMN generated_courses.share_token     IS '공개 공유 URL 토큰 — is_public=true일 때만 발급';
COMMENT ON COLUMN generated_courses.deleted_at      IS '소프트 삭제 시각 — 조회 쿼리는 IS NULL 조건 필수';

COMMENT ON TABLE  generated_course_items IS '코스에 포함된 방문 스팟 (Course 애그리거트의 자식)';
-- original_id / place_type 주석은 1단계에서 처리 → 제외
COMMENT ON COLUMN generated_course_items.expected_cost        IS '음식점만 — 관광지는 NULL';
COMMENT ON COLUMN generated_course_items.distance_from_prev_m IS '이전 지점으로부터 직선거리(m) — 1번 아이템은 지역 중심 기준';

COMMIT;
```

### 1단계 — 참조 축 전환 (generated_course_items)

```sql
BEGIN;

-- place_type 추가. 빈 테이블이지만 데이터가 있는 환경에서도 안전하도록 DEFAULT로 넣고 즉시 제거한다
-- (기본값을 남기면 place_type 누락 INSERT가 조용히 SPOT이 된다)
ALTER TABLE generated_course_items
    ADD COLUMN place_type VARCHAR(10) NOT NULL DEFAULT 'SPOT';
ALTER TABLE generated_course_items
    ALTER COLUMN place_type DROP DEFAULT;

-- spot_content_id → original_id 리네임 + 폭 확장 (VARCHAR 확장은 테이블 재작성 없음)
ALTER TABLE generated_course_items
    RENAME COLUMN spot_content_id TO original_id;
ALTER TABLE generated_course_items
    ALTER COLUMN original_id TYPE VARCHAR(50);

-- 무결성 제약
ALTER TABLE generated_course_items
    ADD CONSTRAINT ck_generated_course_items_place_type
        CHECK (place_type IN ('SPOT', 'FOOD')),
    ADD CONSTRAINT uq_generated_course_items_course_place
        UNIQUE (course_id, place_type, original_id);

COMMENT ON COLUMN generated_course_items.place_type
    IS 'SPOT | FOOD — original_id가 가리키는 원본 테이블';
COMMENT ON COLUMN generated_course_items.original_id
    IS 'SPOT → sp_spots.content_id, FOOD → fd_food_places.id — 컨텍스트 경계 유지를 위해 FK 없음';

COMMIT;
```

### 2단계 — 좋아요

```sql
BEGIN;

-- 카운트 역정규화 (pt_posts.like_count, mp_map_places.review_count 와 동일 패턴)
ALTER TABLE generated_courses
    ADD COLUMN like_count INTEGER NOT NULL DEFAULT 0;

-- 002의 non-negative CHECK를 like_count 포함으로 재정의
ALTER TABLE generated_courses
    DROP CONSTRAINT IF EXISTS ck_generated_courses_non_negative,
    ADD CONSTRAINT ck_generated_courses_non_negative
        CHECK (view_count >= 0 AND like_count >= 0 AND (total_cost IS NULL OR total_cost >= 0));

-- 좋아요 행 테이블
CREATE TABLE generated_course_likes (
    id          BIGSERIAL    PRIMARY KEY,
    course_id   BIGINT       NOT NULL REFERENCES generated_courses(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_generated_course_likes_course_user UNIQUE (course_id, user_id)
);
CREATE INDEX idx_generated_course_likes_user_id ON generated_course_likes (user_id);

COMMENT ON TABLE  generated_course_likes IS '코스 좋아요 — 사용자당 코스 1건, 공개 코스에만 허용(서비스 규칙). 비공개 전환 시 보존';
COMMENT ON COLUMN generated_course_likes.user_id IS 'mb_user_info.id, FK 없음';
COMMENT ON COLUMN generated_courses.like_count IS '좋아요 수 역정규화 — 토글 시 원자 UPDATE(like_count ± 1)로 증감';

COMMIT;
```

### 3단계 — 검증

```sql
-- 컬럼 확인 — place_type / original_id(50) / like_count 가 보여야 함
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns
WHERE table_name IN ('generated_courses', 'generated_course_items', 'generated_course_likes')
ORDER BY table_name, ordinal_position;

-- 제약 확인 — ck_..._place_type, uq_..._course_place, uq_..._course_user, 재정의된 non_negative
SELECT conrelid::regclass AS table_name, conname, contype, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid::regclass::text LIKE 'generated_course%'
ORDER BY 1, 2;

-- 제약 동작 확인 (모두 에러가 나야 정상)
-- INSERT INTO generated_course_items (course_id, serial_num, place_type, original_id) VALUES (1, 1, 'XX', 'a');      -- CHECK
-- INSERT INTO generated_course_items (course_id, serial_num, original_id) VALUES (1, 1, 'a');                        -- NOT NULL (DEFAULT 제거)
-- UPDATE generated_courses SET like_count = -1 WHERE id = 1;                                                          -- CHECK
```

## 3. 애플리케이션 영향 (스키마에 따라오는 코드 변경)

| 스키마 변경 | 코드 |
|---|---|
| `place_type` + `original_id` | `CoursePlaceType` enum, `PlaceRef` VO, `CourseItem`, `Course.addItem`, `CourseItemEntity`, `CourseGenerationService` 한 줄, `CourseQueryService.loadSpots`, Response 3종 |
| `like_count` | `ShareInfo`에 `likeCount` 추가, `CourseEntity` 매핑 |
| `generated_course_likes` | `CourseLike` 도메인 + 엔티티 + 리포지토리, 토글 서비스 (`PostLikeService` 패턴) |

## 4. 여전히 선택 사항 (002 하단, 변경 없음)

- `@Version` 낙관적 락 — `ALTER TABLE generated_courses ADD COLUMN version BIGINT NOT NULL DEFAULT 0;`
- `cs_` 접두사 리네임 — 이제 세 테이블(`generated_courses`, `_items`, `_likes`)이 함께 대상
