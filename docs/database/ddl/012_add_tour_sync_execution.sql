-- TOUR API synchronization lease and persistent execution history.
-- Safe for both the canonical schema and legacy schemas created from travelplanner_final.sql.

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET DEFINE OFF

DECLARE
    v_table_count  NUMBER;
    v_column_count NUMBER;
    v_object_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_table_count
      FROM USER_TABLES
     WHERE TABLE_NAME = 'TOUR_SYNC_STATE';

    IF v_table_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE TOUR_SYNC_STATE (
                LOCK_ID          NUMBER(1)             NOT NULL,
                RUNNING_YN       CHAR(1)               DEFAULT 'N' NOT NULL,
                SYNC_ID          VARCHAR2(40),
                STARTED_AT       TIMESTAMP WITH TIME ZONE,
                LEASE_EXPIRES_AT TIMESTAMP WITH TIME ZONE,
                STARTED_BY       VARCHAR2(100),
                CONSTRAINT PK_TOUR_SYNC_STATE PRIMARY KEY (LOCK_ID),
                CONSTRAINT CK_TOUR_SYNC_STATE_LOCK CHECK (LOCK_ID = 1),
                CONSTRAINT CK_TOUR_SYNC_STATE_RUNNING CHECK (RUNNING_YN IN ('Y', 'N')),
                CONSTRAINT CK_TOUR_SYNC_STATE_VALUES CHECK (
                    (RUNNING_YN = 'N' AND SYNC_ID IS NULL AND STARTED_AT IS NULL
                        AND LEASE_EXPIRES_AT IS NULL AND STARTED_BY IS NULL)
                    OR
                    (RUNNING_YN = 'Y' AND SYNC_ID IS NOT NULL AND STARTED_AT IS NOT NULL
                        AND LEASE_EXPIRES_AT IS NOT NULL AND STARTED_BY IS NOT NULL)
                )
            )
        ]';
    END IF;

    EXECUTE IMMEDIATE q'[
        MERGE INTO TOUR_SYNC_STATE target
        USING (SELECT 1 LOCK_ID FROM DUAL) source
           ON (target.LOCK_ID = source.LOCK_ID)
         WHEN NOT MATCHED THEN
            INSERT (LOCK_ID, RUNNING_YN) VALUES (1, 'N')
    ]';

    SELECT COUNT(*)
      INTO v_table_count
      FROM USER_TABLES
     WHERE TABLE_NAME = 'TOUR_SYNC_HISTORY';

    IF v_table_count = 0 THEN
        EXECUTE IMMEDIATE q'[
            CREATE TABLE TOUR_SYNC_HISTORY (
                SYNC_ID       VARCHAR2(40)             NOT NULL,
                STARTED_AT    TIMESTAMP WITH TIME ZONE NOT NULL,
                CHANGED_COUNT NUMBER(10)               DEFAULT 0 NOT NULL,
                FAILED_COUNT  NUMBER(10)               DEFAULT 0 NOT NULL,
                SYNC_STATUS   VARCHAR2(20)             NOT NULL,
                MANAGER       VARCHAR2(100)            NOT NULL,
                CONSTRAINT PK_TOUR_SYNC_HISTORY PRIMARY KEY (SYNC_ID),
                CONSTRAINT CK_TOUR_SYNC_HISTORY_COUNTS CHECK (CHANGED_COUNT >= 0 AND FAILED_COUNT >= 0),
                CONSTRAINT CK_TOUR_SYNC_HISTORY_STATUS CHECK (SYNC_STATUS IN ('성공', '부분 성공', '실패'))
            )
        ]';
    ELSE
        SELECT COUNT(*)
          INTO v_column_count
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME = 'TOUR_SYNC_HISTORY'
           AND COLUMN_NAME = 'SYNC_ID';

        IF v_column_count = 0 THEN
            SELECT COUNT(*)
              INTO v_object_count
              FROM USER_TABLES
             WHERE TABLE_NAME = 'TOUR_SYNC_HISTORY_V2';

            IF v_object_count > 0 THEN
                RAISE_APPLICATION_ERROR(
                    -20012,
                    'TOUR_SYNC_HISTORY_V2 already exists. Inspect the interrupted migration before retrying.'
                );
            END IF;

            EXECUTE IMMEDIATE q'[
                CREATE TABLE TOUR_SYNC_HISTORY_V2 (
                    SYNC_ID       VARCHAR2(40)             NOT NULL,
                    STARTED_AT    TIMESTAMP WITH TIME ZONE NOT NULL,
                    CHANGED_COUNT NUMBER(10)               DEFAULT 0 NOT NULL,
                    FAILED_COUNT  NUMBER(10)               DEFAULT 0 NOT NULL,
                    SYNC_STATUS   VARCHAR2(20)             NOT NULL,
                    MANAGER       VARCHAR2(100)            NOT NULL,
                    CONSTRAINT PK_TOUR_SYNC_HISTORY_V2 PRIMARY KEY (SYNC_ID),
                    CONSTRAINT CK_TOUR_SYNC_HISTORY_COUNTS_V2 CHECK (CHANGED_COUNT >= 0 AND FAILED_COUNT >= 0),
                    CONSTRAINT CK_TOUR_SYNC_HISTORY_STATUS_V2 CHECK (SYNC_STATUS IN ('성공', '부분 성공', '실패'))
                )
            ]';

            EXECUTE IMMEDIATE q'[
                INSERT INTO TOUR_SYNC_HISTORY_V2 (
                    SYNC_ID, STARTED_AT, CHANGED_COUNT, FAILED_COUNT, SYNC_STATUS, MANAGER
                )
                SELECT 'L-' || TO_CHAR(history.SYNC_HISTORY_ID),
                       history.STARTED_AT,
                       NVL(history.SUCCESS_COUNT, 0),
                       NVL(history.FAIL_COUNT, 0),
                       CASE UPPER(history.PROCESS_STATUS)
                           WHEN 'SUCCESS' THEN '성공'
                           WHEN 'PARTIAL_SUCCESS' THEN '부분 성공'
                           WHEN 'FAILED' THEN '실패'
                           WHEN 'COMPLETED' THEN
                               CASE WHEN NVL(history.FAIL_COUNT, 0) = 0 THEN '성공' ELSE '부분 성공' END
                           ELSE
                               CASE
                                   WHEN NVL(history.FAIL_COUNT, 0) = 0 THEN '성공'
                                   WHEN NVL(history.SUCCESS_COUNT, 0) = 0 THEN '실패'
                                   ELSE '부분 성공'
                               END
                       END,
                       NVL(admin.LOGIN_ID, 'legacy-admin-' || TO_CHAR(history.ADMIN_ID))
                  FROM TOUR_SYNC_HISTORY history
                  LEFT JOIN ADMIN admin ON admin.ADMIN_ID = history.ADMIN_ID
            ]';

            EXECUTE IMMEDIATE 'DROP TABLE TOUR_SYNC_HISTORY CASCADE CONSTRAINTS';
            EXECUTE IMMEDIATE 'ALTER TABLE TOUR_SYNC_HISTORY_V2 RENAME TO TOUR_SYNC_HISTORY';
            EXECUTE IMMEDIATE 'ALTER TABLE TOUR_SYNC_HISTORY RENAME CONSTRAINT PK_TOUR_SYNC_HISTORY_V2 TO PK_TOUR_SYNC_HISTORY';
            EXECUTE IMMEDIATE 'ALTER TABLE TOUR_SYNC_HISTORY RENAME CONSTRAINT CK_TOUR_SYNC_HISTORY_COUNTS_V2 TO CK_TOUR_SYNC_HISTORY_COUNTS';
            EXECUTE IMMEDIATE 'ALTER TABLE TOUR_SYNC_HISTORY RENAME CONSTRAINT CK_TOUR_SYNC_HISTORY_STATUS_V2 TO CK_TOUR_SYNC_HISTORY_STATUS';
        END IF;
    END IF;

    SELECT COUNT(*)
      INTO v_object_count
      FROM USER_INDEXES
     WHERE INDEX_NAME = 'IX_TOUR_SYNC_HISTORY_STARTED';

    IF v_object_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IX_TOUR_SYNC_HISTORY_STARTED '
            || 'ON TOUR_SYNC_HISTORY (SYS_EXTRACT_UTC(STARTED_AT) DESC)';
    END IF;

    SELECT COUNT(*)
      INTO v_object_count
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_TOUR_SYNC_HISTORY';

    IF v_object_count > 0 THEN
        EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_TOUR_SYNC_HISTORY';
    END IF;
END;
/

COMMENT ON TABLE TOUR_SYNC_STATE IS '다중 Instance TOUR API 동기화 Lease 상태';
COMMENT ON TABLE TOUR_SYNC_HISTORY IS 'TOUR API 동기화 실행 결과 이력';

COMMIT;
