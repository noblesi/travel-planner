-- Complete, repeatable fixture for every WITHTRIP_DEV table except REGION_MASTER.
-- This supersedes the legacy external seed_data.sql plus 004_repair_seed_data.sql.
-- Raw test password for all five members: WithTrip-E2E-2026!
-- Raw invitation tokens:
--   900901: withtrip-test-invitation-token-00001
--   900902: withtrip-test-invitation-token-00002
--   900903: withtrip-test-invitation-token-00003
--   900904: withtrip-test-invitation-token-00004
--   900905: withtrip-test-invitation-token-00005

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    c_password_hash CONSTANT VARCHAR2(255) :=
        '$2a$10$vtQ0WJwy8lT3TsxLq/NZ0ucMHmcSxN5hGQiWw28uIe4ehQTtDs2IW';

    TYPE t_text_list IS TABLE OF VARCHAR2(1000) INDEX BY PLS_INTEGER;
    TYPE t_number_list IS TABLE OF NUMBER INDEX BY PLS_INTEGER;

    v_notice_titles t_text_list;
    v_notice_categories t_text_list;
    v_notice_offsets t_number_list;
    v_region_codes t_text_list;
    v_invitation_statuses t_text_list;
    v_report_statuses t_text_list;
    v_report_reasons t_text_list;

    v_count NUMBER;
    v_suffix VARCHAR2(3);
    v_raw_token VARCHAR2(128);
    v_token_hash VARCHAR2(64);
    v_operation_id VARCHAR2(36);
    v_operation_request_hash VARCHAR2(64);
    v_invitation_created_at TIMESTAMP(6) WITH TIME ZONE;
    v_invitation_expires_at TIMESTAMP(6) WITH TIME ZONE;
    v_invitation_responded_at TIMESTAMP(6) WITH TIME ZONE;

    PROCEDURE assert_target_schema IS
    BEGIN
        IF UPPER(SYS_CONTEXT('USERENV', 'SESSION_USER')) <> 'WITHTRIP_DEV'
           OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'WITHTRIP_DEV' THEN
            RAISE_APPLICATION_ERROR(-20001, 'Full test seed is allowed only in WITHTRIP_DEV.');
        END IF;
    END;

    PROCEDURE assert_count(
        p_label IN VARCHAR2,
        p_actual IN NUMBER,
        p_expected IN NUMBER
    ) IS
    BEGIN
        IF p_actual <> p_expected THEN
            RAISE_APPLICATION_ERROR(
                -20002,
                p_label || ' count mismatch: expected ' || p_expected || ', found ' || p_actual
            );
        END IF;
    END;

    FUNCTION utf8_part(p_value IN VARCHAR2) RETURN VARCHAR2 IS
        v_byte_length PLS_INTEGER;
    BEGIN
        v_byte_length := UTL_RAW.LENGTH(UTL_I18N.STRING_TO_RAW(p_value, 'AL32UTF8'));
        RETURN TO_CHAR(v_byte_length, 'FM9999999990') || ':' || p_value || ';';
    END;

    FUNCTION integer_text(p_value IN NUMBER) RETURN VARCHAR2 IS
    BEGIN
        RETURN TO_CHAR(
            p_value,
            'FM9999999999999999990',
            'NLS_NUMERIC_CHARACTERS=''.,'''
        );
    END;

    FUNCTION sha256_text(p_value IN VARCHAR2) RETURN VARCHAR2 IS
        v_hash VARCHAR2(64);
    BEGIN
        SELECT LOWER(
                   RAWTOHEX(
                       STANDARD_HASH(
                           UTL_I18N.STRING_TO_RAW(p_value, 'AL32UTF8'),
                           'SHA256'
                       )
                   )
               )
          INTO v_hash
          FROM DUAL;

        RETURN v_hash;
    END;

    FUNCTION update_request_hash(
        p_item_id IN NUMBER,
        p_schedule_version IN NUMBER,
        p_item_version IN NUMBER,
        p_time_slot IN VARCHAR2
    ) RETURN VARCHAR2 IS
        v_payload VARCHAR2(4000);
    BEGIN
        v_payload := utf8_part(integer_text(p_item_id))
                  || utf8_part(integer_text(p_schedule_version))
                  || utf8_part(integer_text(p_item_version))
                  || utf8_part(p_time_slot);

        RETURN sha256_text(v_payload);
    END;
