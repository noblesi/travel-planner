-- Persistent showcase data for WITHTRIP_DEV only.
-- Run after the application schema and REGION_MASTER seed are present.

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    v_seoul_member_id NUMBER(19);
    v_coast_member_id NUMBER(19);
    v_island_member_id NUMBER(19);
    v_plan_id NUMBER(19);
    v_plan_start_date DATE;
    v_day_id NUMBER(19);

    PROCEDURE assert_target_schema IS
    BEGIN
        IF UPPER(SYS_CONTEXT('USERENV', 'SESSION_USER')) <> 'WITHTRIP_DEV'
           OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'WITHTRIP_DEV' THEN
            RAISE_APPLICATION_ERROR(-20001, 'Demo seed is allowed only in WITHTRIP_DEV.');
        END IF;
    END;

    PROCEDURE ensure_member(
        p_email IN VARCHAR2,
        p_member_name IN VARCHAR2,
        p_nickname IN VARCHAR2,
        p_member_id OUT NUMBER
    ) IS
    BEGIN
        UPDATE MEMBER
           SET MEMBER_NAME = p_member_name,
               NICKNAME = p_nickname,
               PASSWORD_HASH = NULL,
               PRIVACY_CONSENT_YN = 'Y',
               MEMBER_STATUS = 'ACTIVE',
               WITHDRAWN_AT = NULL,
               UPDATED_AT = SYSTIMESTAMP
         WHERE LOWER(EMAIL) = LOWER(p_email);

        IF SQL%ROWCOUNT = 0 THEN
            INSERT INTO MEMBER (
                MEMBER_ID,
                MEMBER_NAME,
                EMAIL,
                NICKNAME,
                PASSWORD_HASH,
                PRIVACY_CONSENT_YN,
                MEMBER_STATUS
            ) VALUES (
                SEQ_MEMBER.NEXTVAL,
                p_member_name,
                LOWER(p_email),
                p_nickname,
                NULL,
                'Y',
                'ACTIVE'
            );
        END IF;

        SELECT MEMBER_ID
          INTO p_member_id
          FROM MEMBER
         WHERE LOWER(EMAIL) = LOWER(p_email);
    END;

    PROCEDURE ensure_plan(
        p_owner_member_id IN NUMBER,
        p_title IN VARCHAR2,
        p_region_code IN VARCHAR2,
        p_start_offset IN NUMBER,
        p_day_count IN NUMBER,
        p_view_count IN NUMBER,
        p_plan_id OUT NUMBER,
        p_start_date OUT DATE
    ) IS
    BEGIN
        SELECT MIN(PLAN_ID)
          INTO p_plan_id
          FROM TRAVEL_PLAN
         WHERE OWNER_MEMBER_ID = p_owner_member_id
           AND TITLE = p_title;

        IF p_plan_id IS NULL THEN
            p_start_date := TRUNC(SYSDATE) + p_start_offset;
            p_plan_id := SEQ_TRAVEL_PLAN.NEXTVAL;

            INSERT INTO TRAVEL_PLAN (
                PLAN_ID,
                OWNER_MEMBER_ID,
                TITLE,
                REGION_CODE,
                START_DATE,
                END_DATE,
                VISIBILITY,
                PLAN_STATUS,
                VIEW_COUNT
            ) VALUES (
                p_plan_id,
                p_owner_member_id,
                p_title,
                p_region_code,
                p_start_date,
                p_start_date + p_day_count - 1,
                'PUBLIC',
                'ACTIVE',
                p_view_count
            );
        ELSE
            SELECT START_DATE
              INTO p_start_date
              FROM TRAVEL_PLAN
             WHERE PLAN_ID = p_plan_id;

            UPDATE TRAVEL_PLAN
               SET VISIBILITY = 'PUBLIC',
                   PLAN_STATUS = 'ACTIVE',
                   DELETED_AT = NULL,
                   DELETED_BY_MEMBER_ID = NULL,
                   VIEW_COUNT = GREATEST(VIEW_COUNT, p_view_count)
             WHERE PLAN_ID = p_plan_id;
        END IF;

        MERGE INTO PLAN_MEMBER target
        USING (
            SELECT p_plan_id PLAN_ID, p_owner_member_id MEMBER_ID FROM DUAL
        ) source
        ON (target.PLAN_ID = source.PLAN_ID AND target.MEMBER_ID = source.MEMBER_ID)
        WHEN NOT MATCHED THEN
            INSERT (PLAN_ID, MEMBER_ID, PARTICIPANT_TYPE)
            VALUES (source.PLAN_ID, source.MEMBER_ID, 'CREATOR');
    END;

    PROCEDURE ensure_day(
        p_plan_id IN NUMBER,
        p_start_date IN DATE,
        p_day_no IN NUMBER,
        p_day_id OUT NUMBER
    ) IS
    BEGIN
        SELECT MIN(PLAN_DAY_ID)
          INTO p_day_id
          FROM PLAN_DAY
         WHERE PLAN_ID = p_plan_id
           AND DAY_NO = p_day_no;

        IF p_day_id IS NULL THEN
            p_day_id := SEQ_PLAN_DAY.NEXTVAL;
            INSERT INTO PLAN_DAY (
                PLAN_DAY_ID,
                PLAN_ID,
                DAY_NO,
                TRAVEL_DATE,
                SCHEDULE_VERSION
            ) VALUES (
                p_day_id,
                p_plan_id,
                p_day_no,
                p_start_date + p_day_no - 1,
                0
            );
        END IF;
    END;

    PROCEDURE ensure_item(
        p_day_id IN NUMBER,
        p_time_slot IN VARCHAR2,
        p_position_no IN NUMBER,
        p_external_place_id IN VARCHAR2,
        p_place_name IN VARCHAR2,
        p_category IN VARCHAR2,
        p_address IN VARCHAR2,
        p_latitude IN VARCHAR2,
        p_longitude IN VARCHAR2,
        p_description IN VARCHAR2
    ) IS
        v_item_id NUMBER(19);
    BEGIN
        SELECT MIN(SCHEDULE_ITEM_ID)
          INTO v_item_id
          FROM PLAN_SCHEDULE_ITEM
         WHERE PLAN_DAY_ID = p_day_id
           AND TIME_SLOT = p_time_slot
           AND PLACE_PROVIDER = 'DEMO'
           AND EXTERNAL_PLACE_ID = p_external_place_id;

        IF v_item_id IS NULL THEN
            INSERT INTO PLAN_SCHEDULE_ITEM (
                SCHEDULE_ITEM_ID,
                PLAN_DAY_ID,
                TIME_SLOT,
                POSITION_NO,
                PLACE_PROVIDER,
                EXTERNAL_PLACE_ID,
                PLACE_NAME_SNAPSHOT,
                CATEGORY_SNAPSHOT,
                ADDRESS_SNAPSHOT,
                LATITUDE_SNAPSHOT,
                LONGITUDE_SNAPSHOT,
                DESCRIPTION_SNAPSHOT,
                ITEM_VERSION
            ) VALUES (
                SEQ_PLAN_SCHEDULE_ITEM.NEXTVAL,
                p_day_id,
                p_time_slot,
                p_position_no,
                'DEMO',
                p_external_place_id,
                p_place_name,
                p_category,
                p_address,
                p_latitude,
                p_longitude,
                p_description,
                0
            );
        END IF;
    END;

    PROCEDURE ensure_like(
        p_plan_id IN NUMBER,
        p_member_id IN NUMBER
    ) IS
    BEGIN
        MERGE INTO PLAN_LIKE target
        USING (
            SELECT p_plan_id PLAN_ID, p_member_id MEMBER_ID FROM DUAL
        ) source
        ON (target.PLAN_ID = source.PLAN_ID AND target.MEMBER_ID = source.MEMBER_ID)
        WHEN NOT MATCHED THEN
            INSERT (PLAN_LIKE_ID, PLAN_ID, MEMBER_ID)
            VALUES (SEQ_PLAN_LIKE.NEXTVAL, source.PLAN_ID, source.MEMBER_ID);
    END;
