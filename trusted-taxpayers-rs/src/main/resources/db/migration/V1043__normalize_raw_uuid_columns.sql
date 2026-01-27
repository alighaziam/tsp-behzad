BEGIN
    FOR rec IN (
        SELECT table_name, column_name
        FROM user_tab_columns
        WHERE data_type = 'RAW'
          AND data_length = 16
    ) LOOP
        EXECUTE IMMEDIATE
            'UPDATE ' || rec.table_name ||
            ' SET ' || rec.column_name || ' = HEXTORAW(LPAD(RAWTOHEX(' || rec.column_name || '), 32, ''0''))' ||
            ' WHERE ' || rec.column_name || ' IS NOT NULL' ||
            '   AND UTL_RAW.LENGTH(' || rec.column_name || ') < 16';
    END LOOP;
END;
/
