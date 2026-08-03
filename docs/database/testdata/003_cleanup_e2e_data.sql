-- Removes only data connected to the reserved e2e.*@withtrip.test members.
-- Persistent demo.*@withtrip.example content is never removed by this script.

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    PROCEDURE assert_target_schema IS
    BEGIN
        IF UPPER(SYS_CONTEXT('USERENV', 'SESSION_USER')) <> 'WITHTRIP_DEV'
           OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'WITHTRIP_DEV' THEN
            RAISE_APPLICATION_ERROR(-20001, 'E2E cleanup is allowed only in WITHTRIP_DEV.');
        END IF;
    END;
BEGIN
    assert_target_schema;

    DELETE FROM NOTIFICATION
     WHERE RECIPIENT_MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR RELATED_REPORT_ID IN (
               SELECT r.REPORT_ID
                 FROM REPORT r
                WHERE r.REPORTER_MEMBER_ID IN (
                          SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
                      )
                   OR r.PLAN_ID IN (
                          SELECT p.PLAN_ID
                            FROM TRAVEL_PLAN p
                            JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                           WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
                      )
           );

    DELETE FROM REPORT_PROCESS
     WHERE REPORT_ID IN (
               SELECT r.REPORT_ID
                 FROM REPORT r
                WHERE r.REPORTER_MEMBER_ID IN (
                          SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
                      )
                   OR r.PLAN_ID IN (
                          SELECT p.PLAN_ID
                            FROM TRAVEL_PLAN p
                            JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                           WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
                      )
           );

    DELETE FROM REPORT
     WHERE REPORTER_MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM PLAN_LIKE
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM RECENTLY_VIEWED_PLAN
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM PLAN_INVITATION
     WHERE INVITER_MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR INVITEE_MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR INVITEE_EMAIL LIKE 'e2e.%@withtrip.test'
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM PLAN_EDIT_OPERATION
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM PLAN_SCHEDULE_ITEM
     WHERE PLAN_DAY_ID IN (
               SELECT d.PLAN_DAY_ID
                 FROM PLAN_DAY d
                WHERE d.PLAN_ID IN (
                          SELECT p.PLAN_ID
                            FROM TRAVEL_PLAN p
                            JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                           WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
                      )
           );

    DELETE FROM PLAN_DAY
     WHERE PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM PLAN_MEMBER
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           )
        OR PLAN_ID IN (
               SELECT p.PLAN_ID
                 FROM TRAVEL_PLAN p
                 JOIN MEMBER m ON m.MEMBER_ID = p.OWNER_MEMBER_ID
                WHERE m.EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM TRAVEL_PLAN
     WHERE OWNER_MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM GOOGLE_ACCOUNT_LINK
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM MEMBER_WARNING_SUPPRESSION
     WHERE MEMBER_ID IN (
               SELECT MEMBER_ID FROM MEMBER WHERE EMAIL LIKE 'e2e.%@withtrip.test'
           );

    DELETE FROM MEMBER
     WHERE EMAIL LIKE 'e2e.%@withtrip.test';

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('E2E cleanup completed for WITHTRIP_DEV.');
END;
/

SELECT COUNT(*) AS REMAINING_E2E_MEMBER_COUNT
  FROM MEMBER
 WHERE EMAIL LIKE 'e2e.%@withtrip.test';

EXIT SUCCESS;
