INSERT INTO mp_map_places (place_type, original_id, name, district, category,
                           longitude, latitude, image_url, min_price, is_free,
                           is_active, created_at, updated_at)
SELECT 'FOOD',
       f.id::VARCHAR,
       f.name,
       f.source_region,
       'FD',
       f.map_x,
       f.map_y,
       NULL,
       (SELECT MIN(m.price)
        FROM fd_food_place_menus m
        WHERE m.food_place_id = f.id
          AND m.price IS NOT NULL),
       COALESCE(
               (SELECT MIN(m.price)
                FROM fd_food_place_menus m
                WHERE m.food_place_id = f.id
                  AND m.price IS NOT NULL),
               -1) = 0,
       f.is_active,
       NOW(),
       NOW()
FROM fd_food_places f
ON CONFLICT (place_type, original_id) DO NOTHING;
