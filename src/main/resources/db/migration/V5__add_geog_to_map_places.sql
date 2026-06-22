-- 기존 장소 중 geog 미설정 행에 대해 좌표값으로 채움
UPDATE mp_map_places
    SET geog = ST_MakePoint(longitude, latitude)::geography
    WHERE longitude IS NOT NULL AND latitude IS NOT NULL AND geog IS NULL;
