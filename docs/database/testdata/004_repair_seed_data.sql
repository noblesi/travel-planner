-- Repairs the legacy seed_data.sql rows that use reserved 900xxx IDs.
-- Run only after the original seed_data.sql completed successfully.
-- Raw invitation tokens used by this fixture:
--   900901: withtrip-test-invitation-token-00001
--   900902: withtrip-test-invitation-token-00002
--   900903: withtrip-test-invitation-token-00003
--   900904: withtrip-test-invitation-token-00004
--   900905: withtrip-test-invitation-token-00005

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    c_token_1 CONSTANT VARCHAR2(128) := 'withtrip-test-invitation-token-00001';
    c_token_2 CONSTANT VARCHAR2(128) := 'withtrip-test-invitation-token-00002';
    c_token_3 CONSTANT VARCHAR2(128) := 'withtrip-test-invitation-token-00003';
    c_token_4 CONSTANT VARCHAR2(128) := 'withtrip-test-invitation-token-00004';
    c_token_5 CONSTANT VARCHAR2(128) := 'withtrip-test-invitation-token-00005';
    v_count NUMBER;
    v_token_hash_1 VARCHAR2(64);
    v_token_hash_2 VARCHAR2(64);
    v_token_hash_3 VARCHAR2(64);
    v_token_hash_4 VARCHAR2(64);
    v_token_hash_5 VARCHAR2(64);
    v_operation_id VARCHAR2(36);
    v_operation_request_hash VARCHAR2(64);

    PROCEDURE assert_target_schema IS
    BEGIN
        IF UPPER(SYS_CONTEXT('USERENV', 'SESSION_USER')) <> 'WITHTRIP_DEV'
           OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'WITHTRIP_DEV' THEN
            RAISE_APPLICATION_ERROR(-20001, 'Seed repair is allowed only in WITHTRIP_DEV.');
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

    FUNCTION update_request_hash(
        p_item_id IN NUMBER,
        p_schedule_version IN NUMBER,
        p_item_version IN NUMBER,
        p_time_slot IN VARCHAR2
    ) RETURN VARCHAR2 IS
        v_payload VARCHAR2(4000);
        v_hash VARCHAR2(64);
    BEGIN
        v_payload := utf8_part(integer_text(p_item_id))
                  || utf8_part(integer_text(p_schedule_version))
                  || utf8_part(integer_text(p_item_version))
                  || utf8_part(p_time_slot);

        SELECT LOWER(
                   RAWTOHEX(
                       STANDARD_HASH(
                           UTL_I18N.STRING_TO_RAW(v_payload, 'AL32UTF8'),
                           'SHA256'
                       )
                   )
               )
          INTO v_hash
          FROM DUAL;

        RETURN v_hash;
    END;

    FUNCTION token_hash(p_raw_token IN VARCHAR2) RETURN VARCHAR2 IS
        v_hash VARCHAR2(64);
    BEGIN
        SELECT LOWER(
                   RAWTOHEX(
                       STANDARD_HASH(
                           UTL_I18N.STRING_TO_RAW(p_raw_token, 'AL32UTF8'),
                           'SHA256'
                       )
                   )
               )
          INTO v_hash
          FROM DUAL;

        RETURN v_hash;
    END;

