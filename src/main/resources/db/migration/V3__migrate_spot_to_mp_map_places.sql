INSERT INTO mp_map_places (place_type, original_id, name, district, category,
                           longitude, latitude, image_url, min_price, is_free,
                           is_active, created_at, updated_at)
SELECT 'SPOT',
       s.content_id,
       s.title,
       split_part(trim(d.addr1), ' ', 2),
       CASE s.lcls_systm_1
           WHEN '자연' THEN 'NA'
           WHEN '역사' THEN 'HS'
           WHEN '레포츠' THEN 'LS'
           WHEN '체험관광' THEN 'EX'
           WHEN '문화시설' THEN 'VE'
           WHEN '문화관광' THEN 'VE'
           WHEN '축제공연행사' THEN 'VE'
           END,
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
WHERE CASE s.lcls_systm_1
          WHEN '자연' THEN 'NA'
          WHEN '역사' THEN 'HS'
          WHEN '레포츠' THEN 'LS'
          WHEN '체험관광' THEN 'EX'
          WHEN '문화시설' THEN 'VE'
          WHEN '문화관광' THEN 'VE'
          WHEN '축제공연행사' THEN 'VE'
          END IS NOT NULL
ON CONFLICT (place_type, original_id) DO NOTHING;
