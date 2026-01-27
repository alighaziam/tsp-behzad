-- Simple fix for Flyway failed migration V110
-- Copy and paste this into your Oracle SQL client (SQL Developer, DBeaver, etc.)
-- Connection: jdbc:oracle:thin:@localhost:1521:XE
-- Username: ham_tsp
-- Password: ham_tsp

-- Step 1: Drop partially created table (if exists)
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE SERVICE_USAGE CASCADE CONSTRAINTS';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE = -942 THEN NULL; ELSE RAISE; END IF;
END;
/

-- Step 2: Delete failed migration record
DELETE FROM flyway_schema_history WHERE version = '110' AND success = 0;
-- If the above doesn't work, try:
-- DELETE FROM "flyway_schema_history" WHERE version = '110' AND success = 0;

COMMIT;

-- Step 3: Verify (should return no rows)
SELECT * FROM flyway_schema_history WHERE version = '110' AND success = 0;