BEGIN
    assert_target_schema;

    v_token_hash_1 := token_hash(c_token_1);
    v_token_hash_2 := token_hash(c_token_2);
    v_token_hash_3 := token_hash(c_token_3);
    v_token_hash_4 := token_hash(c_token_4);
    v_token_hash_5 := token_hash(c_token_5);

    SELECT COUNT(*) INTO v_count
      FROM ADMIN
     WHERE ADMIN_ID BETWEEN 900101 AND 900105;
    assert_count('ADMIN', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM MEMBER
     WHERE MEMBER_ID BETWEEN 900201 AND 900205;
    assert_count('MEMBER', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM TRAVEL_PLAN
     WHERE PLAN_ID BETWEEN 900601 AND 900605;
    assert_count('TRAVEL_PLAN', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_DAY
     WHERE PLAN_DAY_ID BETWEEN 900701 AND 900705;
    assert_count('PLAN_DAY', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_SCHEDULE_ITEM
     WHERE SCHEDULE_ITEM_ID BETWEEN 900801 AND 900805;
    assert_count('PLAN_SCHEDULE_ITEM', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM PLAN_INVITATION
     WHERE INVITATION_ID BETWEEN 900901 AND 900905;
    assert_count('PLAN_INVITATION', v_count, 5);

    SELECT COUNT(*) INTO v_count
      FROM REPORT
     WHERE REPORT_ID BETWEEN 901101 AND 901105;
    assert_count('REPORT', v_count, 5);

    /* Store invitation-token hashes exactly as the application does. */
    UPDATE PLAN_INVITATION
       SET INVITATION_TOKEN = CASE INVITATION_ID
               WHEN 900901 THEN v_token_hash_1
               WHEN 900902 THEN v_token_hash_2
               WHEN 900903 THEN v_token_hash_3
               WHEN 900904 THEN v_token_hash_4
               WHEN 900905 THEN v_token_hash_5
           END,
           EXPIRES_AT = CASE
               WHEN INVITATION_ID = 900904
                   THEN CREATED_AT + NUMTODSINTERVAL(1, 'SECOND')
               ELSE SYSTIMESTAMP + NUMTODSINTERVAL(7, 'DAY')
           END,
           RESPONDED_AT = CASE
               WHEN INVITATION_STATUS IN ('ACCEPTED', 'DECLINED', 'CANCELED')
                   THEN COALESCE(RESPONDED_AT, SYSTIMESTAMP)
               ELSE NULL
           END
     WHERE INVITATION_ID BETWEEN 900901 AND 900905;

    /* An accepted invitation must grant plan access to the invitee. */
    MERGE INTO PLAN_MEMBER target
    USING (
        SELECT 900602 PLAN_ID, 900203 MEMBER_ID FROM DUAL
    ) source
    ON (
        target.PLAN_ID = source.PLAN_ID
        AND target.MEMBER_ID = source.MEMBER_ID
    )
    WHEN MATCHED THEN
        UPDATE SET target.PARTICIPANT_TYPE = 'INVITEE'
    WHEN NOT MATCHED THEN
        INSERT (PLAN_ID, MEMBER_ID, PARTICIPANT_TYPE)
        VALUES (source.PLAN_ID, source.MEMBER_ID, 'INVITEE');

    /* Completed processing rows and result notifications belong only to terminal reports. */
    DELETE FROM NOTIFICATION
     WHERE RELATED_REPORT_ID IN (
               SELECT REPORT_ID
                 FROM REPORT
                WHERE REPORT_ID BETWEEN 901101 AND 901105
                  AND REPORT_STATUS NOT IN ('RESOLVED', 'REJECTED')
           );

    DELETE FROM REPORT_PROCESS
     WHERE REPORT_ID IN (
               SELECT REPORT_ID
                 FROM REPORT
                WHERE REPORT_ID BETWEEN 901101 AND 901105
                  AND REPORT_STATUS NOT IN ('RESOLVED', 'REJECTED')
           );

    UPDATE REPORT_PROCESS process
       SET PROCESS_RESULT_CODE = (
               SELECT report.REPORT_STATUS
                 FROM REPORT report
                WHERE report.REPORT_ID = process.REPORT_ID
           )
     WHERE process.REPORT_ID IN (
               SELECT REPORT_ID
                 FROM REPORT
                WHERE REPORT_ID BETWEEN 901101 AND 901105
                  AND REPORT_STATUS IN ('RESOLVED', 'REJECTED')
           );

    UPDATE NOTIFICATION notification
       SET RECIPIENT_MEMBER_ID = (
               SELECT report.REPORTER_MEMBER_ID
                 FROM REPORT report
                WHERE report.REPORT_ID = notification.RELATED_REPORT_ID
           )
     WHERE notification.RELATED_REPORT_ID IN (
               SELECT REPORT_ID
                 FROM REPORT
                WHERE REPORT_ID BETWEEN 901101 AND 901105
                  AND REPORT_STATUS IN ('RESOLVED', 'REJECTED')
           );

    /* Every completed sync row must satisfy TOTAL_COUNT = SUCCESS_COUNT + FAIL_COUNT. */
    UPDATE TOUR_SYNC_HISTORY
       SET FAIL_COUNT = TOTAL_COUNT - SUCCESS_COUNT
     WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505
       AND PROCESS_STATUS = 'COMPLETED'
       AND SUCCESS_COUNT BETWEEN 0 AND TOTAL_COUNT;

    SELECT COUNT(*) INTO v_count
      FROM TOUR_SYNC_HISTORY
     WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505
       AND TOTAL_COUNT = SUCCESS_COUNT + FAIL_COUNT;
    assert_count('consistent TOUR_SYNC_HISTORY', v_count, 5);

    /* Replace inert legacy operations with valid, replayable no-op UPDATE ledger rows. */
    DELETE FROM PLAN_EDIT_OPERATION
     WHERE OPERATION_ID IN (
               'TEST-OPERATION-00001', 'TEST-OPERATION-00002',
               'TEST-OPERATION-00003', 'TEST-OPERATION-00004',
               'TEST-OPERATION-00005', 'test-operation-00001',
               'test-operation-00002', 'test-operation-00003',
               'test-operation-00004', 'test-operation-00005'
           );

    FOR item_row IN (
        SELECT item.SCHEDULE_ITEM_ID,
               item.ITEM_VERSION,
               item.TIME_SLOT,
               day.PLAN_ID,
               day.SCHEDULE_VERSION,
               plan.OWNER_MEMBER_ID
          FROM PLAN_SCHEDULE_ITEM item
          JOIN PLAN_DAY day
            ON day.PLAN_DAY_ID = item.PLAN_DAY_ID
          JOIN TRAVEL_PLAN plan
            ON plan.PLAN_ID = day.PLAN_ID
         WHERE item.SCHEDULE_ITEM_ID BETWEEN 900801 AND 900805
         ORDER BY item.SCHEDULE_ITEM_ID
    ) LOOP
        v_operation_id := 'test-operation-'
                       || TO_CHAR(item_row.SCHEDULE_ITEM_ID - 900800, 'FM00000');
        v_operation_request_hash := update_request_hash(
            item_row.SCHEDULE_ITEM_ID,
            item_row.SCHEDULE_VERSION,
            item_row.ITEM_VERSION,
            item_row.TIME_SLOT
        );

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
            item_row.PLAN_ID,
            item_row.OWNER_MEMBER_ID,
            'UPDATE',
            item_row.SCHEDULE_ITEM_ID,
            item_row.SCHEDULE_VERSION,
            item_row.SCHEDULE_VERSION,
            v_operation_request_hash
        );
    END LOOP;

    SELECT COUNT(*) INTO v_count
      FROM PLAN_EDIT_OPERATION
     WHERE OPERATION_ID BETWEEN 'test-operation-00001' AND 'test-operation-00005'
       AND LENGTH(REQUEST_HASH) = 64;
    assert_count('PLAN_EDIT_OPERATION', v_count, 5);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Legacy seed repair completed for WITHTRIP_DEV.');
    DBMS_OUTPUT.PUT_LINE('Pending invitation token: ' || c_token_1);
    DBMS_OUTPUT.PUT_LINE('Accepted invitation token: ' || c_token_2);
END;
/

SELECT INVITATION_ID,
       INVITATION_STATUS,
       LENGTH(INVITATION_TOKEN) AS TOKEN_HASH_LENGTH,
       CASE
           WHEN EXPIRES_AT <= SYSTIMESTAMP THEN 'EXPIRED'
           ELSE 'ACTIVE'
       END AS EXPIRY_STATE
  FROM PLAN_INVITATION
 WHERE INVITATION_ID BETWEEN 900901 AND 900905
 ORDER BY INVITATION_ID;

SELECT REPORT_STATUS, COUNT(*) AS REPORT_COUNT
  FROM REPORT
 WHERE REPORT_ID BETWEEN 901101 AND 901105
 GROUP BY REPORT_STATUS
 ORDER BY REPORT_STATUS;

SELECT COUNT(*) AS REPORT_PROCESS_COUNT
  FROM REPORT_PROCESS
 WHERE REPORT_ID BETWEEN 901101 AND 901105;

SELECT COUNT(*) AS REPORT_NOTIFICATION_COUNT
  FROM NOTIFICATION
 WHERE RELATED_REPORT_ID BETWEEN 901101 AND 901105;

SELECT COUNT(*) AS ACCEPTED_INVITEE_MEMBER_COUNT
  FROM PLAN_MEMBER
 WHERE PLAN_ID = 900602
   AND MEMBER_ID = 900203
   AND PARTICIPANT_TYPE = 'INVITEE';

SELECT COUNT(*) AS INCONSISTENT_SYNC_COUNT
  FROM TOUR_SYNC_HISTORY
 WHERE SYNC_HISTORY_ID BETWEEN 900501 AND 900505
   AND TOTAL_COUNT <> SUCCESS_COUNT + FAIL_COUNT;

EXIT SUCCESS;
