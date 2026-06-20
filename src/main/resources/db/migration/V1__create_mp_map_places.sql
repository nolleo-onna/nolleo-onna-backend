CREATE TABLE mp_map_places
(
    id          BIGSERIAL    PRIMARY KEY,
    place_type  VARCHAR(10)  NOT NULL,
    original_id VARCHAR(50)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    district    VARCHAR(50),
    category    VARCHAR(10)  NOT NULL,
    longitude   DECIMAL(10, 7),
    latitude    DECIMAL(10, 7),
    image_url   TEXT,
    min_price   INT,
    is_free     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mp_map_places_type_original UNIQUE (place_type, original_id)
);

CREATE INDEX idx_mp_map_places_district  ON mp_map_places (district);
CREATE INDEX idx_mp_map_places_category  ON mp_map_places (category);
CREATE INDEX idx_mp_map_places_min_price ON mp_map_places (min_price);
