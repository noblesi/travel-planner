-- TourAPI areaCode2 SIDO seed data for the local in-memory database.

INSERT INTO REGION_MASTER
    (REGION_CODE, REGION_NAME, PARENT_REGION_CODE, REGION_LEVEL, DISPLAY_ORDER, ACTIVE_YN)
VALUES
    ('1',  '서울특별시',       NULL, 'SIDO', 1,  'Y'),
    ('6',  '부산광역시',       NULL, 'SIDO', 2,  'Y'),
    ('4',  '대구광역시',       NULL, 'SIDO', 3,  'Y'),
    ('2',  '인천광역시',       NULL, 'SIDO', 4,  'Y'),
    ('5',  '광주광역시',       NULL, 'SIDO', 5,  'Y'),
    ('3',  '대전광역시',       NULL, 'SIDO', 6,  'Y'),
    ('7',  '울산광역시',       NULL, 'SIDO', 7,  'Y'),
    ('8',  '세종특별자치시',   NULL, 'SIDO', 8,  'Y'),
    ('31', '경기도',           NULL, 'SIDO', 9,  'Y'),
    ('32', '강원특별자치도',   NULL, 'SIDO', 10, 'Y'),
    ('33', '충청북도',         NULL, 'SIDO', 11, 'Y'),
    ('34', '충청남도',         NULL, 'SIDO', 12, 'Y'),
    ('37', '전북특별자치도',   NULL, 'SIDO', 13, 'Y'),
    ('38', '전라남도',         NULL, 'SIDO', 14, 'Y'),
    ('35', '경상북도',         NULL, 'SIDO', 15, 'Y'),
    ('36', '경상남도',         NULL, 'SIDO', 16, 'Y'),
    ('39', '제주특별자치도',   NULL, 'SIDO', 17, 'Y');