BEGIN
    assert_target_schema;

    v_region_codes(1) := '1';
    v_region_codes(2) := '6';
    v_region_codes(3) := '31';
    v_region_codes(4) := '32';
    v_region_codes(5) := '39';

    v_invitation_statuses(1) := 'PENDING';
    v_invitation_statuses(2) := 'ACCEPTED';
    v_invitation_statuses(3) := 'DECLINED';
    v_invitation_statuses(4) := 'EXPIRED';
    v_invitation_statuses(5) := 'CANCELED';

    v_report_statuses(1) := 'PENDING';
    v_report_statuses(2) := 'IN_PROGRESS';
    v_report_statuses(3) := 'RESOLVED';
    v_report_statuses(4) := 'REJECTED';
    v_report_statuses(5) := 'RESOLVED';

    v_report_reasons(1) := 'SPAM';
    v_report_reasons(2) := 'INAPPROPRIATE';
    v_report_reasons(3) := 'COPYRIGHT';
    v_report_reasons(4) := 'SPAM';
    v_report_reasons(5) := 'INAPPROPRIATE';

    v_notice_titles(1) := '카카오맵 기반 장소 검색 기능 오픈 안내';
    v_notice_titles(2) := '서버 정기 점검 안내 (매주 화요일 새벽)';
    v_notice_titles(3) := '여행 플랜 공유 링크 기능 추가 안내';
    v_notice_titles(4) := '개인정보 처리방침 개정 안내';
    v_notice_titles(5) := '데이터베이스 마이그레이션에 따른 서비스 임시 중단 안내';
    v_notice_titles(6) := '여행 플랜 초대 기능 오픈 안내';
    v_notice_titles(7) := '신규 회원 대상 웰컴 혜택 안내';
    v_notice_titles(8) := '결제 시스템 연동 점검 안내';
    v_notice_titles(9) := '모바일 웹 UI 개선 안내';
    v_notice_titles(10) := '인기 여행지 추천 기능 업데이트 안내';
    v_notice_titles(11) := '네트워크 장비 교체 작업에 따른 접속 지연 안내';
    v_notice_titles(12) := '여행 플랜 복사 기능 오픈 안내';
    v_notice_titles(13) := '서비스 이용약관 변경 사전 안내';
    v_notice_titles(14) := '긴급 보안 패치 적용 안내';
    v_notice_titles(15) := '겨울 여행 시즌 기획전 오픈 안내';
    v_notice_titles(16) := '관광정보 API 연동 지역 확대 안내';
    v_notice_titles(17) := '정기 백업 작업으로 인한 야간 접속 제한 안내';
    v_notice_titles(18) := '회원 등급별 혜택 개편 안내';
    v_notice_titles(19) := '여행 후기 게시판 오픈 예정 안내';
    v_notice_titles(20) := '클라우드 인프라 이전 작업 안내';
    v_notice_titles(21) := '고객센터 운영시간 변경 안내';
    v_notice_titles(22) := '앱 푸시 알림 설정 기능 추가 안내';
    v_notice_titles(23) := '(내부 검토용) 임시 공지 초안';

    FOR i IN 1..23 LOOP
        v_notice_categories(i) := CASE
            WHEN i IN (2, 5, 8, 11, 14, 17, 20) THEN 'MAINTENANCE'
            ELSE 'GUIDE'
        END;
        v_notice_offsets(i) := CASE i
            WHEN 1 THEN 2 WHEN 2 THEN 5 WHEN 3 THEN 7 WHEN 4 THEN 10
            WHEN 5 THEN 12 WHEN 6 THEN 15 WHEN 7 THEN 18 WHEN 8 THEN 20
            WHEN 9 THEN 22 WHEN 10 THEN 25 WHEN 11 THEN 28 WHEN 12 THEN 30
            WHEN 13 THEN 33 WHEN 14 THEN 35 WHEN 15 THEN 38 WHEN 16 THEN 40
            WHEN 17 THEN 42 WHEN 18 THEN 45 WHEN 19 THEN 48 WHEN 20 THEN 50
            WHEN 21 THEN 53 WHEN 22 THEN 55 ELSE 1
        END;
    END LOOP;

    SELECT COUNT(*) INTO v_count
      FROM REGION_MASTER
     WHERE REGION_CODE IN ('1', '6', '31', '32', '39')
       AND ACTIVE_YN = 'Y';
    assert_count('required REGION_MASTER', v_count, 5);

    /* Cleanup: reserved test identities and everything they own, in FK reverse order. */
    DELETE FROM NOTIFICATION
     WHERE NOTIFICATION_ID BETWEEN 901201 AND 901205
        OR RECIPIENT_MEMBER_ID BETWEEN 900201 AND 900205
        OR RELATED_REPORT_ID IN (
               SELECT report.REPORT_ID
                 FROM REPORT report
                WHERE report.REPORT_ID BETWEEN 901101 AND 901105
                   OR report.REPORTER_MEMBER_ID BETWEEN 900201 AND 900205
                   OR report.PLAN_ID IN (
                          SELECT plan.PLAN_ID
                            FROM TRAVEL_PLAN plan
                           WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                              OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
                      )
           );

    DELETE FROM REPORT_PROCESS
     WHERE REPORT_ID BETWEEN 901101 AND 901105
        OR PROCESSOR_ADMIN_ID BETWEEN 900101 AND 900105
        OR REPORT_ID IN (
               SELECT report.REPORT_ID
                 FROM REPORT report
                WHERE report.REPORTER_MEMBER_ID BETWEEN 900201 AND 900205
                   OR report.PLAN_ID IN (
                          SELECT plan.PLAN_ID
                            FROM TRAVEL_PLAN plan
                           WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                              OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
                      )
           );

    DELETE FROM REPORT
     WHERE REPORT_ID BETWEEN 901101 AND 901105
        OR REPORTER_MEMBER_ID BETWEEN 900201 AND 900205
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM RECENTLY_VIEWED_PLAN
     WHERE MEMBER_ID BETWEEN 900201 AND 900205
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM PLAN_LIKE
     WHERE PLAN_LIKE_ID BETWEEN 901001 AND 901005
        OR MEMBER_ID BETWEEN 900201 AND 900205
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM PLAN_INVITATION
     WHERE INVITATION_ID BETWEEN 900901 AND 900905
        OR INVITER_MEMBER_ID BETWEEN 900201 AND 900205
        OR INVITEE_MEMBER_ID BETWEEN 900201 AND 900205
        OR INVITEE_EMAIL LIKE 'test.member%@withtrip.test'
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM PLAN_EDIT_OPERATION
     WHERE OPERATION_ID IN (
               'TEST-OPERATION-00001', 'TEST-OPERATION-00002',
               'TEST-OPERATION-00003', 'TEST-OPERATION-00004',
               'TEST-OPERATION-00005', 'test-operation-00001',
               'test-operation-00002', 'test-operation-00003',
               'test-operation-00004', 'test-operation-00005'
           )
        OR MEMBER_ID BETWEEN 900201 AND 900205
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM PLAN_SCHEDULE_ITEM
     WHERE SCHEDULE_ITEM_ID BETWEEN 900801 AND 900805
        OR PLAN_DAY_ID IN (
               SELECT day.PLAN_DAY_ID
                 FROM PLAN_DAY day
                WHERE day.PLAN_DAY_ID BETWEEN 900701 AND 900705
                   OR day.PLAN_ID IN (
                          SELECT plan.PLAN_ID
                            FROM TRAVEL_PLAN plan
                           WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                              OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
                      )
           );

    DELETE FROM PLAN_DAY
     WHERE PLAN_DAY_ID BETWEEN 900701 AND 900705
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM PLAN_MEMBER
     WHERE MEMBER_ID BETWEEN 900201 AND 900205
        OR PLAN_ID IN (
               SELECT plan.PLAN_ID
                 FROM TRAVEL_PLAN plan
                WHERE plan.PLAN_ID BETWEEN 900601 AND 900605
                   OR plan.OWNER_MEMBER_ID BETWEEN 900201 AND 900205
           );

    DELETE FROM TRAVEL_PLAN
     WHERE PLAN_ID BETWEEN 900601 AND 900605
        OR OWNER_MEMBER_ID BETWEEN 900201 AND 900205;

    DELETE FROM RECOMMEND_RULE
     WHERE RULE_ID BETWEEN 900401 AND 900405
        OR ADMIN_ID BETWEEN 900101 AND 900105;

    DELETE FROM TOUR_SYNC_HISTORY
     WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505
        OR ADMIN_ID BETWEEN 900101 AND 900105;

    DELETE FROM PLACE_MASTER
     WHERE PLACE_PROVIDER = 'TEST';

    DELETE FROM NOTICE
     WHERE NOTICE_ID BETWEEN 900301 AND 900323
        OR ADMIN_ID BETWEEN 900101 AND 900105;

    DELETE FROM MEMBER_WARNING_SUPPRESSION
     WHERE MEMBER_ID BETWEEN 900201 AND 900205;

    DELETE FROM MEMBER
     WHERE MEMBER_ID BETWEEN 900201 AND 900205
        OR EMAIL LIKE 'test.member%@withtrip.test';

    DELETE FROM ADMIN
     WHERE ADMIN_ID BETWEEN 900101 AND 900105
        OR LOGIN_ID LIKE 'test_admin%'
        OR EMAIL LIKE 'test.admin%@withtrip.test';

    /* Parent identities. */
    FOR i IN 1..5 LOOP
        v_suffix := LPAD(i, 2, '0');

        INSERT INTO ADMIN (
            ADMIN_ID,
            LOGIN_ID,
            ADMIN_NAME,
            PASSWORD_HASH,
            EMAIL,
            ADMIN_ROLE_CODE,
            ADMIN_STATUS
        ) VALUES (
            900100 + i,
            'test_admin' || v_suffix,
            '관리자' || i,
            c_password_hash,
            'test.admin' || v_suffix || '@withtrip.test',
            'CONTENT',
            'ACTIVE'
        );

        INSERT INTO MEMBER (
            MEMBER_ID,
            MEMBER_NAME,
            EMAIL,
            NICKNAME,
            PASSWORD_HASH,
            PRIVACY_CONSENT_YN,
            MEMBER_STATUS
        ) VALUES (
            900200 + i,
            '테스트' || i,
            'test.member' || v_suffix || '@withtrip.test',
            '테스트멤버' || i,
            c_password_hash,
            'Y',
            'ACTIVE'
        );

        INSERT INTO MEMBER_WARNING_SUPPRESSION (
            MEMBER_ID,
            WARNING_TYPE,
            SUPPRESSED_DATE
        ) VALUES (
            900200 + i,
            CASE WHEN MOD(i, 2) = 1
                THEN 'PERIOD_SHORTEN_DELETE'
                ELSE 'SCHEDULE_ITEM_DELETE'
            END,
            TRUNC(SYSDATE)
        );
    END LOOP;

    /* Notices. */
    FOR i IN 1..23 LOOP
        INSERT INTO NOTICE (
            NOTICE_ID,
            ADMIN_ID,
            TITLE,
            CONTENT,
            CATEGORY_CODE,
            HIDDEN_YN,
            CREATED_AT
        ) VALUES (
            900300 + i,
            900100 + MOD(i - 1, 5) + 1,
            v_notice_titles(i),
            CASE WHEN i = 23
                THEN '숨김 처리 동작 확인용 테스트 공지입니다. 사용자 화면에는 노출되지 않아야 합니다.'
                ELSE v_notice_titles(i) || ' 관련 기능과 일정을 확인하기 위한 테스트 공지입니다.'
            END,
            v_notice_categories(i),
            CASE WHEN i = 23 THEN 'Y' ELSE 'N' END,
            SYSTIMESTAMP - NUMTODSINTERVAL(v_notice_offsets(i), 'DAY')
        );
    END LOOP;

    /* Places, recommendation rules and sync histories. */
    FOR i IN 1..5 LOOP
        v_suffix := LPAD(i, 3, '0');

        INSERT INTO PLACE_MASTER (
            PLACE_PROVIDER,
            EXTERNAL_PLACE_ID,
            PLACE_TYPE,
            PLACE_NAME,
            CATEGORY_NAME,
            ADDRESS,
            LATITUDE,
            LONGITUDE,
            ACTIVE_YN
        ) VALUES (
            'TEST',
            'PLACE' || v_suffix,
            'ATTRACTION',
            '테스트 장소 ' || i,
            '테스트카테고리',
            '테스트 주소 ' || i,
            35 + (i / 10),
            128 + (i / 10),
            'Y'
        );

        INSERT INTO RECOMMEND_RULE (
            RULE_ID,
            LIKE_WEIGHT,
            VIEW_WEIGHT,
            COPY_WEIGHT,
            ACTIVE_YN,
            ADMIN_ID
        ) VALUES (
            900400 + i,
            i / 10,
            i / 5,
            i / 20,
            'Y',
            900100 + i
        );

        INSERT INTO TOUR_SYNC_HISTORY (
            SYNC_HISTORY_ID,
            ADMIN_ID,
            PROVIDER_CODE,
            STARTED_AT,
            FINISHED_AT,
            TOTAL_COUNT,
            INSERT_COUNT,
            UPDATE_COUNT,
            SUCCESS_COUNT,
            FAIL_COUNT,
            PROCESS_STATUS
        ) VALUES (
            900500 + i,
            900100 + i,
            'TEST-PROVIDER-' || i,
            SYSTIMESTAMP - NUMTODSINTERVAL(1, 'DAY'),
            SYSTIMESTAMP,
            100 * i,
            10 * i,
            5 * i,
            90 * i,
            10 * i,
            'COMPLETED'
        );
    END LOOP;

    /* Plans, owners, days and schedule items. */
    FOR i IN 1..5 LOOP
        v_suffix := LPAD(i, 3, '0');

        INSERT INTO TRAVEL_PLAN (
            PLAN_ID,
            OWNER_MEMBER_ID,
            TITLE,
            REGION_CODE,
            START_DATE,
            END_DATE,
            VISIBILITY,
            PUBLISH_STATUS,
            PLAN_STATUS,
            VIEW_COUNT
        ) VALUES (
            900600 + i,
            900200 + i,
            '[TEST] 여행 플랜 ' || i,
            v_region_codes(i),
            TRUNC(SYSDATE) + 30 + i,
            TRUNC(SYSDATE) + 31 + i,
            'PUBLIC',
            'PUBLISHED',
            'ACTIVE',
            10 * i
        );

        INSERT INTO PLAN_MEMBER (
            PLAN_ID,
            MEMBER_ID,
            PARTICIPANT_TYPE
        ) VALUES (
            900600 + i,
            900200 + i,
            'CREATOR'
        );

        INSERT INTO PLAN_DAY (
            PLAN_DAY_ID,
            PLAN_ID,
            DAY_NO,
            TRAVEL_DATE,
            SCHEDULE_VERSION
        ) VALUES (
            900700 + i,
            900600 + i,
            1,
            TRUNC(SYSDATE) + 30 + i,
            0
        );

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
            ITEM_VERSION
        ) VALUES (
            900800 + i,
            900700 + i,
            'MORNING',
            1,
            'TEST',
            'PLACE' || v_suffix,
            '테스트 장소 ' || i,
            '테스트카테고리',
            '테스트 주소 ' || i,
            TO_CHAR(35 + (i / 10), 'FM990D0', 'NLS_NUMERIC_CHARACTERS=''.,'''),
            TO_CHAR(128 + (i / 10), 'FM990D0', 'NLS_NUMERIC_CHARACTERS=''.,'''),
            0
        );

        v_operation_id := 'test-operation-' || LPAD(i, 5, '0');
        v_operation_request_hash := update_request_hash(900800 + i, 0, 0, 'MORNING');

        INSERT INTO PLAN_EDIT_OPERATION (
            OPERATION_ID,
            PLAN_ID,
            MEMBER_ID,
            OPERATION_TYPE,
            TARGET_ITEM_ID,
            BASE_VERSION,
            RESULT_VERSION,
            REQUEST_HASH
        ) VALUES (
            v_operation_id,
            900600 + i,
            900200 + i,
            'UPDATE',
            900800 + i,
            0,
            0,
            v_operation_request_hash
        );
    END LOOP;

    /* Invitations, including the membership created by an accepted invitation. */
    FOR i IN 1..5 LOOP
        v_raw_token := 'withtrip-test-invitation-token-' || LPAD(i, 5, '0');
        v_token_hash := sha256_text(v_raw_token);

        IF v_invitation_statuses(i) = 'EXPIRED' THEN
            v_invitation_created_at := SYSTIMESTAMP - NUMTODSINTERVAL(2, 'DAY');
            v_invitation_expires_at := SYSTIMESTAMP - NUMTODSINTERVAL(1, 'DAY');
            v_invitation_responded_at := NULL;
        ELSE
            v_invitation_created_at := SYSTIMESTAMP;
            v_invitation_expires_at := SYSTIMESTAMP + NUMTODSINTERVAL(7, 'DAY');
            v_invitation_responded_at := CASE
                WHEN v_invitation_statuses(i) IN ('ACCEPTED', 'DECLINED', 'CANCELED')
                    THEN SYSTIMESTAMP
                ELSE NULL
            END;
        END IF;

        INSERT INTO PLAN_INVITATION (
            INVITATION_ID,
            PLAN_ID,
            INVITER_MEMBER_ID,
            INVITEE_MEMBER_ID,
            INVITEE_EMAIL,
            INVITATION_STATUS,
            INVITATION_TOKEN,
            EXPIRES_AT,
            RESPONDED_AT,
            CREATED_AT
        ) VALUES (
            900900 + i,
            900600 + i,
            900200 + i,
            900200 + MOD(i, 5) + 1,
            'test.member' || LPAD(MOD(i, 5) + 1, 2, '0') || '@withtrip.test',
            v_invitation_statuses(i),
            v_token_hash,
            v_invitation_expires_at,
            v_invitation_responded_at,
            v_invitation_created_at
        );
    END LOOP;

    INSERT INTO PLAN_MEMBER (
        PLAN_ID,
        MEMBER_ID,
        PARTICIPANT_TYPE
    ) VALUES (
        900602,
        900203,
        'INVITEE'
    );

    /* Likes and recently viewed plans. */
    FOR i IN 1..5 LOOP
        INSERT INTO PLAN_LIKE (
            PLAN_LIKE_ID,
            PLAN_ID,
            MEMBER_ID
        ) VALUES (
            901000 + i,
            900600 + i,
            900200 + MOD(i, 5) + 1
        );

        INSERT INTO RECENTLY_VIEWED_PLAN (
            PLAN_ID,
            MEMBER_ID
        ) VALUES (
            900600 + i,
            900200 + MOD(i, 5) + 1
        );
    END LOOP;

    /* Reports preserve status variety. Only terminal reports have process rows and result notifications. */
    FOR i IN 1..5 LOOP
        INSERT INTO REPORT (
            REPORT_ID,
            PLAN_ID,
            REPORTER_MEMBER_ID,
            REASON_CODE,
            REASON_DETAIL,
            REPORT_STATUS
        ) VALUES (
            901100 + i,
            900600 + i,
            900200 + MOD(i, 5) + 1,
            v_report_reasons(i),
            '테스트 신고 ' || i,
            v_report_statuses(i)
        );

        IF v_report_statuses(i) IN ('RESOLVED', 'REJECTED') THEN
            INSERT INTO REPORT_PROCESS (
                REPORT_ID,
                PROCESSOR_ADMIN_ID,
                PROCESS_RESULT_CODE,
                PROCESS_REASON
            ) VALUES (
                901100 + i,
                900100 + i,
                v_report_statuses(i),
                '테스트 처리 완료 ' || i
            );

            INSERT INTO NOTIFICATION (
                NOTIFICATION_ID,
                RECIPIENT_MEMBER_ID,
                NOTIFICATION_TYPE,
                TITLE,
                CONTENT,
                RELATED_REPORT_ID,
                READ_YN
            ) VALUES (
                901200 + i,
                900200 + MOD(i, 5) + 1,
                'REPORT_RESULT',
                '[TEST] 알림 ' || i,
                '테스트용 알림 본문 ' || i,
                901100 + i,
                CASE WHEN i = 5 THEN 'N' ELSE 'Y' END
            );
        END IF;
    END LOOP;

    /* Final assertions. No commit occurs before every check passes. */
    SELECT COUNT(*) INTO v_count FROM ADMIN WHERE ADMIN_ID BETWEEN 900101 AND 900105;
    assert_count('ADMIN', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM MEMBER WHERE MEMBER_ID BETWEEN 900201 AND 900205;
    assert_count('MEMBER', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM MEMBER_WARNING_SUPPRESSION
     WHERE MEMBER_ID BETWEEN 900201 AND 900205;
    assert_count('MEMBER_WARNING_SUPPRESSION', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM NOTICE WHERE NOTICE_ID BETWEEN 900301 AND 900323;
    assert_count('NOTICE', v_count, 23);

    SELECT COUNT(*) INTO v_count
      FROM PLACE_MASTER
     WHERE PLACE_PROVIDER = 'TEST'
       AND EXTERNAL_PLACE_ID IN ('PLACE001', 'PLACE002', 'PLACE003', 'PLACE004', 'PLACE005');
    assert_count('PLACE_MASTER', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM RECOMMEND_RULE WHERE RULE_ID BETWEEN 900401 AND 900405;
    assert_count('RECOMMEND_RULE', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM TOUR_SYNC_HISTORY WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505;
    assert_count('TOUR_SYNC_HISTORY', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM TRAVEL_PLAN WHERE PLAN_ID BETWEEN 900601 AND 900605;
    assert_count('TRAVEL_PLAN', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM PLAN_MEMBER WHERE PLAN_ID BETWEEN 900601 AND 900605;
    assert_count('PLAN_MEMBER', v_count, 6);

    SELECT COUNT(*) INTO v_count FROM PLAN_DAY WHERE PLAN_DAY_ID BETWEEN 900701 AND 900705;
    assert_count('PLAN_DAY', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_SCHEDULE_ITEM
     WHERE SCHEDULE_ITEM_ID BETWEEN 900801 AND 900805;
    assert_count('PLAN_SCHEDULE_ITEM', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_INVITATION
     WHERE INVITATION_ID BETWEEN 900901 AND 900905
       AND LENGTH(INVITATION_TOKEN) = 64;
    assert_count('hashed PLAN_INVITATION', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_MEMBER
     WHERE PLAN_ID = 900602
       AND MEMBER_ID = 900203
       AND PARTICIPANT_TYPE = 'INVITEE';
    assert_count('accepted PLAN_MEMBER', v_count, 1);

    SELECT COUNT(*) INTO v_count FROM PLAN_LIKE WHERE PLAN_LIKE_ID BETWEEN 901001 AND 901005;
    assert_count('PLAN_LIKE', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM RECENTLY_VIEWED_PLAN
     WHERE PLAN_ID BETWEEN 900601 AND 900605;
    assert_count('RECENTLY_VIEWED_PLAN', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM REPORT WHERE REPORT_ID BETWEEN 901101 AND 901105;
    assert_count('REPORT', v_count, 5);

    SELECT COUNT(*) INTO v_count FROM REPORT_PROCESS WHERE REPORT_ID BETWEEN 901101 AND 901105;
    assert_count('REPORT_PROCESS', v_count, 3);

    SELECT COUNT(*) INTO v_count FROM NOTIFICATION WHERE RELATED_REPORT_ID BETWEEN 901101 AND 901105;
    assert_count('NOTIFICATION', v_count, 3);

    SELECT COUNT(*) INTO v_count
      FROM TOUR_SYNC_HISTORY
     WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505
       AND TOTAL_COUNT = SUCCESS_COUNT + FAIL_COUNT;
    assert_count('consistent TOUR_SYNC_HISTORY', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_EDIT_OPERATION
     WHERE OPERATION_ID BETWEEN 'test-operation-00001' AND 'test-operation-00005'
       AND TARGET_ITEM_ID IS NOT NULL
       AND BASE_VERSION = RESULT_VERSION
       AND LENGTH(REQUEST_HASH) = 64;
    assert_count('replayable PLAN_EDIT_OPERATION', v_count, 5);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Full test seed completed for WITHTRIP_DEV.');
    DBMS_OUTPUT.PUT_LINE('Members: 5, plans: 5, notices: 23, reports: 5.');
    DBMS_OUTPUT.PUT_LINE('Pending invitation token: withtrip-test-invitation-token-00001');
    DBMS_OUTPUT.PUT_LINE('Accepted invitation token: withtrip-test-invitation-token-00002');
END;
/

SELECT COUNT(*) AS TEST_ADMIN_COUNT
  FROM ADMIN
 WHERE ADMIN_ID BETWEEN 900101 AND 900105;

SELECT COUNT(*) AS TEST_MEMBER_COUNT
  FROM MEMBER
 WHERE MEMBER_ID BETWEEN 900201 AND 900205;

SELECT COUNT(*) AS TEST_PLAN_COUNT
  FROM TRAVEL_PLAN
 WHERE PLAN_ID BETWEEN 900601 AND 900605;

SELECT COUNT(*) AS TEST_NOTICE_COUNT
  FROM NOTICE
 WHERE NOTICE_ID BETWEEN 900301 AND 900323;

SELECT COUNT(*) AS TEST_REPORT_COUNT
  FROM REPORT
 WHERE REPORT_ID BETWEEN 901101 AND 901105;

SELECT COUNT(*) AS TEST_REPORT_PROCESS_COUNT
  FROM REPORT_PROCESS
 WHERE REPORT_ID BETWEEN 901101 AND 901105;

SELECT COUNT(*) AS TEST_NOTIFICATION_COUNT
  FROM NOTIFICATION
 WHERE RELATED_REPORT_ID BETWEEN 901101 AND 901105;

EXIT SUCCESS;
