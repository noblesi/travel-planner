/*
 * Existing public plans were published before automatic thumbnail derivation.
 * These statements are idempotent and apply the same server-owned category order
 * used by PlanScheduleItemMapper.findFirstImageUrlByPlanId.
 */
MERGE INTO TRAVEL_PLAN P
USING (
    SELECT PLAN_ID, THUMBNAIL_IMAGE_URL
      FROM (
          SELECT D.PLAN_ID,
                 COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT) AS THUMBNAIL_IMAGE_URL,
                 ROW_NUMBER() OVER (
                     PARTITION BY D.PLAN_ID
					 ORDER BY COALESCE(
								  CASE PM.PLACE_TYPE
									  WHEN 'ATTRACTION' THEN 0
									  WHEN 'CULTURAL_FACILITY' THEN 1
									  WHEN 'FESTIVAL_EVENT' THEN 2
									  WHEN 'TRAVEL_COURSE' THEN 3
									  WHEN 'LEISURE_SPORTS' THEN 4
									  WHEN 'TOURIST_INFORMATION' THEN 5
									  WHEN 'SHOPPING' THEN 6
									  ELSE NULL
								  END,
								  CASE I.CATEGORY_SNAPSHOT
									  WHEN '관광지' THEN 0
									  WHEN '문화시설' THEN 1
									  WHEN '축제·공연·행사' THEN 2
									  WHEN '여행코스' THEN 3
									  WHEN '레포츠' THEN 4
									  WHEN '관광정보' THEN 5
									  WHEN '쇼핑' THEN 6
									  ELSE NULL
								  END,
								  7
							  ),
                              D.DAY_NO,
                              CASE I.TIME_SLOT WHEN 'MORNING' THEN 0 ELSE 1 END,
                              I.POSITION_NO,
                              I.SCHEDULE_ITEM_ID
                 ) AS CANDIDATE_RANK
            FROM PLAN_SCHEDULE_ITEM I
            JOIN PLAN_DAY D ON D.PLAN_DAY_ID = I.PLAN_DAY_ID
            LEFT JOIN PLACE_MASTER PM
              ON PM.PLACE_PROVIDER = I.PLACE_PROVIDER
             AND PM.EXTERNAL_PLACE_ID = I.EXTERNAL_PLACE_ID
             AND PM.ACTIVE_YN = 'Y'
           WHERE COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT) IS NOT NULL
			 AND (
				 LOWER(COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT)) LIKE 'https://%'
				 OR LOWER(COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT)) LIKE 'http://%'
			 )
			 AND (
				 PM.PLACE_TYPE IN (
					 'ATTRACTION', 'CULTURAL_FACILITY', 'FESTIVAL_EVENT',
					 'TRAVEL_COURSE', 'LEISURE_SPORTS', 'TOURIST_INFORMATION', 'SHOPPING'
				 )
				 OR (PM.PLACE_TYPE IS NULL AND I.CATEGORY_SNAPSHOT IN (
					 '관광지', '문화시설', '축제·공연·행사', '여행코스',
					 '레포츠', '관광정보', '쇼핑'
				 ))
			 )
      )
     WHERE CANDIDATE_RANK = 1
) C
ON (
    P.PLAN_ID = C.PLAN_ID
    AND P.VISIBILITY = 'PUBLIC'
    AND P.PUBLISH_STATUS = 'PUBLISHED'
    AND P.PLAN_STATUS = 'ACTIVE'
)
WHEN MATCHED THEN
    UPDATE SET P.THUMBNAIL_IMG = C.THUMBNAIL_IMAGE_URL;

UPDATE TRAVEL_PLAN P
   SET THUMBNAIL_IMG = NULL
 WHERE P.VISIBILITY = 'PUBLIC'
   AND P.PUBLISH_STATUS = 'PUBLISHED'
   AND P.PLAN_STATUS = 'ACTIVE'
   AND NOT EXISTS (
       SELECT 1
         FROM PLAN_SCHEDULE_ITEM I
         JOIN PLAN_DAY D ON D.PLAN_DAY_ID = I.PLAN_DAY_ID
         LEFT JOIN PLACE_MASTER PM
           ON PM.PLACE_PROVIDER = I.PLACE_PROVIDER
          AND PM.EXTERNAL_PLACE_ID = I.EXTERNAL_PLACE_ID
          AND PM.ACTIVE_YN = 'Y'
        WHERE D.PLAN_ID = P.PLAN_ID
          AND COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT) IS NOT NULL
		  AND (
			  LOWER(COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT)) LIKE 'https://%'
			  OR LOWER(COALESCE(PM.IMAGE_URL, I.IMAGE_URL_SNAPSHOT)) LIKE 'http://%'
		  )
		  AND (
			  PM.PLACE_TYPE IN (
				  'ATTRACTION', 'CULTURAL_FACILITY', 'FESTIVAL_EVENT',
				  'TRAVEL_COURSE', 'LEISURE_SPORTS', 'TOURIST_INFORMATION', 'SHOPPING'
			  )
			  OR (PM.PLACE_TYPE IS NULL AND I.CATEGORY_SNAPSHOT IN (
				  '관광지', '문화시설', '축제·공연·행사', '여행코스',
				  '레포츠', '관광정보', '쇼핑'
			  ))
		  )
   );

COMMIT;

SELECT COUNT(*) AS PUBLIC_PLAN_COUNT,
       COUNT(THUMBNAIL_IMG) AS PUBLIC_PLAN_WITH_THUMBNAIL_COUNT
  FROM TRAVEL_PLAN
 WHERE VISIBILITY = 'PUBLIC'
   AND PUBLISH_STATUS = 'PUBLISHED'
   AND PLAN_STATUS = 'ACTIVE';