BEGIN
    assert_target_schema;

    ensure_member('demo.seoul@withtrip.example', '서울봄', '서울산책자', v_seoul_member_id);
    ensure_member('demo.coast@withtrip.example', '부산별', '바다여행자', v_coast_member_id);
    ensure_member('demo.island@withtrip.example', '제주빛', '섬여행자', v_island_member_id);

    ensure_plan(v_seoul_member_id, '서울 궁궐과 골목 산책', '1', 14, 2, 342, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'SEOUL-GYEONGBOK', '경복궁', '역사관광', '서울 종로구 사직로 161', '37.579617', '126.977041', '조선의 대표 궁궐에서 시작하는 서울 역사 산책');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'SEOUL-BUKCHON', '북촌한옥마을', '문화마을', '서울 종로구 계동길 37', '37.582604', '126.983123', '한옥 골목과 작은 공방을 천천히 둘러보는 코스');
    ensure_day(v_plan_id, v_plan_start_date, 2, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'SEOUL-GWANGJANG', '광장시장', '전통시장', '서울 종로구 창경궁로 88', '37.570153', '126.999340', '빈대떡과 육회 등 서울 시장 음식을 맛보는 일정');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'SEOUL-CHEONGGYECHEON', '청계천', '도보여행', '서울 종로구 청계천로', '37.569087', '126.978669', '도심 속 물길을 따라 걷는 가벼운 오후 산책');
    ensure_like(v_plan_id, v_coast_member_id);
    ensure_like(v_plan_id, v_island_member_id);

    ensure_plan(v_coast_member_id, '부산 바다와 야경 여행', '6', 21, 3, 278, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'BUSAN-STATION', '부산역', '교통', '부산 동구 중앙대로 206', '35.115225', '129.041382', '부산 여행의 출발점에서 짐을 맡기고 일정을 시작');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'BUSAN-JAGALCHI', '자갈치시장', '전통시장', '부산 중구 자갈치해안로 52', '35.096705', '129.030453', '싱싱한 해산물과 활기찬 항구 분위기를 즐기는 코스');
    ensure_day(v_plan_id, v_plan_start_date, 2, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'BUSAN-HAEUNDAE', '해운대해수욕장', '해변', '부산 해운대구 해운대해변로 264', '35.158698', '129.160384', '아침 바다를 보며 산책하기 좋은 부산 대표 해변');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'BUSAN-BLUELINE', '해운대 블루라인파크', '관광열차', '부산 해운대구 청사포로 116', '35.161247', '129.191185', '해안선을 따라 달리는 열차에서 부산 바다 감상');
    ensure_day(v_plan_id, v_plan_start_date, 3, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'BUSAN-GWANGAN', '광안리해수욕장', '해변', '부산 수영구 광안해변로 219', '35.153169', '129.118666', '광안대교를 바라보며 여유롭게 보내는 오전');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'BUSAN-THEBAY', '더베이101', '야경', '부산 해운대구 동백로 52', '35.156693', '129.152016', '마리나와 도심 야경을 함께 즐기는 여행 마무리');
    ensure_like(v_plan_id, v_seoul_member_id);
    ensure_like(v_plan_id, v_island_member_id);

    ensure_plan(v_island_member_id, '제주 동쪽 감성 드라이브', '39', 30, 3, 415, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'JEJU-SEONGSAN', '성산일출봉', '자연경관', '제주 서귀포시 성산읍 일출로 284-12', '33.458111', '126.942548', '제주 동쪽을 대표하는 오름에서 시작하는 여행');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'JEJU-SEOPJIKOJI', '섭지코지', '해안산책', '제주 서귀포시 성산읍 섭지코지로 107', '33.423930', '126.930605', '바다와 들판이 이어지는 해안 산책로');
    ensure_day(v_plan_id, v_plan_start_date, 2, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'JEJU-BIJARIM', '비자림', '숲길', '제주 제주시 구좌읍 비자숲길 55', '33.491290', '126.811399', '오래된 비자나무 사이를 걷는 고요한 아침');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'JEJU-WOLJEONG', '월정리해변', '해변', '제주 제주시 구좌읍 월정리', '33.556469', '126.795902', '바다색이 아름다운 해변과 카페 거리');
    ensure_day(v_plan_id, v_plan_start_date, 3, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'JEJU-HAMDEOK', '함덕해수욕장', '해변', '제주 제주시 조천읍 조함해안로 525', '33.543096', '126.669079', '잔잔한 바다를 보며 쉬어가는 마지막 날');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'JEJU-DONGMUN', '동문재래시장', '전통시장', '제주 제주시 관덕로14길 20', '33.511589', '126.526018', '제주 먹거리와 기념품을 만나는 여행 마무리');
    ensure_like(v_plan_id, v_seoul_member_id);
    ensure_like(v_plan_id, v_coast_member_id);

    ensure_plan(v_coast_member_id, '강릉 커피와 바다', '32', 18, 2, 196, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'GANGNEUNG-ANMOK', '안목해변', '해변', '강원 강릉시 창해로 17', '37.771123', '128.947330', '바다를 보며 커피 한 잔으로 시작하는 강릉 여행');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'GANGNEUNG-MARKET', '강릉중앙시장', '전통시장', '강원 강릉시 금성로 21', '37.752030', '128.896285', '닭강정과 지역 먹거리를 즐기는 오후');
    ensure_day(v_plan_id, v_plan_start_date, 2, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'GANGNEUNG-GYEONGPO', '경포호', '호수산책', '강원 강릉시 저동', '37.795568', '128.896611', '호수 둘레길을 따라 걷는 여유로운 아침');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'GANGNEUNG-OJUKHEON', '오죽헌', '역사관광', '강원 강릉시 율곡로3139번길 24', '37.779308', '128.878173', '신사임당과 율곡 이이의 흔적을 만나는 장소');
    ensure_like(v_plan_id, v_seoul_member_id);

    ensure_plan(v_seoul_member_id, '전주 한옥마을 미식 여행', '37', 25, 2, 231, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'JEONJU-HANOK', '전주한옥마을', '문화마을', '전북 전주시 완산구 기린대로 99', '35.815083', '127.153017', '한옥 골목과 전통문화를 천천히 둘러보는 여행');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'JEONJU-GYEONGGIJEON', '경기전', '역사관광', '전북 전주시 완산구 태조로 44', '35.815343', '127.149963', '태조 이성계의 어진을 모신 고즈넉한 공간');
    ensure_day(v_plan_id, v_plan_start_date, 2, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'JEONJU-NAMBU', '전주남부시장', '전통시장', '전북 전주시 완산구 풍남문1길 19-3', '35.811930', '127.147627', '콩나물국밥과 지역 먹거리를 맛보는 아침');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'JEONJU-JAMAN', '자만벽화마을', '문화마을', '전북 전주시 완산구 교동 50-158', '35.814606', '127.158323', '색감 있는 골목과 전망을 즐기는 마지막 일정');
    ensure_like(v_plan_id, v_coast_member_id);

    ensure_plan(v_island_member_id, '수원 화성 하루 여행', '31', 10, 1, 184, v_plan_id, v_plan_start_date);
    ensure_day(v_plan_id, v_plan_start_date, 1, v_day_id);
    ensure_item(v_day_id, 'MORNING', 1, 'SUWON-HWASEONG', '수원 화성', '역사관광', '경기 수원시 장안구 영화동 320-2', '37.287808', '127.012438', '성곽길을 따라 수원의 역사와 풍경을 만나는 코스');
    ensure_item(v_day_id, 'AFTERNOON', 1, 'SUWON-HAENGGUNG', '화성행궁', '궁궐', '경기 수원시 팔달구 정조로 825', '37.281957', '127.013671', '정조의 이야기가 남아 있는 도심 속 행궁');
    ensure_item(v_day_id, 'AFTERNOON', 2, 'SUWON-CHICKEN', '수원 통닭거리', '먹거리', '경기 수원시 팔달구 정조로800번길', '37.278981', '127.017468', '바삭한 통닭으로 하루 여행을 마무리');
    ensure_like(v_plan_id, v_seoul_member_id);
    ensure_like(v_plan_id, v_coast_member_id);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Demo seed completed for WITHTRIP_DEV.');
END;
/

SELECT COUNT(*) AS DEMO_MEMBER_COUNT
  FROM MEMBER
 WHERE EMAIL LIKE 'demo.%@withtrip.example';

SELECT COUNT(*) AS DEMO_PLAN_COUNT
  FROM TRAVEL_PLAN p
  JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
 WHERE m.EMAIL LIKE 'demo.%@withtrip.example';

EXIT SUCCESS;
