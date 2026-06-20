INSERT INTO mp_map_places (place_type, original_id, name, district, category,
                           longitude, latitude, image_url, min_price, is_free,
                           is_active, created_at, updated_at)
SELECT 'SPOT',
       s.content_id,
       s.title,
       split_part(trim(d.addr1), ' ', 2),
       s.lcls_systm_1,
       s.map_x,
       s.map_y,
       s.first_image,
       p.min_price,
       COALESCE(p.min_price, -1) = 0,
       s.is_active,
       NOW(),
       NOW()
FROM sp_spots s
         LEFT JOIN sp_spot_details d ON d.content_id = s.content_id
         LEFT JOIN sp_spot_price_summary p ON p.spot_content_id = s.content_id
WHERE s.lcls_systm_1 IN ('FD', 'VE', 'NA', 'HS', 'EX', 'LS')
ON CONFLICT (place_type, original_id) DO NOTHING;
