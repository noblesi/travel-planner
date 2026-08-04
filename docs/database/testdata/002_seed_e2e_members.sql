-- Temporary authentication fixtures for P2 and P3 in WITHTRIP_DEV only.
-- Raw test password for all three accounts: WithTrip-E2E-2026!
-- BCrypt hash source is verified by backend integration tests.

SET DEFINE OFF;
SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    c_password_hash CONSTANT VARCHAR2(255) := '$2a$10$vtQ0WJwy8lT3TsxLq/NZ0ucMHmcSxN5hGQiWw28uIe4ehQTtDs2IW';

    PROCEDURE assert_target_schema IS
    BEGIN
        IF UPPER(SYS_CONTEXT('USERENV', 'SESSION_USER')) <> 'WITHTRIP_DEV'
           OR UPPER(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')) <> 'WITHTRIP_DEV' THEN
            RAISE_APPLICATION_ERROR(-20001, 'E2E seed is allowed only in WITHTRIP_DEV.');
        END IF;
    END;

    PROCEDURE ensure_member(
        p_email IN VARCHAR2,
        p_member_name IN VARCHAR2,
        p_nickname IN VARCHAR2,
        p_status IN VARCHAR2
    ) IS
    BEGIN
        UPDATE MEMBER
           SET MEMBER_NAME = p_member_name,
               NICKNAME = p_nickname,
               PASSWORD_HASH = c_password_hash,
               PRIVACY_CONSENT_YN = 'Y',
               MEMBER_STATUS = p_status,
               WITHDRAWN_AT = CASE WHEN p_status = 'WITHDRAWN' THEN SYSTIMESTAMP ELSE NULL END,
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
                MEMBER_STATUS,
                WITHDRAWN_AT
            ) VALUES (
                SEQ_MEMBER.NEXTVAL,
                p_member_name,
                LOWER(p_email),
                p_nickname,
                c_password_hash,
                'Y',
                p_status,
                CASE WHEN p_status = 'WITHDRAWN' THEN SYSTIMESTAMP ELSE NULL END
            );
        END IF;
    END;
BEGIN
    assert_target_schema;

    ensure_member('e2e.owner@withtrip.test', 'E2EOwner', 'E2E Plan Owner', 'ACTIVE');
    ensure_member('e2e.invitee@withtrip.test', 'E2EGuest', 'E2E Invitee', 'ACTIVE');
    ensure_member('e2e.withdrawn@withtrip.test', 'E2EFormer', 'E2E Withdrawn Member', 'WITHDRAWN');

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('E2E member seed completed for WITHTRIP_DEV.');
END;
/

SELECT MEMBER_ID, EMAIL, NICKNAME, MEMBER_STATUS
  FROM MEMBER
 WHERE EMAIL LIKE 'e2e.%@withtrip.test'
 ORDER BY EMAIL;

EXIT SUCCESS;
