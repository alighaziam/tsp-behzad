-- Fix Flyway failed migration for V110
-- This script removes the failed migration record from Flyway's schema history
-- Run this script manually against your database before restarting the application
-- 
-- Connection: jdbc:oracle:thin:@localhost:1521:XE
-- Username: ham_tsp
-- Password: ham_tsp

-- First, check if SERVICE_USAGE table was partially created and drop it if needed
BEGIN
  EXECUTE IMMEDIATE 'DROP TABLE SERVICE_USAGE CASCADE CONSTRAINTS';
  DBMS_OUTPUT.PUT_LINE('Dropped partially created SERVICE_USAGE table');
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE = -942 THEN
      DBMS_OUTPUT.PUT_LINE('SERVICE_USAGE table does not exist, skipping drop');
    ELSE
      RAISE;
    END IF;
END;
/

-- Remove the failed migration record from flyway_schema_history
-- Oracle typically stores table names in uppercase, so try both variations
BEGIN
  -- Try with quotes first (case-sensitive)
  EXECUTE IMMEDIATE 'DELETE FROM "flyway_schema_history" WHERE version = ''110'' AND success = 0';
  DBMS_OUTPUT.PUT_LINE('Deleted failed migration record (with quotes)');
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE = -942 THEN
      -- Table might be uppercase, try without quotes
      BEGIN
        EXECUTE IMMEDIATE 'DELETE FROM FLYWAY_SCHEMA_HISTORY WHERE version = ''110'' AND success = 0';
        DBMS_OUTPUT.PUT_LINE('Deleted failed migration record (uppercase)');
      EXCEPTION
        WHEN OTHERS THEN
          DBMS_OUTPUT.PUT_LINE('Error deleting record: ' || SQLERRM);
          RAISE;
      END;
    ELSE
      RAISE;
    END IF;
END;
/

COMMIT;

-- Verify the record was removed
SELECT version, description, type, script, installed_on, success 
FROM flyway_schema_history 
WHERE version = '110'
ORDER BY installed_rank DESC;

