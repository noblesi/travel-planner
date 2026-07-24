-- TourAPI areaCode2 SIDO seed data.
-- REGION_CODE stores the TourAPI areaCode value without transformation.
-- Future SIGUNGU rows must use "{areaCode}-{sigunguCode}" as REGION_CODE.

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

MERGE INTO REGION_MASTER target
USING (
    SELECT '1' REGION_CODE, '서울특별시' REGION_NAME, 1 DISPLAY_ORDER FROM DUAL
    UNION ALL SELECT '6',  '부산광역시',       2  FROM DUAL
    UNION ALL SELECT '4',  '대구광역시',       3  FROM DUAL
    UNION ALL SELECT '2',  '인천광역시',       4  FROM DUAL
    UNION ALL SELECT '5',  '광주광역시',       5  FROM DUAL
    UNION ALL SELECT '3',  '대전광역시',       6  FROM DUAL
    UNION ALL SELECT '7',  '울산광역시',       7  FROM DUAL
    UNION ALL SELECT '8',  '세종특별자치시',    8  FROM DUAL
    UNION ALL SELECT '31', '경기도',           9  FROM DUAL
    UNION ALL SELECT '32', '강원특별자치도',   10 FROM DUAL
    UNION ALL SELECT '33', '충청북도',         11 FROM DUAL
    UNION ALL SELECT '34', '충청남도',         12 FROM DUAL
    UNION ALL SELECT '37', '전북특별자치도',   13 FROM DUAL
    UNION ALL SELECT '38', '전라남도',         14 FROM DUAL
    UNION ALL SELECT '35', '경상북도',         15 FROM DUAL
    UNION ALL SELECT '36', '경상남도',         16 FROM DUAL
    UNION ALL SELECT '39', '제주특별자치도',   17 FROM DUAL
) source
ON (target.REGION_CODE = source.REGION_CODE)
WHEN MATCHED THEN
    UPDATE SET
        target.REGION_NAME = source.REGION_NAME,
        target.PARENT_REGION_CODE = NULL,
        target.REGION_LEVEL = 'SIDO',
        target.DISPLAY_ORDER = source.DISPLAY_ORDER,
        target.ACTIVE_YN = 'Y'
WHEN NOT MATCHED THEN
    INSERT (
        REGION_CODE,
        REGION_NAME,
        PARENT_REGION_CODE,
        REGION_LEVEL,
        DISPLAY_ORDER,
        ACTIVE_YN
    )
    VALUES (
        source.REGION_CODE,
        source.REGION_NAME,
        NULL,
        'SIDO',
        source.DISPLAY_ORDER,
        'Y'
    );

COMMIT;
